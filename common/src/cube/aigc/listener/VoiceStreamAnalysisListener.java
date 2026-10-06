/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.listener;

import cube.common.entity.VoiceStreamSink;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;

/**
 * 语音流分析监听器。
 *
 * <p><b>为何在 common 而非宿主</b>：说话人分离由宿主的音频单元完成，
 * 而分片登记、归档与策略生成属咨询业务。回调横跨两侧，故接口须在
 * 两者都能看见的位置。</p>
 *
 * <p>回调在分离完成的线程上触发，实现方须自行保证线程安全。</p>
 */
public interface VoiceStreamAnalysisListener {

    void onCompleted(FileLabel source, VoiceStreamSink streamSink);

    void onFailed(FileLabel source, AIGCStateCode stateCode);
}
