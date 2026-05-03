/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.common.entity;

import cube.aigc.psychology.consultation.ConsultationTheme;

public class Appointment extends Entity {

    private String instruction = "我会根据您的情况和需求为您安排专业的咨询师为您服务。";

    private ConsultationTheme consultationTheme;

    private long appointmentDate;

    public Appointment() {
        super();
    }

    public Appointment(ConsultationTheme consultationTheme) {
        super();
        this.consultationTheme = consultationTheme;
    }

    public String getInstruction() {
        return this.instruction;
    }

    public void setConsultationTheme(ConsultationTheme theme) {
        this.consultationTheme = theme;
    }

    public ConsultationTheme getConsultationTheme() {
        return this.consultationTheme;
    }


}
