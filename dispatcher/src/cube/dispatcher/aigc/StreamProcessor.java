/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc;

import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.dispatcher.stream.Stream;
import cube.dispatcher.stream.StreamType;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StreamProcessor {

    private Map<String, List<Stream>> speechRecognitionCache;

    private Timer timer;

    private ExecutorService executorService;

    private String filePath = "cache/";

    public StreamProcessor() {
        this.speechRecognitionCache = new HashMap<>();

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new Daemon(), 30 * 1000, 1000);

        this.executorService = Executors.newCachedThreadPool();
    }

    public void stop() {
        this.timer.purge();
        this.timer.cancel();
    }

    public void receive(Stream stream) {
        if (stream.getType() == StreamType.SpeechRecognition) {
            synchronized (this.speechRecognitionCache) {
                List<Stream> streams = this.speechRecognitionCache.computeIfAbsent(stream.name, k -> new ArrayList<>());
                synchronized (streams) {
                    streams.add(stream);
                }
            }
        }
        else {
            Logger.w(this.getClass(), "#receive - stream type is unknown: " + stream.type);
        }
    }

    protected class Daemon extends TimerTask {

        @Override
        public void run() {
            synchronized (speechRecognitionCache) {
                Iterator<Map.Entry<String, List<Stream>>> iter = speechRecognitionCache.entrySet().iterator();
                while (iter.hasNext()) {
                    List<Stream> streams = iter.next().getValue();
                    if (streams.size() >= 20) {
                        executorService.execute(new StreamTask(streams));
                    }
                }
            }
        }
    }

    protected class StreamTask implements Runnable {

        private final int sampleRate = 16000;
        private final int sampleSizeInBits = 16;
        private final int channels = 1;

        private List<Stream> streams;

        public StreamTask(List<Stream> streams) {
            this.streams = streams;
        }

        @Override
        public void run() {
            synchronized (this.streams) {
                if (this.streams.isEmpty()) {
                    return;
                }
            }

            FlexibleByteBuffer buf = new FlexibleByteBuffer();
            synchronized (this.streams) {
                for (Stream stream : this.streams) {
                    buf.put(stream.data);
                }
                this.streams.clear();
            }

            buf.flip();
            byte[] data = new byte[buf.limit()];
            System.arraycopy(buf.array(), 0, data, 0, data.length);

            AudioFormat format = new AudioFormat(this.sampleRate, this.sampleSizeInBits,
                    this.channels, true, false);
            AudioInputStream audioStream = new AudioInputStream(new ByteArrayInputStream(data), format,
                    data.length / format.getFrameSize());

            File wavFile = new File("output.wav");
            try {
                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, wavFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
