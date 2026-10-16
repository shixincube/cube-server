package cube.service.test;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.aigc.ContactPreference;
import cube.aigc.ModelConfig;
import cube.aigc.Usage;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.AICapability;
import cube.common.entity.AIGCUnit;
import cube.common.entity.Contact;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.AIGCStorage;
import cube.service.aigc.unit.AIGCTaskExecutor;
import cube.service.aigc.unit.UnitScheduler;
import cube.storage.StorageType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Exercises the real service queue and persistent event path without inference hardware.
 */
public final class ChatCompletionServiceTest {
    static int assertions;

    static void check(boolean value, String message) {
        ++assertions;
        if (!value) throw new AssertionError(message);
    }

    static void until(BooleanSupplier predicate, String message) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!predicate.getAsBoolean() && System.nanoTime() < end) Thread.sleep(5);
        check(predicate.getAsBoolean(), message);
    }

    public static void main(String[] args) throws Exception {
        FakeCellet cellet = new FakeCellet();
        FakeService service = new FakeService(cellet);
        try {
            testModels(service);
            testEvents(service, cellet);
            testCancellation(service, cellet);
            testMalformedAndTimeout(service, cellet);
            testTwelveUnits();
            service.getCompletions().stop();
            check(service.getCompletions().activeRequests() == 0, "stop clears registry");
            until(() -> service.getGenerateTextUnitRealtimeCount().get("Baize2").get() == 0, "stop releases counters");
            System.out.println("ChatCompletionServiceTest: " + assertions + " assertions passed");
        } finally {
            service.getCompletions().stop();
            service.queue.shutdown();
        }
    }

    static void testModels(FakeService service) {
        check(service.getCompletions().models("token").getJSONArray("models").length() == 1, "only declared structured models listed");
        try {
            service.getCompletions().models("bad");
            throw new AssertionError("bad token accepted");
        } catch (cube.aigc.completion.CompletionError error) {
            check(error.status == 401, "token validation");
        }
        service.preference = new ContactPreference(1, new JSONArray().put("hidden"));
        check(service.getCompletions().models("token").getJSONArray("models").length() == 0, "account model selection respected");
        service.preference = null;
        UnitScheduler scheduler = new UnitScheduler();
        Contact contact = new Contact(500L, "domain");
        AICapability legacy = capability(false), updated = capability(true);
        AIGCUnit registered = scheduler.setup(contact, Collections.singletonList(legacy), service.owner).get(0);
        check(!registered.getCapability().getChatCompletion().optBoolean("text"), "older units default unsupported");
        scheduler.setup(contact, Collections.singletonList(updated), service.owner);
        check(registered.getCapability().getChatCompletion().getBoolean("text"), "capabilities refreshed on re-registration");
    }

    static JSONObject request(String model) {
        return new JSONObject().put("model", model).put("stream", true).put("options", new JSONObject())
                .put("messages", new JSONArray().put(new JSONObject().put("role", "system").put("content", "client-only"))
                        .put(new JSONObject().put("role", "user").put("content", "hello")));
    }

    static void generate(FakeService service, String id, long timeout) {
        ActionDialect dialect = new Packet(AIGCAction.ChatCompletion.name, new JSONObject().put("requestId", id)
                .put("request", request("model-a")).put("timeoutMs", timeout)).toDialect();
        dialect.addParam("token", "token");
        dialect.addParam("_performer", new JSONObject().put("sn", 7).put("ts", 1));
        service.getCompletions().generate("token", service.origin, dialect);
    }

    static void event(FakeService service, TalkContext context, String id, long sequence, String kind, JSONObject payload) {
        ActionDialect dialect = new Packet(AIGCAction.ChatCompletionEvent.name, new JSONObject().put("requestId", id)
                .put("sequence", sequence).put("kind", kind).put("payload", payload)).toDialect();
        service.getCompletions().onUnitEvent(context, dialect);
    }

    static JSONObject result() {
        return new JSONObject().put("message", new JSONObject().put("role", "assistant").put("content", "answer"))
                .put("finish_reason", "stop").put("usage", new JSONObject().put("prompt_tokens", 17).put("completion_tokens", 5).put("total_tokens", 22));
    }

    static void testEvents(FakeService service, FakeCellet cellet) throws Exception {
        generate(service, "events", 3000);
        until(() -> cellet.find("events", AIGCAction.ChatCompletion.name) != null, "unit received generation");
        check(cellet.find("events", AIGCAction.ChatCompletion.name).context == service.owner, "selected unit pinned");
        JSONObject dispatched = new Packet(cellet.find("events", AIGCAction.ChatCompletion.name).dialect).data;
        check(dispatched.getJSONObject("request").getJSONArray("messages").getJSONObject(0).getString("content").equals("client-only"), "no server context injected");
        event(service, service.other, "events", 1, "started", new JSONObject());
        check(cellet.events("events").size() == 0, "foreign unit events ignored");
        event(service, service.owner, "events", 1, "started", new JSONObject());
        event(service, service.owner, "events", 2, "delta", new JSONObject().put("delta", new JSONObject().put("content", "answer")));
        event(service, service.owner, "events", 3, "completed", new JSONObject().put("result", result()));
        event(service, service.owner, "events", 3, "completed", new JSONObject().put("result", result()));
        event(service, service.owner, "events", 4, "delta", new JSONObject().put("delta", new JSONObject().put("content", "late")));
        check(cellet.events("events").size() == 3, "continuous delivery and late event discard");
        check(cellet.events("events").get(2).dialect.containsParam("_performer"), "dispatcher routing preserved");
        check(service.storage.calls.get() == 1 && service.storage.input == 17 && service.storage.output == 5, "real usage recorded once");
        until(() -> service.getCompletions().activeRequests() == 0 && service.getGenerateTextUnitRealtimeCount().get("Baize2").get() == 0, "terminal registry and count release");
    }

    static void testCancellation(FakeService service, FakeCellet cellet) throws Exception {
        service.getCompletions().cancel("token", "overtaken", service.origin);
        generate(service, "overtaken", 3000);
        check(service.getCompletions().activeRequests() == 0 && cellet.find("overtaken", AIGCAction.ChatCompletion.name) == null, "early cancellation survives service task reordering");
        generate(service, "cancel", 3000);
        until(() -> cellet.find("cancel", AIGCAction.ChatCompletion.name) != null, "cancel task started");
        // Re-registering changes AIGCUnit.context; cancellation still belongs to its original connection.
        service.selected.setContext(service.other);
        service.getCompletions().cancel("bad", "cancel", service.origin);
        check(service.getCompletions().activeRequests() == 1, "wrong token cannot cancel");
        service.getCompletions().cancel("token", "cancel", service.other);
        check(service.getCompletions().activeRequests() == 1, "other dispatcher cannot cancel");
        service.getCompletions().cancel("token", "cancel", service.origin);
        check(cellet.find("cancel", AIGCAction.CancelChatCompletion.name).context == service.owner, "cancel uses original unit connection");
        until(() -> service.getCompletions().activeRequests() == 0 && service.getGenerateTextUnitRealtimeCount().get("Baize2").get() == 0, "cancellation releases resources");
        check(service.storage.calls.get() == 1, "cancel does not invent usage");
        service.selected.setContext(service.owner);
        generate(service, "blocking", 3000);
        until(() -> cellet.find("blocking", AIGCAction.ChatCompletion.name) != null, "queue blocker started");
        generate(service, "queued", 3000);
        service.getCompletions().cancel("token", "queued", service.origin);
        service.getCompletions().cancel("token", "blocking", service.origin);
        until(() -> service.getGenerateTextUnitRealtimeCount().get("Baize2").get() == 0, "queue drain released");
        check(cellet.find("queued", AIGCAction.ChatCompletion.name) == null, "queued cancellation does not dispatch");
    }

    static void testMalformedAndTimeout(FakeService service, FakeCellet cellet) throws Exception {
        for (String id : Arrays.asList("sequence", "usage", "malformed")) {
            generate(service, id, 3000);
            until(() -> cellet.find(id, AIGCAction.ChatCompletion.name) != null, "failure task started");
            if (id.equals("sequence")) event(service, service.owner, id, 2, "delta", new JSONObject());
            else if (id.equals("usage"))
                event(service, service.owner, id, 1, "completed", new JSONObject().put("result", result().put("usage", new JSONObject())));
            else event(service, service.owner, id, 1, "completed", new JSONObject());
            List<Sent> events = cellet.events(id);
            check(events.size() == 1 && new Packet(events.get(0).dialect).data.getString("kind").equals("error"), "malformed task failed");
            check(new Packet(events.get(0).dialect).data.getLong("sequence") == 1, "replacement terminal sequence has no gap");
            check(service.getCompletions().activeRequests() == 0, "failure releases listener");
        }
        generate(service, "timeout", 60);
        until(() -> service.getCompletions().activeRequests() == 0, "timeout releases listener");
        until(() -> service.getGenerateTextUnitRealtimeCount().get("Baize2").get() == 0, "timeout releases counter");
        check(service.storage.calls.get() == 1, "failed requests are not billed");
        generate(service, "stopping", 3000);
        until(() -> cellet.find("stopping", AIGCAction.ChatCompletion.name) != null, "stop task started");
    }

    static AICapability capability(boolean compatible) {
        AICapability capability = new AICapability("Baize2", "Multimodal", "1.0", Collections.singletonList("FeatureExtraction"), "test");
        if (compatible)
            capability.setChatCompletion(new JSONObject().put("text", true).put("stream", true).put("images", true).put("tools", true).put("parameters", new JSONArray()));
        return capability;
    }

    static final class Context extends TalkContext {
        Context() {
            super(null, null);
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public boolean equals(Object other) {
            return this == other;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    static final class Sent {
        final TalkContext context;
        final ActionDialect dialect;

        Sent(TalkContext context, Primitive primitive) {
            this.context = context;
            this.dialect = new ActionDialect(primitive);
        }
    }

    static final class FakeCellet extends AIGCCellet {
        final List<Sent> sent = new CopyOnWriteArrayList<>();

        @Override
        public boolean speak(TalkContext context, Primitive primitive) {
            sent.add(new Sent(context, primitive));
            return true;
        }

        Sent find(String id, String action) {
            for (Sent item : sent)
                if (item.dialect.getName().equals(action) && new Packet(item.dialect).data.optString("requestId").equals(id))
                    return item;
            return null;
        }

        List<Sent> events(String id) {
            List<Sent> result = new ArrayList<>();
            for (Sent item : sent)
                if (item.dialect.getName().equals(AIGCAction.ChatCompletionEvent.name) && new Packet(item.dialect).data.getString("requestId").equals(id))
                    result.add(item);
            return result;
        }
    }

    static final class FakeStorage extends AIGCStorage {
        final AtomicInteger calls = new AtomicInteger();
        long input, output, totalInput, totalOutput;

        FakeStorage() {
            super(StorageType.Other, new JSONObject());
        }

        @Override
        public synchronized Usage updateUsage(long cid, String model, long output, long input) {
            this.input = input;
            this.output = output;
            this.totalInput += input;
            this.totalOutput += output;
            calls.incrementAndGet();
            return new Usage(model, input, output, 0);
        }
    }

    static void testTwelveUnits() throws Exception {
        FakeCellet cellet = new FakeCellet();
        FleetService service = new FleetService(cellet);
        Map<String, TalkContext> owners = new LinkedHashMap<>();
        Set<TalkContext> selected = new HashSet<>();
        try {
            check(service.getCompletions().models("token").getJSONArray("models").length() == 2,
                    "twelve workers expose two model IDs");
            for (int i = 0; i < 12; ++i) {
                String id = "fleet-" + i, model = i < 8 ? "Baize" : "Baize2";
                ActionDialect dialect = new Packet(AIGCAction.ChatCompletion.name, new JSONObject()
                        .put("requestId", id).put("request", request(model))
                        .put("timeoutMs", i % 4 == 2 ? 1200 : 5000)).toDialect();
                dialect.addParam("token", "token");
                dialect.addParam("_performer", new JSONObject().put("sn", 7).put("ts", 1));
                service.getCompletions().generate("token", service.origin, dialect);
                until(() -> cellet.find(id, AIGCAction.ChatCompletion.name) != null, "fleet request dispatched");
                Sent sent = cellet.find(id, AIGCAction.ChatCompletion.name);
                owners.put(id, sent.context);
                check(selected.add(sent.context), "each concurrent request uses an idle worker");
                check(new Packet(sent.dialect).data.getString("unit").equals(model), "fleet preserves model choice");
                event(service, service.other, id, 1, "started", new JSONObject());
                check(cellet.events(id).isEmpty(), "foreign worker cannot emit into a fleet stream");
                event(service, sent.context, id, 1, "started", new JSONObject());
                event(service, sent.context, id, 2, "delta", new JSONObject().put("delta", new JSONObject().put("content", id)));
            }
            check(service.getCompletions().activeRequests() == 12, "all twelve requests coexist");
            check(service.getGenerateTextUnitRealtimeCount().get("Baize").get() == 8 &&
                    service.getGenerateTextUnitRealtimeCount().get("Baize2").get() == 4, "8+4 running counters");
            // A reconnect must not move an existing request's event/cancellation ownership.
            for (AIGCUnit unit : service.scheduler.getAll()) unit.setContext(new Context());
            for (int i = 0; i < 12; ++i) {
                String id = "fleet-" + i;
                if (i % 4 == 0) {
                    service.getCompletions().cancel("token", id, service.origin);
                    check(cellet.find(id, AIGCAction.CancelChatCompletion.name).context == owners.get(id),
                            "fleet cancellation uses original connection after reconnect");
                } else if (i % 4 == 1) {
                    event(service, owners.get(id), id, 3, "error", new JSONObject().put("error",
                            new JSONObject().put("status", 502).put("code", "upstream_error").put("message", "controlled failure")));
                } else if (i % 4 == 3) {
                    JSONObject result = result();
                    result.getJSONObject("message").put("content", id);
                    event(service, owners.get(id), id, 3, "completed", new JSONObject().put("result", result));
                    event(service, owners.get(id), id, 3, "completed", new JSONObject().put("result", result));
                }
            }
            until(() -> service.getCompletions().activeRequests() == 0, "fleet timeout/cancel/failure clear registry");
            until(() -> service.getGenerateTextUnitRealtimeCount().get("Baize").get() == 0 &&
                    service.getGenerateTextUnitRealtimeCount().get("Baize2").get() == 0, "fleet releases both counters");
            check(service.storage.calls.get() == 3 && service.storage.totalInput == 51 && service.storage.totalOutput == 15,
                    "only three successful requests settle exact usage once");
            for (int i = 0; i < 12; ++i) {
                String id = "fleet-" + i;
                List<Sent> events = cellet.events(id);
                JSONObject last = new Packet(events.get(events.size() - 1).dialect).data;
                check(last.getString("kind").equals(i % 4 == 3 ? "completed" : "error"), "fleet terminal kind");
                if (i % 4 == 3) check(last.getJSONObject("payload").getJSONObject("result")
                        .getJSONObject("message").getString("content").equals(id), "fleet results never cross streams");
                int count = events.size();
                event(service, owners.get(id), id, 99, "delta", new JSONObject().put("delta", new JSONObject().put("content", "late")));
                check(cellet.events(id).size() == count, "late fleet event discarded");
            }
            for (AIGCUnit unit : service.scheduler.getAll()) check(unit.toJSON().getInt("runningCounter") == 0,
                    "all individual worker counters released");
        } finally {
            service.getCompletions().stop();
            service.queue.shutdown();
        }
    }

    static final class FleetService extends FakeService {
        final UnitScheduler scheduler = new UnitScheduler();

        FleetService(FakeCellet cellet) throws Exception {
            super(cellet);
            for (int i = 0; i < 12; ++i) {
                AICapability capability = new AICapability(i < 8 ? "Baize" : "Baize2", "Text", "1.0",
                        Collections.singletonList("FeatureExtraction"), "fleet");
                capability.setChatCompletion(new JSONObject().put("text", true).put("stream", true)
                        .put("parameters", new JSONArray()));
                AIGCUnit unit = scheduler.setup(new Contact(1000L + i, "domain"),
                        Collections.singletonList(capability), new Context()).get(0);
                unit.setTimestamp(0); // Workers have completed their registration warmup.
            }
        }

        @Override public List<ModelConfig> getModelConfigs() {
            return Arrays.asList(new ModelConfig("Baize", "Baize", "", "http://localhost/", new JSONObject().put("unit", "Baize")),
                    new ModelConfig("Baize2", "Baize2", "", "http://localhost/", new JSONObject().put("unit", "Baize2")));
        }
        @Override public List<AIGCUnit> getAllUnits() { return scheduler.getAll(); }
        @Override public AIGCUnit selectUnitByName(String name) { return scheduler.select(name); }
    }

    static class FakeService extends AIGCService {
        final AIGCTaskExecutor queue;
        final FakeStorage storage = new FakeStorage();
        final Context owner = new Context(), other = new Context(), origin = new Context();
        final AIGCUnit selected = new AIGCUnit(new Contact(100L, "domain"), capability(true), owner);
        final AuthToken token = new AuthToken("token", "domain", "app", 1L, new Date(), new Date(Long.MAX_VALUE), null, false);
        ContactPreference preference;

        FakeService(FakeCellet cellet) throws Exception {
            super(cellet);
            started.set(true);
            Field field = AIGCService.class.getDeclaredField("taskExecutor");
            field.setAccessible(true);
            queue = (AIGCTaskExecutor) field.get(this);
            queue.createExecutor(2, "fixed");
        }

        @Override
        public AuthToken getToken(String code) {
            return code.equals("token") ? token : null;
        }

        @Override
        public ContactPreference getPreference(long id) {
            return preference;
        }

        @Override
        public List<ModelConfig> getModelConfigs() {
            return Collections.singletonList(new ModelConfig("model-a", "Model A", "", "http://localhost/", new JSONObject().put("unit", "Baize2")));
        }

        @Override
        public List<ModelConfig> getModelConfigs(JSONArray models) {
            return Collections.emptyList();
        }

        @Override
        public List<AIGCUnit> getAllUnits() {
            return Collections.singletonList(selected);
        }

        @Override
        public AIGCUnit selectUnitByName(String name) {
            return name.equals("Baize2") ? selected : null;
        }

        @Override
        public AIGCStorage getStorage() {
            return storage;
        }
    }
}
