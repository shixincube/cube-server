/*
 * This source file is part of Cube.
 * Copyright (c) 2023-2026 Ambrose Xu.
 */
package cube.service.test;

import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.Packet;
import cube.common.entity.AICapability;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.Contact;
import cube.common.entity.MultimodalInput;
import cube.common.entity.MultimodalOutput;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.AIGCService;
import cube.service.aigc.channel.ChannelManager;
import cube.service.aigc.event.Event;
import cube.service.aigc.event.EventCenter;
import cube.service.aigc.event.MultimodalHandler;
import cube.service.aigc.listener.MultimodalListener;
import cube.service.aigc.unit.AIGCTaskExecutor;
import cube.service.aigc.unit.MultimodalUnitMeta;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JDK 8 下运行的无网络回归验证，覆盖真实服务入口、单元队列和应答处理。
 * classpath 包含 service/classes、common/classes 及第三方依赖。
 */
public class MultimodalStreamRoutingTest {

    private static final EventCenter CENTER = EventCenter.getInstance();
    private static int assertions;

    public static void main(String[] args) throws Exception {
        FakeCellet cellet = new FakeCellet();
        FakeService service = new FakeService(cellet);
        try {
            testControlRouting(service, cellet);
            testImmediateFollowup(service, cellet);
            testResponseRegistration(service, cellet);
            testRetentionAndCleanup(service);
            System.out.println("MultimodalStreamRoutingTest: " + assertions + " assertions passed");
        }
        finally {
            CENTER.stop();
            service.executor.shutdown();
        }
    }

    private static void testControlRouting(FakeService service, FakeCellet cellet) throws Exception {
        CENTER.stop();
        AIGCUnit owner = unit(100001L);
        service.selected = unit(100002L);
        MultimodalUnitMeta start = meta(service, owner, new Reply());
        CENTER.putUnitMeta("stream-a", start);
        cellet.reset("stream-a", "running");
        int selections = service.selections.get();
        for (String operation : new String[] {"status", "update", "stop", " STATUS "}) {
            Reply reply = new Reply();
            MultimodalInput input = input(operation, "stream-a");
            input.unit = "OmniVL";
            check(service.executeMultimodal("token", "channel", input, reply), "control accepted: " + operation);
            reply.await();
            check(cellet.context == owner.getContext(), "control sent to stream owner: " + operation);
            check("OmniGround".equals(cellet.request.getString("unit")), "owner capability forwarded");
            check("stream-a".equals(cellet.request.getJSONObject("option").getString("streamId")),
                    "streamId forwarded");
            check(CENTER.getUnitMeta("stream-a") == start, "control preserves original metadata");
        }
        check(service.selections.get() == selections, "controls bypass capacity scheduler");

        cellet.status = "stopping";
        Reply stop = new Reply();
        check(service.executeMultimodal("token", "channel", input("stop", "stream-a"), stop), "stop accepted");
        stop.await();
        check(CENTER.searchMultimodalUnitMeta(owner) == start, "stopping keeps stream occupied");
        new MultimodalHandler(service).onEvent(CENTER, new Event(new JSONObject()
                .put("name", "streamStopped").put("source", "OmniGround")
                .put("payload", new JSONObject().put("streamId", "stream-a"))));
        check(CENTER.searchMultimodalUnitMeta(owner) == null, "terminal event releases occupancy");
        cellet.status = "stopped";
        Reply terminal = new Reply();
        check(service.executeMultimodal("token", "channel", input("status", "stream-a"), terminal),
                "terminal status query accepted");
        terminal.await();
        check(cellet.context == owner.getContext(), "terminal query retains owner route");
        Reply repeatedStop = new Reply();
        check(service.executeMultimodal("token", "channel", input("stop", "stream-a"), repeatedStop),
                "repeated stop accepted");
        repeatedStop.await();
        check(cellet.context == owner.getContext(), "repeated stop retains owner route");

        int sent = cellet.calls.get();
        for (String operation : new String[] {"status", "stop", "update"}) {
            check(!service.executeMultimodal("token", "channel", input(operation, "unknown"), new Reply()),
                    "unknown stream rejected");
            check(!service.executeMultimodal("token", "channel", input(operation, null), new Reply()),
                    "missing streamId rejected");
        }
        ((FakeContext) owner.getContext()).valid = false;
        check(!service.executeMultimodal("token", "channel", input("stop", "stream-a"), new Reply()),
                "offline owner rejected");
        check(cellet.calls.get() == sent, "invalid controls never reach another Unit");
        check(service.selections.get() == selections, "invalid controls never fall back to scheduler");
        check(!service.channel.isProcessing(), "rejected requests leave channel available");
    }

