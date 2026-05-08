/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.common.entity;

import cube.aigc.psychology.app.Link;
import cube.aigc.psychology.consultation.ConsultationTheme;
import cube.util.TimeUtils;

import java.util.Calendar;

public class Appointment extends Entity {

    private final String instruction = "我会根据您的情况和需求为您安排专业的咨询师为您服务。";

    private long userId;

    private ConsultationTheme consultationTheme;

    private long consultationDate;

    private Calendar dateCalendar;

    public Appointment(long userId) {
        super();
        this.userId = userId;
    }

    public Appointment(long userId, ConsultationTheme consultationTheme) {
        this(userId);
        this.consultationTheme = consultationTheme;
    }

    public String getInstruction() {
        return this.instruction;
    }

    public long getUserId() {
        return this.userId;
    }

    public void setConsultationTheme(ConsultationTheme theme) {
        this.consultationTheme = theme;
    }

    public ConsultationTheme getConsultationTheme() {
        return this.consultationTheme;
    }

    public void setConsultationDate(long value) {
        this.consultationDate = value;
        if (value > 0) {
            this.dateCalendar = Calendar.getInstance();
            this.dateCalendar.setTimeInMillis(value);
        }
        else {
            this.dateCalendar = null;
        }
    }

    public long getConsultationDate() {
        return this.consultationDate;
    }

    public boolean isReady() {
        if (null == this.dateCalendar) {
            return false;
        }

        int hour = this.dateCalendar.get(Calendar.HOUR);
        if (hour == 0) {
            return false;
        }

        if (this.dateCalendar.getTimeInMillis() - System.currentTimeMillis() < 8 * 60 * 60 * 1000) {
            // 间隔小于8小时
            return false;
        }

        return (null != this.consultationTheme);
    }

    public String makeConversation() {
        if (null == this.consultationTheme) {
            StringBuilder buf = new StringBuilder();
            buf.append("您想咨询哪个主题内容：\n\n");
            for (ConsultationTheme theme : ConsultationTheme.values()) {
                buf.append("- ");
                buf.append(Link.formatPromptDirectMarkdown(theme.nameCN, "我想咨询" + theme.nameCN));
                buf.append("\n");
            }
            buf.append("\n");
            buf.append("请选择一个您期望的咨询主题。");
            return buf.toString();
        }

        if (0 == this.consultationDate) {
            StringBuilder buf = new StringBuilder();
            buf.append("您想预约哪天线上咨询？");
            return buf.toString();
        }

        if (null != this.dateCalendar) {
            int hour = this.dateCalendar.get(Calendar.HOUR);
            if (hour == 0) {
                StringBuffer buf = new StringBuffer();
                buf.append("您预约的咨询日期是");
                buf.append(TimeUtils.formatDateYMD(this.dateCalendar.getTimeInMillis()));
                buf.append("。请问您想预约的具体时间是几点？");
                return buf.toString();
            }

            if (this.dateCalendar.getTimeInMillis() - System.currentTimeMillis() < 8 * 60 * 60 * 1000) {
                StringBuffer buf = new StringBuffer();
                buf.append("您想预约的咨询日期是");
                buf.append(TimeUtils.formatDateYMDH(this.dateCalendar.getTimeInMillis()));
                buf.append("。这个时间不在预约范围内，我建议您预约其他时间。");
                return buf.toString();
            }
        }

        StringBuilder buf = new StringBuilder();
        buf.append("您已经预约了");
        buf.append(TimeUtils.formatDateYMDH(this.consultationDate));
        buf.append("的咨询，");
        buf.append("咨询主题是");
        buf.append(this.consultationTheme.nameCN);
        buf.append("。\n\n");
        buf.append("咨询师会随后向您进行预约确认。");
        return buf.toString();
    }
}
