package cube.dispatcher.test;

import cell.api.Nucleus;
import cell.core.net.Endpoint;
import cell.core.talk.dialect.ActionDialect;
import cube.aigc.completion.CompletionError;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.dispatcher.Director;
import cube.dispatcher.Performer;
import cube.dispatcher.Scope;
import cube.dispatcher.aigc.completion.CompletionBackend;
import cube.dispatcher.aigc.completion.CompletionGateway;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests continuous subscriptions, node pinning, reordering and shutdown.
 */
public final class CompletionGatewayTest {
    static int assertions;

    static void check(boolean value, String message) {
        ++assertions;
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Nucleus nucleus = new Nucleus();
        FakePerformer performer = new FakePerformer(nucleus);
        CompletionGateway gateway = new CompletionGateway();
        try {
            gateway.start(performer);
            check(gateway.models("token", 1000).length() == 1, "models action");
            List<JSONObject> first = new CopyOnWriteArrayList<>(), second = new CopyOnWriteArrayList<>();
            CompletionBackend.Subscription subscription = gateway.generate("token", "one", new JSONObject(), 1000, first::add);
            check(performer.lastDirector == performer.a, "initial director selected");
            performer.selected = performer.b;
            gateway.generate("token", "two", new JSONObject(), 1000, second::add);
            check(performer.lastDirector == performer.b, "second director selected independently");
            ExecutorService emitters = Executors.newFixedThreadPool(2);
            try {
                Future<?> a = emitters.submit(() -> {
                    for (int i = 1; i <= 20; ++i) event(gateway, "one", i, "delta");
                });
                Future<?> b = emitters.submit(() -> {
                    for (int i = 1; i <= 20; ++i) event(gateway, "two", i, "delta");
                });
                a.get();
                b.get();
            } finally {
                emitters.shutdownNow();
            }
            check(first.size() == 20 && second.size() == 20, "persistent subscriptions receive all events");
            for (int i = 0; i < 20; ++i)
                check(first.get(i).getString("requestId").equals("one") && second.get(i).getString("requestId").equals("two"), "no cross-stream delivery");
            event(gateway, "one", 20, "delta");
            check(first.size() == 20, "duplicate event ignored");
            subscription.cancel();
            check(performer.lastDirector == performer.a && performer.last.getName().equals(AIGCAction.CancelChatCompletion.name), "cancel pinned to original director");
            check(performer.last.getParamAsString("token").equals("token"), "cancel authentication preserved");
            event(gateway, "one", 21, "delta");
            check(first.size() == 20, "late cancelled event ignored");
            event(gateway, "two", 21, "completed");
            check(gateway.activeRequests() == 0, "completed subscription removed");
            List<JSONObject> errors = new ArrayList<>();
            gateway.generate("token", "gap", new JSONObject(), 1000, errors::add);
            event(gateway, "gap", 2, "delta");
            check(errors.size() == 1 && errors.get(0).getString("kind").equals("error"), "missing sequence fails immediately");
            check(gateway.activeRequests() == 0, "broken sequence releases registry");
            errors.clear();
            performer.sendSuccess = false;
            gateway.generate("token", "unavailable", new JSONObject(), 1000, errors::add);
            check(errors.size() == 1 && gateway.activeRequests() == 0, "transport failure cleanup");
            performer.sendSuccess = true;
            AtomicInteger stopped = new AtomicInteger();
            for (int i = 0; i < 128; ++i)
                gateway.generate("token", "stop-" + i, new JSONObject(), 1000, event -> stopped.incrementAndGet());
            try {
                gateway.generate("token", "full", new JSONObject(), 1000, event -> {
                });
                throw new AssertionError("queue limit absent");
            } catch (CompletionError error) {
                check(error.status == 429, "dispatcher admission bound");
            }
            gateway.stop();
            check(gateway.activeRequests() == 0 && stopped.get() == 128, "stop cancels and notifies every request");
            try {
                gateway.models("token", 1000);
                throw new AssertionError("stopped gateway accepted request");
            } catch (CompletionError error) {
                check(error.status == 503, "stopped gateway unavailable");
            }
            System.out.println("CompletionGatewayTest: " + assertions + " assertions passed");
        } finally {
            gateway.stop();
        }
    }

    static void event(CompletionGateway gateway, String id, int sequence, String kind) {
        gateway.onEvent(new Packet(AIGCAction.ChatCompletionEvent.name, new JSONObject().put("requestId", id)
                .put("sequence", sequence).put("kind", kind).put("payload", new JSONObject())).toDialect());
    }

    static class FakePerformer extends Performer {
        final Director a = new Director(new Endpoint("127.0.0.1", 1), null, new Scope());
        final Director b = new Director(new Endpoint("127.0.0.1", 2), null, new Scope());
        Director selected = a, lastDirector;
        ActionDialect last;
        boolean sendSuccess = true;

        FakePerformer(Nucleus nucleus) {
            super(nucleus);
        }

        @Override
        public Director selectDirector(String token, String cellet) {
            return selected;
        }

        @Override
        public boolean tryTransmit(Director director, String cellet, ActionDialect dialect) {
            lastDirector = director;
            last = dialect;
            return sendSuccess;
        }

        @Override
        public ActionDialect syncTransmit(Director director, String cellet, ActionDialect dialect, long timeout) {
            return new Packet(AIGCAction.ListChatModels.name, new JSONObject().put("code", 0)
                    .put("data", new JSONObject().put("models", new JSONArray().put(new JSONObject().put("id", "model"))))).toDialect();
        }
    }
}