    private static void testImmediateFollowup(FakeService service, FakeCellet cellet) throws Exception {
        CENTER.stop();
        service.selected = unit(100003L);
        AIGCUnit owner = service.selected;
        cellet.reset("immediate", "starting");
        Reply followup = new Reply();
        Reply start = new Reply() {
            @Override
            public void onResponse(AIGCChannel channel, MultimodalOutput output) {
                if (CENTER.getUnitMeta("immediate") == null
                        || !service.executeMultimodal("token", "channel", input("status", "immediate"), followup)) {
                    error = new AssertionError("immediate followup was rejected");
                }
                super.onResponse(channel, output);
            }
        };
        int selections = service.selections.get();
        check(service.executeMultimodal("token", "channel", input("start", null), start), "start accepted");
        start.await();
        followup.await();
        check(service.selections.get() == selections + 1, "only start uses capacity scheduler");
        check(cellet.context == owner.getContext(), "immediate followup reaches starting Unit");
    }

    private static void testResponseRegistration(FakeService service, FakeCellet cellet) throws Exception {
        for (String status : new String[] {"starting", "running", "reconnecting", "failed"}) {
            CENTER.stop();
            AIGCUnit owner = unit(100004L);
            cellet.reset("response-" + status, status);
            Reply reply = new Reply();
            MultimodalUnitMeta start = meta(service, owner, reply);
            service.channel.setProcessing(true);
            start.process();
            reply.await();
            check(CENTER.getUnitMeta(cellet.streamId) == start, "response registers route without usage: " + status);
            check((CENTER.searchMultimodalUnitMeta(owner) == null) == "failed".equals(status),
                    "response occupancy follows terminal status");
        }

        CENTER.stop();
        AIGCUnit owner = unit(100005L);
        cellet.reset("early-stop", "starting");
        cellet.stopBeforeResponse = true;
        Reply early = new Reply();
        MultimodalUnitMeta start = meta(service, owner, early);
        start.process();
        early.await();
        check(CENTER.getUnitMeta("early-stop") == start, "early stop still retains route");
        check(CENTER.searchMultimodalUnitMeta(owner) == null, "late start response cannot restore stopped occupancy");

        cellet.reset("channel-release", "running");
        Reply reentrant = new Reply() {
            @Override
            public void onResponse(AIGCChannel channel, MultimodalOutput output) {
                check(!channel.isProcessing(), "channel released before response callback");
                channel.setProcessing(true);
                super.onResponse(channel, output);
            }
        };
        meta(service, owner, reentrant).process();
        reentrant.await();
        check(service.channel.isProcessing(), "previous request does not release next request's channel");
        service.channel.setProcessing(false);

        cellet.reset("no-payload", "running");
        cellet.omitPayload = true;
        cellet.withUsage = true;
        Reply noPayload = new Reply();
        meta(service, owner, noPayload).process();
        noPayload.await();
        check(CENTER.getUnitMeta("no-payload") == null, "non-stream response registers no route");
    }

    private static void testRetentionAndCleanup(FakeService service) {
        CENTER.stop();
        AIGCUnit owner = unit(100006L);
        AIGCUnit other = unit(100007L);
        MultimodalUnitMeta otherMeta = meta(service, other, new Reply());
        CENTER.putUnitMeta("other", otherMeta);
        CENTER.markStreamStopped("other");
        for (int i = 0; i < 22; ++i) {
            String id = "retained-" + i;
            CENTER.putUnitMeta(id, meta(service, owner, new Reply()));
            CENTER.markStreamStopped(id);
        }
        check(CENTER.getUnitMeta("retained-0") == null && CENTER.getUnitMeta("retained-1") == null,
                "old terminal routes evicted");
        for (int i = 2; i < 22; ++i) {
            check(CENTER.getUnitMeta("retained-" + i) != null, "recent terminal route retained");
        }
        CENTER.putUnitMeta("active-1", meta(service, owner, new Reply()));
        CENTER.putUnitMeta("active-2", meta(service, owner, new Reply()));
        CENTER.removeUnitMeta(owner);
        check(CENTER.getUnitMeta("active-1") == null && CENTER.getUnitMeta("active-2") == null,
                "disconnect removes every active stream");
        check(CENTER.getUnitMeta("retained-21") == null, "disconnect removes terminal routes");
        check(CENTER.getUnitMeta("other") == otherMeta, "cleanup preserves other Unit's routes");
        CENTER.stop();
        check(CENTER.getUnitMeta("other") == null, "service shutdown clears routes");
    }

