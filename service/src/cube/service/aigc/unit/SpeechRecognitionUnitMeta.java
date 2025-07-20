/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.entity.SpeechRecognitionInfo;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.AutomaticSpeechRecognitionListener;
import org.json.JSONObject;

public class SpeechRecognitionUnitMeta extends UnitMeta {

//        protected final static String PROMPT = "合理使用标点符号给以下文本断句并修正错别字：%s";

    protected FileLabel file;

    protected AutomaticSpeechRecognitionListener listener;

    public SpeechRecognitionUnitMeta(AIGCService service, AIGCUnit unit, FileLabel file,
                                     AutomaticSpeechRecognitionListener listener) {
        super(service, unit);
        this.file = file;
        this.listener = listener;
    }

    @Override
    public void process() {
        JSONObject data = new JSONObject();
        data.put("fileLabel", this.file.toJSON());
        Packet request = new Packet(AIGCAction.AutomaticSpeechRecognition.name, data);
        ActionDialect dialect = this.service.getCellet().transmit(this.unit.getContext(), request.toDialect(), 5 * 60 * 1000);
        if (null == dialect) {
            Logger.w(AIGCService.class, "#process - Speech unit error: " + this.file.getFileCode());
            // 回调错误
            this.listener.onFailed(this.file, AIGCStateCode.UnitError);
            return;
        }

        Packet response = new Packet(dialect);
        if (AIGCStateCode.Ok.code != Packet.extractCode(response)) {
            Logger.w(AIGCService.class, "#process - Speech unit failed: " + this.file.getFileCode());
            this.listener.onFailed(this.file, AIGCStateCode.Failure);
            return;
        }

        JSONObject payload = Packet.extractDataPayload(response);
        SpeechRecognitionInfo info = new SpeechRecognitionInfo(payload.getJSONObject("result"));

        if (Logger.isDebugLevel()) {
            Logger.d(this.getClass(), "#process STT result -\nfile: " + info.file.getFileCode() +
                    "\nelapsed: " + info.elapsed +
                    "\nlang: " + info.lang +
                    "\ntext: " + info.text +
                    "\nduration: " + info.durationInSeconds);
        }

        // 进行标点断句
//            String prompt = String.format(PROMPT, info.getText());
//            AIGCUnit unit = selectUnitByName(ModelConfig.BAIZE_UNIT);
//            if (null != unit) {
//                GeneratingRecord result = syncGenerateText(unit, prompt, null, null, null);
//                if (null != result) {
//                    info.setText(result.answer);
//                }
//            }
        this.listener.onCompleted(this.file, info);
    }
}
