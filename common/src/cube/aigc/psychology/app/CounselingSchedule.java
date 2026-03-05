/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.psychology.app;

import cube.aigc.psychology.ConsultationTheme;
import cube.aigc.psychology.Consultation;
import cube.aigc.psychology.CounselingScheduleState;
import cube.common.JSONable;
import cube.util.ConfigUtils;
import org.json.JSONObject;

/**
 * 咨询日程。
 */
public class CounselingSchedule implements JSONable {

    public final long id;

    public final long customerId;

    public long appointmentTime;

    public long appointmentDuration;

    public ConsultationTheme appointmentTheme;

    public Consultation appointmentConsultation;

    public CounselingScheduleState state;

    public long counselingTime;

    public long counselingDuration;

    public ConsultationTheme counselingTheme;

    public Consultation counselingConsultation;

    public String recordingName;

    public String recordingFileCode;

    public long voiceDiarizationId;

    public CounselingSchedule(long id, long customerId) {
        this.id = id;
        this.customerId = customerId;
    }

    public CounselingSchedule(CounselingSchedule src) {
        this.id = ConfigUtils.generateSerialNumber();
        this.customerId = src.customerId;
        this.appointmentTime = src.appointmentTime;
        this.appointmentDuration = src.appointmentDuration;
        this.appointmentTheme = src.appointmentTheme;
        this.appointmentConsultation = src.appointmentConsultation;
        this.state = src.state;
        this.counselingTime = src.counselingTime;
        this.counselingDuration = src.counselingDuration;
        this.counselingTheme = src.counselingTheme;
        this.counselingConsultation = src.counselingConsultation;
        this.recordingName = src.recordingName;
        this.recordingFileCode = src.recordingFileCode;
        this.voiceDiarizationId = src.voiceDiarizationId;
    }

    public CounselingSchedule(JSONObject json) {
        this.id = json.getLong("id");
        this.customerId = json.getLong("customerId");
        this.appointmentTime = json.getLong("appointmentTime");
        this.appointmentDuration = json.getLong("appointmentDuration");
        this.appointmentTheme = ConsultationTheme.parse(json.getString("appointmentTheme"));
        this.appointmentConsultation = Consultation.parse(json.getInt("appointmentConsultation"));
        this.state = CounselingScheduleState.parse(json.getInt("state"));
        this.counselingTime = json.getLong("counselingTime");
        this.counselingDuration = json.getLong("counselingDuration");
        this.counselingTheme = ConsultationTheme.parse(json.getString("counselingTheme"));
        this.counselingConsultation = Consultation.parse(json.getInt("counselingConsultation"));
        this.recordingName = json.has("recordingName") ? json.getString("recordingName") : null;
        this.recordingFileCode = json.has("recordingFileCode") ? json.getString("recordingFileCode") : null;
        this.voiceDiarizationId = json.has("voiceDiarizationId") ? json.getLong("voiceDiarizationId") : 0;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("customerId", this.customerId);
        json.put("appointmentTime", this.appointmentTime);
        json.put("appointmentDuration", this.appointmentDuration);
        json.put("appointmentTheme", this.appointmentTheme.code);
        json.put("appointmentConsultation", this.appointmentConsultation.code);
        json.put("state", this.state.code);
        json.put("counselingTime", this.counselingTime);
        json.put("counselingDuration", this.counselingDuration);
        json.put("counselingTheme", this.counselingTheme.code);
        json.put("counselingConsultation", this.counselingConsultation.code);
        if (null != this.recordingName) {
            json.put("recordingName", this.recordingName);
        }
        if (null != this.recordingFileCode) {
            json.put("recordingFileCode", this.recordingFileCode);
        }
        if (0 != this.voiceDiarizationId) {
            json.put("voiceDiarizationId", this.voiceDiarizationId);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
