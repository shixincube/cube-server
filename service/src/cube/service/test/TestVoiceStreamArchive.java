package cube.service.test;

import cube.service.aigc.scene.VoiceStreamArchive;
import cube.util.AudioUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class TestVoiceStreamArchive {

    public static void main(String[] args) {
//        testWriteFile();
//        testCoverData();

//        testAppendData();

//        testReadFile();

        testData();
    }

    protected static void testReadFile() {
        String path = "storage/test/";
        String streamName = "STREAM_NAME";

        VoiceStreamArchive archive = new VoiceStreamArchive(new File(path, streamName + "." + VoiceStreamArchive.Extension));
        if (archive.load()) {
            VoiceStreamArchive.Header header = archive.getHeader();
            System.out.println("version: " + header.version);
            System.out.println("streamName: " + header.streamName);
            System.out.println("timestamp: " + header.timestamp);
            System.out.println("sampleRate: " + header.sampleRate);
            System.out.println("sampleSizeInBits: " + header.sampleSizeInBits);
            System.out.println("channels: " + header.channels);
            System.out.println("num of streams: " + header.numStreams());

            byte[] pcm = archive.readPCM();
            System.out.println("PCM length: " + pcm.length);
            System.out.println("PCM data: " + new String(pcm, StandardCharsets.UTF_8));
        }
        else {
            System.out.println("Load failed");
        }
    }

    protected static void testWriteFile() {
        String path = "storage/test/";
        String streamName = "STREAM_NAME";

        byte[] pcmData = new byte[] { 49, 50, 51, 52 };

        VoiceStreamArchive archive = new VoiceStreamArchive(path, streamName, AudioUtils.SAMPLE_RATE,
                AudioUtils.SAMPLE_SIZE_IN_BITS, AudioUtils.CHANNELS);
        boolean success = archive.save(0, pcmData, System.currentTimeMillis());
        File output = archive.archive();
        System.out.println("Write to file: " + output.getAbsolutePath());
    }

    protected static void testCoverData() {
        String path = "storage/test/";
        String streamName = "STREAM_NAME";

        byte[] pcmData = new byte[] { 52, 51, 50, 49 };

        VoiceStreamArchive archive = new VoiceStreamArchive(path, streamName, AudioUtils.SAMPLE_RATE,
                AudioUtils.SAMPLE_SIZE_IN_BITS, AudioUtils.CHANNELS);
        boolean success = archive.save(0, pcmData, System.currentTimeMillis());
        File output = archive.archive();
        System.out.println("Cover to file: " + output.getAbsolutePath());

        byte[] pcm = archive.readPCM();
        System.out.println("PCM: " + new String(pcm, StandardCharsets.UTF_8));
    }

    protected static void testAppendData() {
        String path = "storage/test/";
        String streamName = "STREAM_NAME";

        VoiceStreamArchive archive = new VoiceStreamArchive(path, streamName, AudioUtils.SAMPLE_RATE,
                AudioUtils.SAMPLE_SIZE_IN_BITS, AudioUtils.CHANNELS);

        byte[] pcmData = new byte[] { 53, 54, 55, 56 };
        archive.save(1, pcmData, System.currentTimeMillis());

        pcmData = new byte[] { 57, 58, 59, 60 };
        archive.save(2, pcmData, System.currentTimeMillis());

        File output = archive.archive();
        System.out.println("Append to file: " + output.getAbsolutePath());
    }

    protected static void testData() {
        String path = "storage/test/";
        String streamName = "STREAM_NAME";

        VoiceStreamArchive.Header header;

        VoiceStreamArchive archive = new VoiceStreamArchive(path, streamName, AudioUtils.SAMPLE_RATE,
                AudioUtils.SAMPLE_SIZE_IN_BITS, AudioUtils.CHANNELS);
        byte[] pcmData = new byte[] { 49, 50, 51, 52 };
        archive.save(2, pcmData, System.currentTimeMillis());
        File file = archive.archive();
        header = archive.getHeader();
        System.out.println("1: " + header.numStreams() + " - file: " + file.length());

        archive = new VoiceStreamArchive(path, streamName, AudioUtils.SAMPLE_RATE,
                AudioUtils.SAMPLE_SIZE_IN_BITS, AudioUtils.CHANNELS);
        pcmData = new byte[] { 53, 54, 55, 56 };
        archive.save(1, pcmData, System.currentTimeMillis());
        file = archive.archive();
        header = archive.getHeader();
        System.out.println("2: " + header.numStreams() + " - file: " + file.length());
    }
}
