/*
 * This source file is part of Cube.
 * Copyright (c) 2023-2026 Ambrose Xu.
 */
package cube.dispatcher.test;

import cell.api.Nucleus;
import cell.api.Speakable;
import cell.core.net.Endpoint;
import cell.core.talk.dialect.ActionDialect;
import cube.auth.AuthToken;
import cube.common.JSONable;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;
import cube.dispatcher.Director;
import cube.dispatcher.Performer;
import cube.dispatcher.PerformerListener;
import cube.dispatcher.Scope;
import cube.dispatcher.aigc.AIGCCellet;
import cube.dispatcher.aigc.Manager;
import cube.dispatcher.util.Tickable;
import cube.util.HttpServer;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Consumer;

/**
 * 无真实网络连接的回归验证。使用 JDK 8，运行于独立临时目录，
 * classpath 包含 dispatcher/classes、common/classes、deploy/libs/* 和 deploy/bin/cell.jar。
 * 断言失败抛出 AssertionError，不依赖 -ea 或额外测试框架。
 */
public class ManagerRobustnessTest {

    private static final String TOKEN = "manager-regression-token";
    private static final long HOUR = 60L * 60 * 1000;
    private static final Device DEVICE = new Device("Browser", "Windows");
    private static int assertions;

    public static void main(String[] args) throws Exception {
        Nucleus nucleus = new Nucleus();
        try {
            testLifecycle(nucleus);
            testTokens(nucleus);
            testAsyncTasks(nucleus);
            testConcurrentSubmission(nucleus);
            testSnapshotsAndStop(nucleus);
            testExpiry(nucleus);
            testPerformer(nucleus);
            System.out.println("ManagerRobustnessTest: " + assertions + " assertions passed");
        } finally {
            nucleus.destroy();
        }
    }

    private static void testLifecycle(Nucleus nucleus) throws Exception {
        Manager manager = new Manager();
        FakePerformer first = new FakePerformer(nucleus);
        FakePerformer second = new FakePerformer(nucleus);
        manager.stop();
        manager.onTick(System.currentTimeMillis() + HOUR);
        check(manager.getSpeechRecognitionFuture(null) == null, "null query before start");
        check(manager.checkAndGetToken(TOKEN, DEVICE) == null, "authentication before start");
        check(manager.textToFile(TOKEN, "hello", new JSONArray()) == null, "submission before start");
        check(manager.getOrCreateUser(new JSONObject()) == null, "sync request before start");
        manager.start(first);
        int endpoints = first.server.getContextHandlers().size();
        check(endpoints > 0, "handlers registered");
        manager.start(first);
        check(first.server.getContextHandlers().size() == endpoints, "idempotent start");
        check(list(first, "tickableList").size() == 1, "one tick registration");
        try {
            manager.start(second);
            throw new AssertionError("running Manager accepted another Performer");
        } catch (IllegalStateException expected) {
            check(true, "reject rebind while running");
        }
        Manager.TextToFileFuture pending = manager.textToFile(TOKEN, "hello", new JSONArray());
        ActionDialect oldRequest = first.lastRequest();
        manager.stop();
        manager.stop();
        check(state(pending) == AIGCStateCode.Cancelled.code, "stop cancels pending future");
        check(map(first, "listenerMap").isEmpty(), "stop removes listener");
        check(list(first, "tickableList").isEmpty(), "stop removes tick");
        check(manager.getTextToFileFuture(pending.toJSON().getLong("sn")) == null, "stop clears futures");
        check(manager.queryQueueCount(TOKEN) == null, "sync request after stop");
        manager.start(first);
        check(first.server.getContextHandlers().size() == endpoints, "restart retains one handler per path");
        reply(manager, oldRequest, AIGCStateCode.Ok, new JSONObject().put("result", new JSONObject()));
        check(state(pending) == AIGCStateCode.Cancelled.code, "old callback after restart ignored");
        manager.stop();
        manager.start(second);
        check(manager.getPerformer() == second, "rebind after stop");
        manager.stop();

        Manager retry = new Manager();
        FakePerformer failing = new FakePerformer(nucleus);
        failing.failListener = true;
        try {
            retry.start(failing);
            throw new AssertionError("expected startup failure");
        } catch (IllegalStateException expected) {
            check(map(failing, "listenerMap").isEmpty(), "failed start rolls back listener");
            check(list(failing, "tickableList").isEmpty(), "failed start rolls back tick");
        }
        int registered = failing.server.getContextHandlers().size();
        failing.failListener = false;
        retry.start(failing);
        check(failing.server.getContextHandlers().size() == registered, "retry avoids duplicate endpoints");
        retry.stop();

        Manager partial = new Manager();
        FakePerformer partialPerformer = new FakePerformer(nucleus);
        partialPerformer.server.failAt = 4;
        try {
            partial.start(partialPerformer);
            throw new AssertionError("expected handler registration failure");
        } catch (IllegalStateException expected) {
            check(list(partialPerformer, "tickableList").isEmpty(), "partial registration has no tick");
        }
        partialPerformer.server.failAt = -1;
        partial.start(partialPerformer);
        List<String> paths = new ArrayList<>();
        for (ContextHandler handler : partialPerformer.server.getContextHandlers()) {
            check(!paths.contains(handler.getContextPath()), "partial startup retry deduplicates paths");
            paths.add(handler.getContextPath());
        }
        partial.stop();
    }