    private static MultimodalInput input(String operation, String streamId) {
        JSONObject option = new JSONObject().put("operation", operation);
        if (streamId != null) {
            option.put("streamId", streamId);
        }
        return new MultimodalInput(new JSONObject().put("unit", "OmniGround").put("content", "")
                .put("files", new JSONArray()).put("option", option));
    }

    private static AIGCUnit unit(long id) {
        return new AIGCUnit(new Contact(id, "domain"), new AICapability("OmniGround", "Multimodal", "1.0",
                Collections.singletonList("Grounding"), "test"), new FakeContext());
    }

    private static MultimodalUnitMeta meta(FakeService service, AIGCUnit unit, Reply reply) {
        return new MultimodalUnitMeta(service, unit, service.channel, input("start", null), reply);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        ++assertions;
    }

    private static Object field(Object object, Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static class Reply implements MultimodalListener {
        final CountDownLatch done = new CountDownLatch(1);
        volatile AssertionError error;

        @Override
        public void onResponse(AIGCChannel channel, MultimodalOutput output) {
            done.countDown();
        }

        @Override
        public void onFailed(AIGCChannel channel, AIGCStateCode code) {
            error = new AssertionError("request failed: " + code);
            done.countDown();
        }

        void await() throws Exception {
            check(done.await(5, TimeUnit.SECONDS), "response completed");
            if (error != null) {
                throw error;
            }
        }
    }

    private static class FakeContext extends TalkContext {
        boolean valid = true;

        FakeContext() {
            super(null, null);
        }

        @Override
        public boolean isValid() {
            return valid;
        }
    }

    private static class FakeCellet extends AIGCCellet {
        String streamId;
        String status;
        boolean stopBeforeResponse;
        boolean omitPayload;
        boolean withUsage;
        volatile TalkContext context;
        volatile JSONObject request;
        final AtomicInteger calls = new AtomicInteger();

        void reset(String id, String state) {
            streamId = id;
            status = state;
            stopBeforeResponse = omitPayload = withUsage = false;
        }

        @Override
        public ActionDialect transmit(TalkContext context, ActionDialect request, long timeout, long sn) {
            this.context = context;
            this.request = new Packet(request).data;
            calls.incrementAndGet();
            if (stopBeforeResponse) {
                CENTER.markStreamStopped(streamId);
            }
            JSONObject result = new JSONObject().put("response", "").put("thought", "");
            if (!omitPayload) {
                result.put("resultPayload", new JSONObject().put("streamId", streamId).put("status", status));
            }
            if (withUsage) {
                result.put("performance", new JSONObject().put("inputTokens", 0).put("outputTokens", 0));
            }
            return new Packet("multimodal", new JSONObject().put("code", 0).put("data", result)).toDialect();
        }
    }

    private static class FakeService extends AIGCService {
        final AIGCChannel channel;
        final AIGCTaskExecutor executor;
        final AtomicInteger selections = new AtomicInteger();
        AIGCUnit selected;

        @SuppressWarnings("unchecked")
        FakeService(FakeCellet cellet) throws Exception {
            super(cellet);
            started.set(true);
            executor = (AIGCTaskExecutor) field(this, AIGCService.class, "taskExecutor");
            executor.createExecutor(1, "fixed");
            AuthToken token = new AuthToken("token", "domain", "app", 1L,
                    new Date(), new Date(Long.MAX_VALUE), null, false);
            channel = new AIGCChannel(token, "test", "channel", Language.Chinese);
            ChannelManager manager = (ChannelManager) field(this, AIGCService.class, "channelManager");
            ((Map<String, AIGCChannel>) field(manager, ChannelManager.class, "channelMap")).put("channel", channel);
        }

        @Override
        public AIGCUnit selectUnitByName(String name) {
            selections.incrementAndGet();
            return selected;
        }
    }
}
