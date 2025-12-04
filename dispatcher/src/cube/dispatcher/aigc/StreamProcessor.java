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
import cube.util.AudioUtils;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StreamProcessor {

    private Map<String, List<Stream>> speechRecognitionCache;

    private Timer timer;

    private ExecutorService executorService;

    private String filePath = "cache/";

    private Map<String, Queue<File>> fileFragmentMap;

    public StreamProcessor() {
        this.speechRecognitionCache = new HashMap<>();

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new Daemon(), 30 * 1000, 1000);

        this.executorService = Executors.newCachedThreadPool();

        this.fileFragmentMap = new ConcurrentHashMap<>();
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
                    if (streams.size() >= 100) {
                        executorService.execute(new StreamTask(streams.get(0).name, streams));
                    }
                }
            }
        }
    }

    protected class StreamTask implements Runnable {

        private String name;

        private List<Stream> streams;

        public StreamTask(String name, List<Stream> streams) {
            this.name = name;
            this.streams = streams;
        }

        @Override
        public void run() {
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

            // PCM 转 WAV
            byte[] waveData = AudioUtils.pcmToWav(data);

            Queue<File> files = fileFragmentMap.get(this.name);
            if (null == files) {
                files = new ConcurrentLinkedQueue<>();
                fileFragmentMap.put(this.name, files);
            }

            String filename = "sr-" + this.name + "-" + String.format("%03d", files.size()) + ".wav";
            File file = new File(filePath, filename);
            files.add(file);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                fos.write(waveData);
                fos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != fos) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        // Nothing
                    }
                }
            }

//            AudioFormat format = new AudioFormat(this.sampleRate, this.sampleSizeInBits,
//                    this.channels, true, false);
//            AudioInputStream audioStream = new AudioInputStream(new ByteArrayInputStream(data), format,
//                    data.length / format.getFrameSize());
//            try {
//                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, wavFile);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
    }
}
