/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.entity.VoiceDiarization;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.VoiceDiarizationListener;
import cube.service.aigc.scene.VoiceDiarizationIndicator;
import org.json.JSONObject;

public class AudioUnitMeta extends UnitMeta {

    protected AuthToken authToken;

    protected FileLabel file;

    protected AIGCAction action;

    public VoiceDiarizationListener voiceDiarizationListener;

    public AudioUnitMeta(AIGCService service, AIGCUnit unit, AuthToken authToken, AIGCAction action, FileLabel file) {
        super(service, unit);
        this.authToken = authToken;
        this.action = action;
        this.file = file;
    }

    @Override
    public void process() {
        if (null == this.voiceDiarizationListener) {
            Logger.w(this.getClass(), "#process - The listener is null: " + this.file.getFileCode());
        }

        JSONObject data = new JSONObject();
        data.put("fileLabel", this.file.toJSON());
        Packet request = new Packet(this.action.name, data);
        ActionDialect dialect = this.service.getCellet().transmit(this.unit.getContext(), request.toDialect(), 5 * 60 * 1000);
        if (null == dialect) {
            Logger.w(AIGCService.class, "#process - Audio unit error: " + this.file.getFileCode());
            // 回调错误
            if (null != this.voiceDiarizationListener) {
                this.voiceDiarizationListener.onFailed(this.file, AIGCStateCode.UnitError);
            }
            return;
        }

        Packet response = new Packet(dialect);
        if (AIGCStateCode.Ok.code != Packet.extractCode(response)) {
            Logger.w(AIGCService.class, "#process - Audio unit failed: " + this.file.getFileCode());
            // 回调错误
            if (null != this.voiceDiarizationListener) {
                this.voiceDiarizationListener.onFailed(this.file, AIGCStateCode.Failure);
            }
            return;
        }

        JSONObject payload = Packet.extractDataPayload(response);
        if (this.action == AIGCAction.SpeakerDiarization) {
            VoiceDiarization result = new VoiceDiarization(payload.getJSONObject("result"));
            // 补齐参数
            result.contactId = this.authToken.getContactId();
            result.title = "语音-" + this.file.getFileName();
            result.remark = "";

            if (Logger.isDebugLevel()) {
                Logger.d(this.getClass(), "#process Speaker Diarization result -\nfile: " + result.file.getFileCode() +
                        "\nelapsed: " + result.elapsed +
                        "\ntracks: " + result.tracks.size() +
                        "\nduration: " + result.duration);
            }

            // 执行分析
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    VoiceDiarizationIndicator voiceIndicator = new VoiceDiarizationIndicator(result.getId());
                    voiceIndicator.analyse(service, result);

                    if (null != voiceDiarizationListener) {
                        voiceDiarizationListener.onCompleted(file, result, voiceIndicator);
                    }
                }
            });
        }
        else {
            Logger.e(this.getClass(), "#process - Unknown action: " + this.action.name);
        }
    }
}
