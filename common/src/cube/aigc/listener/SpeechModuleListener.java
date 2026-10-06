/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.aigc.listener;

import cube.common.entity.FileLabel;
import cube.common.entity.VoiceDiarization;

/**
 * 语音能力模块级监听器。
 *
 * <p><b>定位</b>：说话人分离是宿主的平台级基础能力，分离完成后的「收尾操作」
 * （角色映射、业务归档等）属各业务模块。本接口让模块以<b>订阅</b>方式挂入
 * 分离完成事件，而不必把业务逻辑写进宿主。</p>
 *
 * <p><b>典型分工</b>：心理学模块注册监听器把说话人映射为「来访者 / 咨询师」；
 * 其他调用者可能需要完整还原所有说话人——不注册监听器时得到的就是宿主的
 * 完整说话人集合（标签为归一化的占位名，不折叠、不丢失）。</p>
 *
 * <p><b>回调时机</b>：宿主在「指标分析完成后、请求级
 * {@link VoiceDiarizationListener} 回调前」扇出。故实现方拿到的
 * {@link VoiceDiarization} 已含指标（若该次请求开启指标分析），
 * 实现方对说话人标签的改写也会被请求级回调与落库看到。</p>
 *
 * <p><b>线程与异常</b>：回调在宿主的分离收尾线程上触发，实现方须自行保证
 * 线程安全；实现抛出的异常会被宿主捕获并记录，不影响其他监听器、
 * 请求级回调与结果落库。</p>
 */
public interface SpeechModuleListener {

    /**
     * 监听器名称，用于日志与重复注册诊断。
     *
     * @return 返回名称。
     */
    String getName();

    /**
     * 说话人分离完成。
     *
     * @param source 源音频文件标签。
     * @param diarization 分离结果，含指标（若该次请求开启指标分析）。
     */
    void onDiarizationCompleted(FileLabel source, VoiceDiarization diarization);
}
