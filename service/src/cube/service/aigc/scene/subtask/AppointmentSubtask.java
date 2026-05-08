/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.aigc.psychology.consultation.ConsultationTheme;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.guidance.Prompts;
import cube.service.aigc.listener.GenerateTextListener;
import cube.service.aigc.scene.SceneManager;
import cube.util.TimeUtils;

import java.util.Date;

public class AppointmentSubtask extends ConversationSubtask {

    public AppointmentSubtask(AIGCService service, AIGCChannel channel, String query,
                              ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                              GenerateTextListener listener) {
        super(Subtask.Appointment, service, channel, query, context, relation, convCtx, listener);
    }

    @Override
    public AIGCStateCode execute(Subtask roundSubtask) {
        final Appointment appointment = this.convCtx.getAppointment();
        if (null == appointment) {
            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = Prompts.getPrompt("ANSWER_NO_APPOINTMENT_DATA");
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);
                }
            });
            return AIGCStateCode.Ok;
        }

        if (roundSubtask == Subtask.StopAppointment) {
            // 取消子任务
            this.convCtx.deactivateSubtask();

            this.service.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    ComplexContext complexContext = new ComplexContext();
                    complexContext.setSubtask(Subtask.StopAppointment);

                    String answer = fastPolish(Prompts.getPrompt("ANSWER_INTERRUPT_APPOINTMENT"));

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = answer;
                    record.context = complexContext;
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            });
            return AIGCStateCode.Ok;
        }

        final String dataPrompt = Prompts.getPrompt("psy_appointment_data");
        if (null == dataPrompt) {
            Logger.e(this.getClass(), "#execute - Can NOT find the \"psy_appointment_data\" prompt");
            channel.setProcessing(false);
            return AIGCStateCode.Failure;
        }

        final String answerPrompt = Prompts.getPrompt("psy_appointment_answer");
        if (null == answerPrompt) {
            Logger.e(this.getClass(), "#execute - Can NOT find the \"psy_appointment_answer\" prompt");
            channel.setProcessing(false);
            return AIGCStateCode.Failure;
        }

        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final boolean[] mutex = new boolean[] { false, false };
                final MutableGeneratingRecord generatingAnswer = new MutableGeneratingRecord();

                (new Thread() {
                    @Override
                    public void run() {
                        try {
                            String memory = convCtx.getSubtaskMemory().toMarkdown();
                            memory += AppointmentSubtask.this.query;

                            String query = dataPrompt.replace("{{content}}", memory);
                            query = query.replace("{{today}}", TimeUtils.formatTodayFullDate());

                            GeneratingRecord record = service.syncGenerateText(channel.getAuthToken(),
                                    ModelConfig.BAIZE_NEXT_UNIT, query, null);
                            if (null != record) {
                                ConsultationTheme theme = extractConsultationTheme(record.answer);
                                Date date = extractConsultationDate(record.answer);

                                if (null != theme) {
                                    appointment.setConsultationTheme(theme);
                                }
                                if (null != date) {
                                    appointment.setConsultationDate(date.getTime());
                                }
                            }
                        } catch (Exception e) {
                            Logger.w(this.getClass(), "", e);
                        } finally {
                            mutex[0] = true;
                            synchronized (mutex) {
                                mutex.notify();
                            }
                        }
                    }
                }).start();

                (new Thread() {
                    @Override
                    public void run() {
                        try {
                            String memory = convCtx.getSubtaskMemory().toMarkdown();
                            memory += AppointmentSubtask.this.query;

                            String query = answerPrompt.replace("{{content}}", memory);
                            query = query.replace("{{today}}", TimeUtils.formatTodayFullDate());

                            GeneratingRecord record = service.syncGenerateText(channel.getAuthToken(),
                                    ModelConfig.BAIZE_NEXT_UNIT, query, null);
                            if (null != record) {
                                int index = record.answer.indexOf("：");
                                if (index > 1 && record.answer.startsWith("云宝")) {
                                    record.answer = record.answer.substring(index + 1);
                                }
                                generatingAnswer.setValue(record);
                            }
                        } catch (Exception e) {
                            Logger.w(this.getClass(), "", e);
                        } finally {
                            mutex[1] = true;
                            synchronized (mutex) {
                                mutex.notify();
                            }
                        }
                    }
                }).start();

                while (!mutex[0] || !mutex[1]) {
                    synchronized (mutex) {
                        try {
                            mutex.wait(2 * 60 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                String answer = null;
                if (appointment.isReady()) {
                    // 取消子任务
                    convCtx.deactivateSubtask();

                    // 数据已齐备
                    answer = appointment.makeConversation();

                    ComplexContext complexContext = new ComplexContext();
                    complexContext.setSubtask(Subtask.StopAppointment);

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = answer;
                    record.context = complexContext;
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
                else {
                    if (generatingAnswer.isNotNull()) {
                        answer = String.format("%s\n\n%s",
                                generatingAnswer.getValue().answer.trim(),
                                appointment.makeConversation());
                    }
                    else {
                        answer = appointment.makeConversation();
                    }

                    ComplexContext complexContext = new ComplexContext();
                    complexContext.setSubtask(Subtask.Appointment);

                    GeneratingRecord record = new GeneratingRecord(query);
                    record.answer = answer;
                    record.context = complexContext;
                    listener.onGenerated(channel, record);
                    channel.setProcessing(false);

                    // 建立记忆
                    convCtx.getSubtaskMemory().record(record);

                    SceneManager.getInstance().saveHistoryRecord(channel.getCode(), ModelConfig.AIXINLI,
                            convCtx, record);
                }
            }
        });

        return AIGCStateCode.Ok;
    }

    private ConsultationTheme extractConsultationTheme(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.length() <= 2) {
                continue;
            }
            for (ConsultationTheme theme : ConsultationTheme.values()) {
                if (line.contains(theme.nameCN) || line.contains(theme.nameEN)) {
                    return theme;
                }
            }
        }
        return null;
    }

    private Date extractConsultationDate(String text) {
        return TimeUtils.extractDate(text);
    }
}
