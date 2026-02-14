package cube.service.test;

import cube.common.entity.VoiceStreamSink;
import cube.service.aigc.scene.VoiceStreamArchive;
import cube.util.AudioUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class TestVoiceStreamArchive {

    public static void main(String[] args) {
//        testWriteFile();
//        testCoverData();
//        testAppendData();

        testReadFile();
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

        VoiceStreamSink sink = new VoiceStreamSink(streamName, 0);
        sink.setTimestamp(System.currentTimeMillis());
        byte[] pcmData = new byte[] { 49, 50, 51, 52 };

        VoiceStreamArchive archive = new VoiceStreamArchive(path, streamName, AudioUtils.SAMPLE_RATE,
                AudioUtils.SAMPLE_SIZE_IN_BITS, AudioUtils.CHANNELS);
        boolean success = archive.save(sink, pcmData);
        File output = archive.archive();
        System.out.println("Write to file: " + output.getAbsolutePath());
    }

    protected static void testCoverData() {
        String path = "storage/test/";
        String streamName = "STREAM_NAME";

        VoiceStreamSink sink = new VoiceStreamSink(streamName, 0);
        sink.setTimestamp(System.currentTimeMillis());
        byte[] pcmData = new byte[] { 52, 51, 50, 49 };

        VoiceStreamArchive archive = new VoiceStreamArchive(path, streamName, AudioUtils.SAMPLE_RATE,
                AudioUtils.SAMPLE_SIZE_IN_BITS, AudioUtils.CHANNELS);
        boolean success = archive.save(sink, pcmData);
        File output = archive.archive();
        System.out.println("Cover to file: " + output.getAbsolutePath());
    }

    protected static void testAppendData() {
        String path = "storage/test/";
        String streamName = "STREAM_NAME";

        VoiceStreamArchive archive = new VoiceStreamArchive(path, streamName, AudioUtils.SAMPLE_RATE,
                AudioUtils.SAMPLE_SIZE_IN_BITS, AudioUtils.CHANNELS);

        VoiceStreamSink sink = new VoiceStreamSink(streamName, 1);
        sink.setTimestamp(System.currentTimeMillis());
        byte[] pcmData = new byte[] { 53, 54, 55, 56 };
        archive.save(sink, pcmData);

        sink = new VoiceStreamSink(streamName, 2);
        sink.setTimestamp(System.currentTimeMillis());
        pcmData = new byte[] { 57, 58, 59, 60 };
        archive.save(sink, pcmData);

        File output = archive.archive();
        System.out.println("Cover to file: " + output.getAbsolutePath());
    }


}
