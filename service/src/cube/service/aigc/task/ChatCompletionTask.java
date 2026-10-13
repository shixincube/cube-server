package cube.service.aigc.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cube.aigc.completion.CompletionError;
import cube.benchmark.ResponseTime;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.service.ServiceTask;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.completion.ChatCompletionService;
import org.json.JSONObject;

public class ChatCompletionTask extends ServiceTask {
    public ChatCompletionTask(Cellet cellet, TalkContext context, Primitive primitive, ResponseTime time) {
        super(cellet, context, primitive, time);
    }

    @Override public void run() {
        ActionDialect dialect = new ActionDialect(primitive);
        Packet packet = new Packet(dialect);
        ChatCompletionService completions = ((AIGCCellet) cellet).getService().getCompletions();
        String token = getTokenCode(dialect);
        try {
            if (AIGCAction.ChatCompletion.name.equals(packet.name)) {
                completions.generate(token, talkContext, dialect);
            }
            else if (AIGCAction.CancelChatCompletion.name.equals(packet.name)) {
                completions.cancel(token, packet.data.optString("requestId"), talkContext);
            }
            else {
                cellet.speak(talkContext, makeResponse(dialect, packet, 0, completions.models(token)));
            }
        } catch (CompletionError error) {
            cellet.speak(talkContext, makeResponse(dialect, packet, 9, new JSONObject().put("error", error.toJSON())));
        } finally { markResponseTime(); }
    }
}
