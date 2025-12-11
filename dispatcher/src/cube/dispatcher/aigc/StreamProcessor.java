/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc;

import cell.core.talk.dialect.ActionDialect;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.action.AIGCAction;
import cube.common.action.FileStorageAction;
import cube.common.entity.AudioStreamSink;
import cube.common.state.FileStorageStateCode;
import cube.dispatcher.Performer;
import cube.dispatcher.filestorage.FileStorageCellet;
import cube.dispatcher.stream.Stream;
import cube.dispatcher.stream.StreamType;
import cube.dispatcher.stream.Track;
import cube.util.AudioUtils;
import cube.util.FileLabels;
import cube.util.FileUtils;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StreamProcessor {

    private final Performer performer;

    private final FileStorageCellet fileStorageCellet;

    private Map<String, List<Stream>> speechRecognitionCache;

    private Timer timer;

    private ExecutorService executorService;

    private String filePath = "cache/";

    private Map<String, List<String>> streamFileCodeMap;

    /**
     * Key: Stream name
     */
    private Map<String, Register> registerMap;

    public StreamProcessor(Performer performer, FileStorageCellet fileStorageCellet) {
        this.performer = performer;
        this.fileStorageCellet = fileStorageCellet;

        this.registerMap = new ConcurrentHashMap<>();
        this.speechRecognitionCache = new HashMap<>();

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new Daemon(), 10 * 1000, 1000);

        this.executorService = Executors.newCachedThreadPool();

        this.streamFileCodeMap = new ConcurrentHashMap<>();
    }

    public void stop() {
        this.timer.cancel();
    }

    public void register(String streamName, AuthToken authToken) {
        Register register = new Register(streamName, authToken);
        this.registerMap.put(streamName, register);
    }

    public void receive(Track track, Stream stream) {
        if (stream.getType() == StreamType.SpeechRecognition) {
            synchronized (this.speechRecognitionCache) {
                List<Stream> streams = this.speechRecognitionCache.computeIfAbsent(stream.name, k -> new ArrayList<>());
                synchronized (streams) {
                    streams.add(stream);
                }
            }

            Register register =  this.registerMap.get(stream.name);
            if (null != register && null == register.track) {
                register.track = track;
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
                    if (streams.size() >= 64) {
                        executorService.execute(new StreamTask(streams.get(0).name, streams));
                    }
                }
            }

            long time = System.currentTimeMillis();

            Iterator<Map.Entry<String, Register>> iter = registerMap.entrySet().iterator();
            while (iter.hasNext()) {
                Register register = iter.next().getValue();
                if (time - register.refresh > 5 * 60 * 1000) {
                    // 删除过期数据
                    speechRecognitionCache.remove(register.streamName);
                    streamFileCodeMap.remove(register.streamName);
                    iter.remove();
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
            Register register = registerMap.get(this.name);
            if (null == register) {
                Logger.w(this.getClass(), "#run - No token for stream: " + this.name);
                synchronized (this.streams) {
                    this.streams.clear();
                }
                return;
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

            register.refresh = System.currentTimeMillis();

            // PCM 转 WAV
            byte[] waveData = AudioUtils.pcmToWav(data);

            List<String> fileLabels = streamFileCodeMap.computeIfAbsent(this.name, k -> new ArrayList<>());
            int index = fileLabels.size();
            String filename = "sr-" + this.name + "-" + String.format("%03d", index) + ".wav";
            // 发送文件
            String fileCode = fileStorageCellet.transfer(register.authToken, filename, waveData);
            fileLabels.add(fileCode);

            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (!this.checkFile(register, fileCode) && System.currentTimeMillis() - register.refresh < 30 * 1000) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            this.analysis(register, fileCode, index);
        }

        private boolean checkFile(Register register, String fileCode) {
            JSONObject payload = new JSONObject();
            payload.put("fileCode", fileCode);
            Packet packet = new Packet(FileStorageAction.FindFile.name, payload);
            ActionDialect packetDialect = packet.toDialect();
            packetDialect.addParam("token", register.authToken.getCode());

            ActionDialect responseDialect = performer.syncTransmit(FileStorageCellet.NAME, packetDialect);
            if (null == responseDialect) {
                Logger.d(this.getClass(), "#checkFile - No response");
                return false;
            }

            Packet responsePacket = new Packet(responseDialect);

            int stateCode = Packet.extractCode(responsePacket);
            if (stateCode != FileStorageStateCode.Ok.code) {
                Logger.d(this.getClass(), "#checkFile - Not find file");
                return false;
            }

            return true;
        }

        private void analysis(Register register, String fileCode, int index) {
            JSONObject payload = new JSONObject();
            payload.put("fileCode", fileCode);
            payload.put("streamName", register.streamName);
            payload.put("index", index);
            Packet packet = new Packet(AIGCAction.AnalyseAudioStream.name, payload);
            ActionDialect packetDialect = packet.toDialect();
            packetDialect.addParam("token", register.authToken.getCode());

            ActionDialect responseDialect = performer.syncTransmit(AIGCCellet.NAME, packetDialect,
                    3 * 60 * 1000);
            if (null == responseDialect) {
                Logger.w(this.getClass(), "#analysis - The response is null");
                return;
            }

            Packet responsePacket = new Packet(responseDialect);
            int stateCode = Packet.extractCode(responsePacket);
            if (stateCode != FileStorageStateCode.Ok.code) {
                Logger.w(this.getClass(), "#analysis - The state code is NOT ok: " + stateCode);
                return;
            }

            AudioStreamSink streamSink = new AudioStreamSink(Packet.extractDataPayload(responsePacket));
            JSONObject json = streamSink.toJSON();
            if (json.has("file")) {
                FileLabels.reviseFileLabel(json.getJSONObject("file"), register.authToken.getCode(),
                        performer.getExternalHttpEndpoint(),
                        performer.getExternalHttpsEndpoint());
            }
            register.track.write(StreamType.SpeechRecognition, json);
        }
    }

    protected class Register {

        protected final String streamName;

        protected final long timestamp;

        protected final AuthToken authToken;

        protected Track track;

        protected long refresh;

        public Register(String streamName, AuthToken authToken) {
            this.streamName = streamName;
            this.timestamp = System.currentTimeMillis();
            this.authToken = authToken;
            this.refresh = this.timestamp;
        }
    }
}