    private static void testTokens(Nucleus nucleus) throws Exception {
        Manager manager = new Manager();
        FakePerformer performer = new FakePerformer(nucleus);
        manager.start(performer);
        try {
            performer.syncResponse = request -> response(request, AIGCStateCode.Ok, tokenPayload(System.currentTimeMillis() + HOUR));
            Manager.ContactToken contact = manager.getContactToken("123456", DEVICE);
            check(contact != null && TOKEN.equals(contact.authToken.getCode()), "invitation returns resolved token object");
            int calls = performer.syncCalls.get();
            check(TOKEN.equals(manager.checkAndGetToken(TOKEN, DEVICE)), "canonical token cache hit");
            check(calls == performer.syncCalls.get(), "cache hit avoids network request");
            check(manager.getContactToken(TOKEN, new Device("Unknown", "Unknown")) == contact, "unknown device compatibility");
            manager.getContactToken(TOKEN, new Device("Phone", "Android"));
            check(performer.syncCalls.get() == calls + 1, "different device revalidates token");
            manager.removeTokenCache(null);
            check(manager.getContactToken(null, DEVICE) == null, "null token safe");
            check(manager.getContactToken(" ", DEVICE) == null, "blank token rejected");
            check(manager.getContactToken(TOKEN, null) == null, "null device rejected");

            manager.removeTokenCache(TOKEN);
            performer.syncResponse = request -> response(request, AIGCStateCode.Ok, tokenPayload(System.currentTimeMillis() - 1));
            check(manager.getContactToken(TOKEN, DEVICE) == null, "expired service token rejected");
            check(map(manager, "validTokenMap").isEmpty(), "expired token not cached");
            performer.syncResponse = request -> response(request, AIGCStateCode.Ok, new JSONObject());
            check(manager.getContactToken(TOKEN, DEVICE) == null, "malformed authentication safe");
            performer.syncResponse = request -> response(request, AIGCStateCode.NoToken, new JSONObject());
            check(manager.getContactToken(TOKEN, DEVICE) == null, "failed authentication safe");

            performer.syncResponse = request -> response(request, AIGCStateCode.Ok, tokenPayload(System.currentTimeMillis() + HOUR));
            contact = manager.getContactToken(TOKEN, DEVICE);
            setLong(contact, "timestamp", System.currentTimeMillis() - 24 * HOUR);
            calls = performer.syncCalls.get();
            manager.getContactToken(TOKEN, DEVICE);
            check(performer.syncCalls.get() == calls + 1, "token cache TTL checked on access");
            contact = manager.getContactToken(TOKEN, DEVICE);
            setLong(contact.authToken, "expiry", System.currentTimeMillis() - 1);
            calls = performer.syncCalls.get();
            manager.getContactToken(TOKEN, DEVICE);
            check(performer.syncCalls.get() == calls + 1, "real token expiry checked on cache hit");
            performer.syncResponse = request -> response(request, AIGCStateCode.Ok, new JSONObject());
            check(manager.signOutUser(TOKEN) != null, "successful sign out");
            check(map(manager, "validTokenMap").isEmpty(), "sign out invalidates token cache");

            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            performer.syncResponse = request -> {
                entered.countDown();
                await(release);
                return response(request, AIGCStateCode.Ok, tokenPayload(System.currentTimeMillis() + HOUR));
            };
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<Manager.ContactToken> result = executor.submit(() -> manager.getContactToken(TOKEN, DEVICE));
                await(entered);
                manager.stop();
                manager.start(performer);
                release.countDown();
                check(result.get(5, TimeUnit.SECONDS) == null, "old authentication cannot populate restarted Manager");
                check(map(manager, "validTokenMap").isEmpty(), "stopped generation does not refill cache");
            } finally {
                release.countDown();
                executor.shutdownNow();
            }
        } finally {
            manager.stop();
        }
    }

    private static void testAsyncTasks(Nucleus nucleus) throws Exception {
        Manager manager = new Manager();
        FakePerformer performer = new FakePerformer(nucleus);
        manager.start(performer);
        try {
            for (int kind = 0; kind < 5; ++kind) {
                JSONable future = submit(manager, kind, "file-" + kind, false);
                JSONObject initial = future.toJSON();
                check(state(future) == AIGCStateCode.Processing.code, "initial processing state");
                check(initial.has(kind == 0 ? "sn" : kind == 1 || kind == 3 ? "queryCode" : "fileCode"), "legacy identifier field");
                check(initial.has("timestamp") && !initial.has("result"), "legacy processing JSON");
                ActionDialect request = performer.lastRequest();
                ActionDialect mismatch = response(request, AIGCStateCode.Ok, successPayload(kind, "file-" + kind));
                mismatch.setName("unexpected-action");
                manager.onReceived(AIGCCellet.NAME, mismatch);
                check(state(future) == AIGCStateCode.Processing.code, "mismatched action ignored");
                manager.onReceived("Other", response(request, AIGCStateCode.Ok, successPayload(kind, "file-" + kind)));
                check(state(future) == AIGCStateCode.Processing.code, "mismatched cellet ignored");
                reply(manager, request, AIGCStateCode.Ok, successPayload(kind, "file-" + kind));
                check(state(future) == AIGCStateCode.Ok.code && future.toJSON().has("result"), "completed result and state published");
                String completed = future.toJSON().toString();
                reply(manager, request, AIGCStateCode.Failure, new JSONObject());
                check(completed.equals(future.toJSON().toString()), "duplicate callback cannot overwrite completion");
                check(map(manager, "pendingRequests").isEmpty(), "completion removes request association");
                check(map(manager, "pendingFutures").isEmpty(), "completion removes future association");

                JSONable malformed = submit(manager, kind, "bad-" + kind, false);
                ActionDialect badRequest = performer.lastRequest();
                reply(manager, badRequest, AIGCStateCode.Ok, new JSONObject());
                check(state(malformed) == AIGCStateCode.DataStructureError.code, "malformed success produces terminal failure");
                JSONable failed = submit(manager, kind, "failed-" + kind, false);
                reply(manager, performer.lastRequest(), AIGCStateCode.NoToken, new JSONObject());
                check(state(failed) == AIGCStateCode.NoToken.code, "empty failed payload still associated by packet sn");

                performer.sendSuccess = false;
                JSONable unsent = submit(manager, kind, "unsent-" + kind, false);
                check(state(unsent) == AIGCStateCode.Failure.code, "failed send terminates future");
                performer.sendSuccess = true;
                performer.throwOnSend = true;
                JSONable throwing = submit(manager, kind, "throwing-" + kind, false);
                check(state(throwing) == AIGCStateCode.Failure.code, "throwing send terminates future");
                performer.throwOnSend = false;
            }
            for (int kind = 1; kind < 5; ++kind) {
                JSONable old = submit(manager, kind, "reset-" + kind, false);
                ActionDialect oldRequest = performer.lastRequest();
                JSONable replacement = submit(manager, kind, "reset-" + kind, true);
                ActionDialect replacementRequest = performer.lastRequest();
                check(old != replacement && state(old) == AIGCStateCode.Cancelled.code, "reset replaces and cancels old task");
                reply(manager, oldRequest, AIGCStateCode.Ok, successPayload(kind, "reset-" + kind));
                check(state(replacement) == AIGCStateCode.Processing.code, "old reply does not complete replacement");
                reply(manager, replacementRequest, AIGCStateCode.Ok, successPayload(kind, "reset-" + kind));
                check(state(replacement) == AIGCStateCode.Ok.code, "replacement completes by its own sn");
                int sends = performer.requests.size();
                check(submit(manager, kind, "reset-" + kind, false) == replacement, "completed cached task reused");
                check(performer.requests.size() == sends, "cache reuse does not send again");
            }
            performer.syncResponse = request -> response(request, AIGCStateCode.Ok, successPayload(1, "sync-file"));
            JSONable synchronous = manager.automaticSpeechRecognition(TOKEN, "sync-file", null, true, false);
            check(state(synchronous) == AIGCStateCode.Ok.code && synchronous.toJSON().has("result"), "synchronous ASR returns Ok");
            performer.callbackOnSend = request -> reply(manager, request, AIGCStateCode.Ok, successPayload(1, "immediate"));
            JSONable immediate = submit(manager, 1, "immediate", false);
            check(state(immediate) == AIGCStateCode.Ok.code, "association exists before an immediate send callback");
            performer.callbackOnSend = null;
            check(map(manager, "pendingRequests").isEmpty(), "immediate reply leaves no association");
            JSONable urlTask = manager.automaticSpeechRecognition(TOKEN, null, "https://example.test/audio.wav", false, false);
            reply(manager, performer.lastRequest(), AIGCStateCode.Ok, successPayload(1, "resolved-file-code"));
            check(state(urlTask) == AIGCStateCode.Ok.code, "URL request associates by sn despite a different result file code");
            check(manager.speechDiarization(TOKEN, null, null, false) == null, "missing file source rejected");
            check(manager.facialExpressionRecognition(TOKEN, " ", false, false) == null, "blank file rejected");
            check(manager.textToFile(TOKEN, "hello", null) == null, "missing files rejected");
            check(manager.getSpeechDiarization(TOKEN, null) == null, "null diarization query safe");
            manager.onReceived(AIGCCellet.NAME, new ActionDialect("unknown"));
            manager.onReceived(AIGCCellet.NAME, null);
        } finally {
            manager.stop();
        }
    }

    private static void testConcurrentSubmission(Nucleus nucleus) throws Exception {
        Manager manager = new Manager();
        FakePerformer performer = new FakePerformer(nucleus);
        manager.start(performer);
        ExecutorService executor = Executors.newFixedThreadPool(12);
        try {
            for (int kind = 1; kind < 5; ++kind) {
                final int type = kind;
                int sent = performer.requests.size();
                CountDownLatch ready = new CountDownLatch(12);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<JSONable>> results = new ArrayList<>();
                for (int i = 0; i < 12; ++i) {
                    results.add(executor.submit(() -> {
                        ready.countDown();
                        await(start);
                        return submit(manager, type, "concurrent-" + type, false);
                    }));
                }
                await(ready);
                start.countDown();
                JSONable first = results.get(0).get(5, TimeUnit.SECONDS);
                for (Future<JSONable> result : results) {
                    check(result.get(5, TimeUnit.SECONDS) == first, "concurrent callers share one future");
                }
                check(performer.requests.size() == sent + 1, "concurrent submissions send exactly once");
            }
        } finally {
            executor.shutdownNow();
            manager.stop();
        }
    }

    private static void testSnapshotsAndStop(Nucleus nucleus) throws Exception {
        Manager manager = new Manager();
        FakePerformer performer = new FakePerformer(nucleus);
        manager.start(performer);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            JSONable future = submit(manager, 0, "snapshot", false);
            ActionDialect request = performer.lastRequest();
            CountDownLatch start = new CountDownLatch(1);
            Future<?> reader = executor.submit(() -> {
                await(start);
                for (int i = 0; i < 2000; ++i) {
                    JSONObject json = future.toJSON();
                    if ((json.getInt("state") == AIGCStateCode.Ok.code) != json.has("result")) {
                        throw new AssertionError("inconsistent state/result snapshot");
                    }
                }
            });
            Future<?> writer = executor.submit(() -> {
                await(start);
                reply(manager, request, AIGCStateCode.Ok, successPayload(0, "snapshot"));
            });
            start.countDown();
            reader.get(5, TimeUnit.SECONDS);
            writer.get(5, TimeUnit.SECONDS);
            check(state(future) == AIGCStateCode.Ok.code, "concurrent serialization observes coherent success");

            performer.sendEntered = new CountDownLatch(1);
            performer.sendRelease = new CountDownLatch(1);
            Future<JSONable> sending = executor.submit(() -> submit(manager, 2, "stopping", false));
            await(performer.sendEntered);
            ActionDialect inFlight = performer.lastRequest();
            manager.stop();
            manager.start(performer);
            performer.sendRelease.countDown();
            JSONable cancelled = sending.get(5, TimeUnit.SECONDS);
            check(state(cancelled) == AIGCStateCode.Cancelled.code, "stop does not wait for in-flight send");
            reply(manager, inFlight, AIGCStateCode.Ok, successPayload(2, "stopping"));
            check(state(cancelled) == AIGCStateCode.Cancelled.code, "in-flight reply ignored after restart");
            check(manager.getSpeechEmotionRecognitionFuture("stopping") == null, "in-flight send cannot refill cache");
        } finally {
            if (performer.sendRelease != null) {
                performer.sendRelease.countDown();
            }
            executor.shutdownNow();
            manager.stop();
        }
    }

    private static void testExpiry(Nucleus nucleus) throws Exception {
        Manager manager = new Manager();
        FakePerformer performer = new FakePerformer(nucleus);
        manager.start(performer);
        try {
            List<JSONable> stale = new ArrayList<>();
            List<ActionDialect> requests = new ArrayList<>();
            for (int kind = 0; kind < 5; ++kind) {
                JSONable future = submit(manager, kind, "stale-" + kind, false);
                setLong(future, "timestamp", System.currentTimeMillis() - HOUR - 1000);
                stale.add(future);
                requests.add(performer.lastRequest());
            }
            setLong(manager, "lastTickTime", System.currentTimeMillis() - 60000);
            manager.onTick(System.currentTimeMillis());
            for (int kind = 0; kind < 5; ++kind) {
                check(state(stale.get(kind)) == AIGCStateCode.Expired.code, "all pending task types expire");
                check(cache(manager, kind).isEmpty(), "expired task removed from cache");
                reply(manager, requests.get(kind), AIGCStateCode.Ok, successPayload(kind, "stale-" + kind));
                check(state(stale.get(kind)) == AIGCStateCode.Expired.code, "late reply cannot resurrect expired task");
            }
            check(map(manager, "pendingRequests").isEmpty() && map(manager, "pendingFutures").isEmpty(), "expiry cleans both associations");
            JSONable old = submit(manager, 4, "replaced-expired", false);
            setLong(old, "timestamp", System.currentTimeMillis() - HOUR - 1000);
            JSONable replacement = submit(manager, 4, "replaced-expired", false);
            check(old != replacement && state(old) == AIGCStateCode.Expired.code, "expired entry replaced without reset");
            setLong(manager, "lastTickTime", System.currentTimeMillis() - 60000);
            manager.onTick(System.currentTimeMillis());
            check(manager.getFacialExpressionRecognitionFuture("replaced-expired") == replacement, "cleanup retains fresh replacement");
            JSONable callbackExpired = submit(manager, 2, "expired-callback", false);
            setLong(callbackExpired, "timestamp", System.currentTimeMillis() - HOUR - 1000);
            reply(manager, performer.lastRequest(), AIGCStateCode.Ok, successPayload(2, "expired-callback"));
            check(state(callbackExpired) == AIGCStateCode.Expired.code, "expired reply terminates pending future before tick");
            check(manager.getSpeechEmotionRecognitionFuture("expired-callback") == null, "expired reply removes cached task before tick");
        } finally {
            manager.stop();
        }
    }

    private static void testPerformer(Nucleus nucleus) throws Exception {
        Performer performer = new Performer(nucleus);
        ActionDialect request = new Packet(AIGCAction.GetQueueCount.name, new JSONObject()).toDialect();
        check(!performer.tryTransmit(AIGCCellet.NAME, request), "no director send fails safely");
        performer.transmit(AIGCCellet.NAME, request);
        check(performer.syncTransmit(AIGCCellet.NAME, request) == null, "no director sync returns null");
        check(performer.syncTransmit(TOKEN, AIGCCellet.NAME, request) == null, "no director token sync safe");
        PerformerListener first = (cellet, primitive) -> { };
        PerformerListener replacement = (cellet, primitive) -> { };
        performer.setListener(AIGCCellet.NAME, first);
        performer.setListener(AIGCCellet.NAME, replacement);
        check(!performer.removeListener(AIGCCellet.NAME, first), "old listener cannot remove replacement");
        check(performer.removeListener(AIGCCellet.NAME, replacement), "current listener can be removed");

        AtomicInteger ticks = new AtomicInteger();
        Tickable marker = now -> ticks.incrementAndGet();
        Tickable mutating = new Tickable() {
            public void onTick(long now) {
                performer.removeTickable(this);
                performer.addTickable(marker);
                performer.addTickable(marker);
            }
        };
        performer.addTickable(mutating);
        performer.onTick(0);
        performer.onTick(0);
        check(ticks.get() == 1, "tick mutation safe and registration deduplicated");

        Director director = performer.addDirector("127.0.0.1", 6000, "127.0.0.1", 6001, new Scope());
        check(!performer.tryTransmit(AIGCCellet.NAME, request), "missing speaker fails safely");
        director.speaker = speaker(action -> false);
        check(!performer.tryTransmit(AIGCCellet.NAME, request), "speaker rejection returned");
        check(performer.syncTransmit(AIGCCellet.NAME, request) == null, "sync rejection returned");
        check(map(performer, "blockMap").isEmpty(), "rejected send leaves no block");
        director.speaker = speaker(action -> { throw new IllegalStateException("expected send exception"); });
        check(!performer.tryTransmit(AIGCCellet.NAME, request), "send exception converted to failure");
        check(performer.syncTransmit(AIGCCellet.NAME, request) == null, "sync send exception safe");
        check(map(performer, "blockMap").isEmpty(), "throwing send releases block");

        director.speaker = speaker(action -> {
            performer.onListened(null, AIGCCellet.NAME, response(action, AIGCStateCode.Ok, new JSONObject()));
            return true;
        });
        ActionDialect completed = performer.syncTransmit(AIGCCellet.NAME, request, 500);
        check(completed != null && !completed.containsParam("_performer"), "immediate sync reply visible and metadata removed");
        check(map(performer, "blockMap").isEmpty(), "successful request releases block");
        director.speaker = speaker(action -> true);
        check(performer.syncTransmit(AIGCCellet.NAME, request, 1) == null, "sync timeout safe");
        check(map(performer, "blockMap").isEmpty(), "timeout releases block");

        CountDownLatch sent = new CountDownLatch(1);
        director.speaker = speaker(action -> { sent.countDown(); return true; });
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicBoolean returnedNull = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            returnedNull.set(performer.syncTransmit(AIGCCellet.NAME, request, 10000) == null);
            interrupted.set(Thread.currentThread().isInterrupted());
        });
        waiter.start();
        await(sent);
        waiter.interrupt();
        waiter.join(5000);
        check(!waiter.isAlive() && returnedNull.get() && interrupted.get(), "interrupt promptly returns and preserves flag");
        check(map(performer, "blockMap").isEmpty(), "interrupted request releases block");
    }

    private static JSONable submit(Manager manager, int kind, String file, boolean reset) {
        switch (kind) {
            case 0: return manager.textToFile(TOKEN, "hello", new JSONArray());
            case 1: return manager.automaticSpeechRecognition(TOKEN, file, null, false, reset);
            case 2: return manager.speechEmotionRecognition(TOKEN, file, reset);
            case 3: return manager.speechDiarization(TOKEN, file, null, reset);
            case 4: return manager.facialExpressionRecognition(TOKEN, file, false, reset);
            default: throw new AssertionError("unexpected future kind");
        }
    }

    private static Map<?, ?> cache(Manager manager, int kind) throws Exception {
        return map(manager, new String[] { "textToFileFutureMap", "speechRecognitionFutureMap",
                "speechEmotionRecognitionFutureMap", "speechDiarizationFutureMap", "facialExpressionRecognitionFutureMap" }[kind]);
    }

    private static JSONObject tokenPayload(long expiry) {
        AuthToken token = new AuthToken(TOKEN, "regression", "test-app", 42L, System.currentTimeMillis(), expiry, false);
        return new JSONObject().put("token", token.toJSON()).put("contact", new Contact(42L, "regression", "Test").toJSON());
    }

    private static JSONObject successPayload(int kind, String file) {
        if (kind == 0) {
            return new JSONObject().put("result", new JSONObject().put("text", "generated"));
        }
        JSONObject data = new JSONObject().put("file", new FileLabel(42L, "regression", file,
                42L, "test.wav", 128L, 1L, 1L, 0L).toJSON()).put("elapsed", 1L);
        if (kind == 1) {
            return data.put("text", "transcript").put("words", new JSONArray()).put("lang", "zh").put("duration", 1.0);
        }
        if (kind == 2) {
            return data.put("score", 0.5).put("duration", 1.0);
        }
        if (kind == 3) {
            return data.put("tracks", new JSONArray()).put("duration", 1.0);
        }
        return data.put("list", new JSONArray());
    }

    private static ActionDialect response(ActionDialect request, AIGCStateCode state, JSONObject data) {
        ActionDialect response = new Packet(request.getParamAsLong("sn"), request.getName(),
                new JSONObject().put("code", state.code).put("data", data)).toDialect();
        if (request.containsParam("_performer")) {
            response.addParam("_performer", request.getParamAsJson("_performer"));
        }
        return response;
    }

    private static void reply(Manager manager, ActionDialect request, AIGCStateCode state, JSONObject data) {
        manager.onReceived(AIGCCellet.NAME, response(request, state, data));
    }

    private static int state(JSONable future) {
        JSONObject json = future.toJSON();
        return json.getInt(json.has("state") ? "state" : "stateCode");
    }

    private static Speakable speaker(Function<ActionDialect, Boolean> send) {
        return (Speakable) Proxy.newProxyInstance(Speakable.class.getClassLoader(), new Class<?>[] { Speakable.class },
                (proxy, method, args) -> {
                    if ("speak".equals(method.getName())) {
                        return send.apply((ActionDialect) args[1]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Field field(Object object, String name) throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    private static Map<?, ?> map(Object object, String name) throws Exception {
        return (Map<?, ?>) field(object, name).get(object);
    }

    private static List<?> list(Object object, String name) throws Exception {
        return (List<?>) field(object, name).get(object);
    }

    private static void setLong(Object object, String name, long value) throws Exception {
        field(object, name).setLong(object, value);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("latch timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        ++assertions;
    }

    private static class FakePerformer extends Performer {
        final RecordingHttpServer server = new RecordingHttpServer();
        final List<ActionDialect> requests = new CopyOnWriteArrayList<>();
        final AtomicInteger syncCalls = new AtomicInteger();
        volatile Function<ActionDialect, ActionDialect> syncResponse = request -> null;
        volatile boolean sendSuccess = true;
        volatile boolean throwOnSend;
        volatile boolean failListener;
        volatile CountDownLatch sendEntered;
        volatile CountDownLatch sendRelease;
        volatile Consumer<ActionDialect> callbackOnSend;

        FakePerformer(Nucleus nucleus) {
            super(nucleus);
        }

        @Override public HttpServer getHttpServer() { return this.server; }
        @Override public Endpoint getExternalHttpEndpoint() { return new Endpoint("127.0.0.1", 7010); }
        @Override public Endpoint getExternalHttpsEndpoint() { return new Endpoint("127.0.0.1", 7017); }

        @Override public void setListener(String cellet, PerformerListener listener) {
            super.setListener(cellet, listener);
            if (this.failListener) {
                throw new IllegalStateException("expected listener registration failure");
            }
        }

        @Override public boolean tryTransmit(String cellet, ActionDialect request) {
            this.requests.add(request);
            if (this.throwOnSend) {
                throw new IllegalStateException("expected send exception");
            }
            if (this.sendEntered != null) {
                this.sendEntered.countDown();
                await(this.sendRelease);
            }
            if (this.callbackOnSend != null) {
                this.callbackOnSend.accept(request);
            }
            return this.sendSuccess;
        }

        @Override public ActionDialect syncTransmit(String cellet, ActionDialect request) {
            this.syncCalls.incrementAndGet();
            return this.syncResponse.apply(request);
        }

        @Override public ActionDialect syncTransmit(String cellet, ActionDialect request, long timeout) {
            return this.syncTransmit(cellet, request);
        }

        ActionDialect lastRequest() {
            return this.requests.get(this.requests.size() - 1);
        }
    }

    private static class RecordingHttpServer extends HttpServer {
        int failAt = -1;
        @Override public void addContextHandler(ContextHandler handler) {
            if (this.getContextHandlers().size() == this.failAt) {
                throw new IllegalStateException("expected handler registration failure");
            }
            super.addContextHandler(handler);
        }
    }
}
