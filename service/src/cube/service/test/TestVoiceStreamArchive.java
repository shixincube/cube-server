package cube.service.test;

import cell.util.Utils;
import cube.service.aigc.scene.StreamArchive;
import cube.util.AudioUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class TestVoiceStreamArchive {

    public static void main(String[] args) {
//        testWriteFile();

        testReadFile();

//        testAppendData();

//        testData();
    }

    protected static void testReadFile() {
        String path = "storage/test/";
//        String streamName = "JilpjkRH";
        String streamName = "kgHNFcRP";

        StreamArchive archive = new StreamArchive(path, streamName);
        if (archive.exists()) {
            StreamArchive.Header header = archive.getHeader();
            System.out.println("version: " + header.version);
            System.out.println("streamName: " + header.streamName);
            System.out.println("timestamp: " + header.timestamp);
            System.out.println("sampleRate: " + header.sampleRate);
            System.out.println("sampleSizeInBits: " + header.sampleSizeInBits);
            System.out.println("channels: " + header.channels);
            System.out.println("num of chunks: " + header.numChunks());

            for (int i = 0; i < header.numChunks(); ++i) {
                StreamArchive.StreamChunk chunk = header.getStreamChunk(i);
                System.out.println("Chunk " + i + " : " + chunk.index + " - " + chunk.length);
            }

            byte[] pcm = archive.loadPCM();
            System.out.println("PCM length: " + pcm.length);
//            System.out.println("PCM data: " + new String(pcm, StandardCharsets.UTF_8));
        }
        else {
            System.out.println("Load failed");
        }
    }

    protected static void testWriteFile() {
        String path = "storage/test/";
        String streamName = "AilpjkRH";

        byte[] pcmData = new byte[] { 49, 50, 51, 52 };

        StreamArchive archive = new StreamArchive(path, streamName);
        File output = archive.save(0, pcmData);
        System.out.println("Write to file: " + output.length());
    }

    protected static void testAppendData() {
        String path = "storage/test/";
        String streamName = "AXcHwJSa";

        StreamArchive archive = new StreamArchive(path, streamName);

        byte[] pcmData = new byte[] { 53, 54, 55, 56 };
        File output = archive.save(1, pcmData);

//        pcmData = new byte[] { 57, 58, 59, 60 };
//        archive.save(2, pcmData, System.currentTimeMillis());
        System.out.println("Append to file: " + output.getAbsolutePath());
    }

    protected static void testData() {
        String path = "storage/test/";
        String streamName = "JilpjkRH";

        StreamArchive.Header header;

        // 第一段
        StreamArchive archive = new StreamArchive(path, streamName);
        byte[] pcmData = new byte[1024];
        for (int i = 0; i < pcmData.length; ++i) {
            pcmData[i] = (byte) Utils.randomInt(0, 127);
        }
        File file = archive.save(0, pcmData);
        header = archive.getHeader();
        System.out.println("0: " + header.numChunks() + " - file: " + file.length());

        // 第二段
        archive = new StreamArchive(path, streamName);
        pcmData = new byte[2048];
        for (int i = 0; i < pcmData.length; ++i) {
            pcmData[i] = (byte) Utils.randomInt(0, 127);
        }
        file = archive.save(1, pcmData);
        header = archive.getHeader();
        System.out.println("1: " + header.numChunks() + " - file: " + file.length());

        // 第三段
        archive = new StreamArchive(path, streamName);
        pcmData = new byte[4096];
        for (int i = 0; i < pcmData.length; ++i) {
            pcmData[i] = (byte) Utils.randomInt(0, 127);
        }
        file = archive.save(2, pcmData);
        header = archive.getHeader();
        System.out.println("2: " + header.numChunks() + " - file: " + file.length());
    }
}
