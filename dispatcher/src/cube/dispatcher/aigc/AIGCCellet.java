/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cube.core.AbstractCellet;
import cube.dispatcher.Performer;
import cube.dispatcher.filestorage.FileStorageCellet;
import cube.dispatcher.stream.Stream;
import cube.dispatcher.stream.StreamListener;
import cube.dispatcher.stream.StreamType;
import cube.dispatcher.stream.Track;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AIGC 服务单元。
 */
public class AIGCCellet extends AbstractCellet {

    public final static String NAME = "AIGC";

    /**
     * 执行机。
     */
    private Performer performer;

    /**
     * 任务对象的缓存队列。
     */
    private ConcurrentLinkedQueue<PassThroughTask> taskQueue;

    private StreamProcessor processor;

    public AIGCCellet() {
        super(NAME);
        this.taskQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public boolean install() {
        this.performer = (Performer) this.getNucleus().getParameter("performer");
        Manager.getInstance().start(this.performer);

        this.processor = new StreamProcessor(this.performer,
                (FileStorageCellet) this.performer.getCellet(FileStorageCellet.NAME));

        this.performer.getStreamServer().setListener(StreamType.SpeakerDiarization,
                new SpeechStreamListener(this.processor));

        return true;
    }

    @Override
    public void uninstall() {
        this.processor.stop();

        this.performer.getStreamServer().removeListener(StreamType.SpeakerDiarization);

        Manager.getInstance().stop();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        this.performer.execute(this.borrowTask(talkContext, primitive));
    }

    protected PassThroughTask borrowTask(TalkContext talkContext, Primitive primitive) {
        PassThroughTask task = this.taskQueue.poll();
        if (null == task) {
            task = new PassThroughTask(this, talkContext, primitive, this.performer);
            task.responseTime = this.markResponseTime(task.getAction().getName());
            return task;
        }

        task.reset(talkContext, primitive);
        task.responseTime = this.markResponseTime(task.getAction().getName());
        return task;
    }

    protected void returnTask(PassThroughTask task) {
        task.markResponseTime();

        this.taskQueue.offer(task);
    }

    public StreamProcessor getStreamProcessor() {
        return this.processor;
    }

    protected class SpeechStreamListener implements StreamListener {

        private final StreamProcessor processor;

        public SpeechStreamListener(StreamProcessor processor) {
            this.processor = processor;
        }

        @Override
        public void onStream(Track track, Stream stream) {
            this.processor.receive(track, stream);
        }
    }
}
