/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.ByteUtils;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.util.AudioUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StreamArchive {

    public final static String Extension = "vsa";

    private final String streamName;

    private File file;

    private Header header;

    public StreamArchive(String workingPath, String streamName) {
        this.streamName = streamName;
        this.file = new File(workingPath, streamName + "." + Extension);

        if (this.file.exists()) {
            this.header = this.readHeader(this.file);
        }
        else {
            this.header = new Header(streamName, System.currentTimeMillis(), AudioUtils.SAMPLE_RATE,
                    AudioUtils.SAMPLE_SIZE_IN_BITS, AudioUtils.CHANNELS);
        }
    }

    public Header getHeader() {
        return this.header;
    }

    public long getTimestamp() {
        return this.header.timestamp;
    }

    public boolean exists() {
        return this.file.exists();
    }

    public File getFile() {
        return this.file;
    }

    /**
     * 计算以毫秒为单位的播放时长。
     *
     * @return
     */
    public long calculateDurationMillis() {
        if (0 == this.header.numChunks()) {
            return 0;
        }

        byte[] pcmData = this.loadPCM();
        if (null == pcmData) {
            Logger.w(this.getClass(), "#calculateDurationMillis - No PCM data: " + this.streamName);
            return 0;
        }

        double frames = ((double) pcmData.length) / (((double) this.header.sampleSizeInBits) / 8.0 * (double) this.header.channels);
        return Math.round(frames / ((double) this.header.sampleRate) * 1000.0);
    }

    public byte[] loadPCM() {
        if (this.header.numChunks() == 0) {
            Logger.w(this.getClass(), "#loadPCM - No data loaded: " + file.getAbsolutePath());
            return null;
        }

        RandomAccessFile raf = null;
        try {
            FlexibleByteBuffer buf = new FlexibleByteBuffer();
            raf = new RandomAccessFile(file.getAbsolutePath(), "r");
            raf.seek(this.header.lengthInBytes);
            byte[] bytes = new byte[4096];
            int len;
            while ((len = raf.read(bytes)) > 0) {
                buf.put(bytes, 0, len);
            }
            // flip
            buf.flip();

            byte[] result = new byte[buf.limit()];
            System.arraycopy(buf.array(), 0, result, 0, buf.limit());
            return result;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#loadPCM", e);
            return null;
        } finally {
            if (null != raf) {
                try {
                    raf.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }
    }

    /**
     * 转为 WAV 文件。
     *
     * @return
     */
    public File outputWavFile() {
        byte[] pcmData = this.loadPCM();
        if (null == pcmData) {
            return null;
        }

        byte[] wavData = AudioUtils.pcmToWav(pcmData);
        File output = new File(this.file.getParent(), this.streamName + ".wav");
        if (output.exists()) {
            output.delete();
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(output);
            fos.write(wavData);
            fos.flush();
            return output;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#outputWavFile", e);
            return null;
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }
    }

    /**
     * 删除文件。
     */
    public boolean delete() {
        if (this.file.exists()) {
            return file.delete();
        }
        return false;
    }

    public File save(int index, byte[] pcmData) {
        StreamChunk chunk = new StreamChunk(index, pcmData.length);
        chunk.pcmData = pcmData;

        if (this.file.exists()) {
            Logger.d(this.getClass(), "#save - File exists: " + this.file.getAbsolutePath());

            // 旧数据
            if (!this.readPCMToChunks(this.file, this.header)) {
                Logger.e(this.getClass(), "#save - PCM data error: " + this.file.getAbsolutePath());
                return null;
            }

            // 新数据
            this.header.addStream(chunk);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(this.file);
                // 文件头
                fos.write(this.header.toBytes());
                // PCM 数据
                for (StreamChunk sc : this.header.chunks) {
                    fos.write(sc.pcmData, 0, sc.length);
                }
                fos.flush();

                return this.file;
            } catch (Exception e) {
                Logger.e(this.getClass(), "#save", e);
                return null;
            } finally {
                if (null != fos) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        // Nothing
                    }
                }
            }
        }
        else {
            Logger.d(this.getClass(), "#save - New file: " + this.file.getAbsolutePath());

            this.header.addStream(chunk);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(this.file);
                // 文件头
                fos.write(this.header.toBytes());
                // PCM 数据
                for (StreamChunk sc : this.header.chunks) {
                    fos.write(sc.pcmData, 0, sc.length);
                }
                fos.flush();
                return this.file;
            } catch (Exception e) {
                Logger.e(this.getClass(), "#save", e);
                return null;
            } finally {
                if (null != fos) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        // Nothing
                    }
                }
            }
        }
    }

    private boolean readPCMToChunks(File file, Header header) {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file.getAbsolutePath(), "r");
            raf.seek(header.lengthInBytes);

            for (StreamChunk chunk : header.chunks) {
                byte[] bytes = new byte[chunk.length];
                raf.readFully(bytes);
                chunk.pcmData = bytes;
            }
            return true;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#readPCMToChunks", e);
            return false;
        } finally {
            if (null != raf) {
                try {
                    raf.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }
    }

    private Header readHeader(File file) {
        int sepCounts = 6;
        FileInputStream fis = null;
        FlexibleByteBuffer buf = new FlexibleByteBuffer();
        try {
            fis = new FileInputStream(file);
            byte b;
            int lastSepIndex = 0;
            int index = -1;
            while ((b = (byte) fis.read()) != -1) {
                ++index;
                buf.put(b);
                if (b == Header.SEP && sepCounts > 0) {
                    --sepCounts;
                    lastSepIndex = index;
                }
                else if (b == Header.EOH_1) {
                    // 下一字节
                    byte next;
                    if ((next = (byte) fis.read()) >= 0) {
                        ++index;
                        buf.put(next);
                        if (next == Header.EOH_2) {
                            break;
                        }
                        else if (next == Header.SEP && sepCounts > 0) {
                            --sepCounts;
                            lastSepIndex = index;
                        }
                    }
                }
            }

            buf.flip();

            byte[] data = buf.array();
            if (data[0] != Header.BOH) {
                Logger.d(this.getClass(), "#readHeader - NO VSA format file: " + file.getAbsolutePath());
                return null;
            }

            byte[] info = new byte[lastSepIndex - 1];
            System.arraycopy(data, 1, info, 0, info.length);

            byte[] tags = new byte[buf.limit() - lastSepIndex - 3];
            System.arraycopy(data, lastSepIndex + 1, tags, 0, tags.length);

            Header header = new Header(info, tags);
            return header;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#readHeader", e);
            return null;
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }
    }

    public class Header {

        protected final static byte BOH = 0x11;
        protected final static byte EOH_1 = 0x17;
        protected final static byte EOH_2 = 0x03;
        protected final static byte SEP = '\n';
        protected final static byte EMP = '~';

        public final String version;
        public final String streamName;
        public final long timestamp;

        public final int sampleRate;
        public final int sampleSizeInBits;
        public final int channels;

        protected List<StreamChunk> chunks;

        protected int lengthInBytes;

        protected Header(String streamName, long timestamp, int sampleRate, int sampleSizeInBits, int channels) {
            this.version = "1.0";
            this.streamName = streamName;
            this.timestamp = timestamp;
            this.sampleRate = sampleRate;
            this.sampleSizeInBits = sampleSizeInBits;
            this.channels = channels;
            this.chunks = new ArrayList<>();

            this.lengthInBytes = this.version.length() + streamName.length() + Long.toString(timestamp).length() +
                    Integer.toString(sampleRate).length() + Integer.toString(sampleSizeInBits).length() +
                    Integer.toString(channels).length() + 10;
        }

        protected Header(byte[] info, byte[] tags) throws IOException,
                NumberFormatException, IndexOutOfBoundsException, NullPointerException {
            String infoString = new String(info, StandardCharsets.UTF_8);
            String[] infos = infoString.split(new String(new byte[]{ SEP }, StandardCharsets.UTF_8));
            if (infos.length != 6) {
                throw new IOException("Header format error");
            }

            this.version = infos[0];
            this.streamName = infos[1];
            this.timestamp = Long.parseLong(infos[2]);
            this.sampleRate = Integer.parseInt(infos[3]);
            this.sampleSizeInBits = Integer.parseInt(infos[4]);
            this.channels = Integer.parseInt(infos[5]);

//            System.out.println("version: " + version);
//            System.out.println("streamName: " + streamName);
//            System.out.println("timestamp: " + timestamp);
//            System.out.println("sampleRate: " + sampleRate);
//            System.out.println("sampleSizeInBits: " + sampleSizeInBits);
//            System.out.println("channels: " + channels);

            this.chunks = new ArrayList<>();
            if (tags.length > 0 && (tags.length % 8) == 0) {
                int size = tags.length / 4;
                for (int i = 0; i < size; ++i) {
                    byte[] bytesIndex = new byte[4];
                    byte[] bytesLength = new byte[4];
                    // 定位到 i
                    System.arraycopy(tags, i * 4, bytesIndex, 0, 4);
                    // 定位到 i + 1
                    i += 1;
                    System.arraycopy(tags, i * 4, bytesLength, 0, 4);

                    int index = ByteUtils.toInt(bytesIndex);
                    int length = ByteUtils.toInt(bytesLength);
                    StreamChunk chunk = new StreamChunk(index, length);
                    this.chunks.add(chunk);
                }
            }

            // 1 byte BOH + 1 byte SEP + 2 bytes EOH = 4 bytes
            this.lengthInBytes = info.length + tags.length + 4;
        }

        protected byte[] toBytes() {
            FlexibleByteBuffer buf = new FlexibleByteBuffer();
            buf.put(BOH);
            buf.put(this.version.getBytes(StandardCharsets.UTF_8));
            buf.put(SEP);
            buf.put(this.streamName.getBytes(StandardCharsets.UTF_8));
            buf.put(SEP);
            buf.put(Long.toString(this.timestamp).getBytes(StandardCharsets.UTF_8));
            buf.put(SEP);
            buf.put(Integer.toString(this.sampleRate).getBytes(StandardCharsets.UTF_8));
            buf.put(SEP);
            buf.put(Integer.toString(this.sampleSizeInBits).getBytes(StandardCharsets.UTF_8));
            buf.put(SEP);
            buf.put(Integer.toString(this.channels).getBytes(StandardCharsets.UTF_8));
            buf.put(SEP);
            if (this.chunks.isEmpty()) {
                buf.put(EMP);
            }
            else {
                for (StreamChunk chunk : this.chunks) {
                    buf.put(chunk.toBytes());
                }
            }
            buf.put(EOH_1);
            buf.put(EOH_2);

            buf.flip();

            byte[] result = new byte[buf.limit()];
            System.arraycopy(buf.array(), 0, result, 0, buf.limit());

            // 更新长度数据
            this.lengthInBytes = buf.limit();

            return result;
        }

        public int numChunks() {
            return this.chunks.size();
        }

        public StreamChunk getStreamChunk(int index) {
            if (this.chunks.isEmpty()) {
                return null;
            }

            return this.chunks.get(index);
        }

        protected void addStream(StreamChunk chunk) {
            for (StreamChunk current : this.chunks) {
                if (current.index == chunk.index) {
                    this.chunks.remove(current);
                    break;
                }
            }

            this.chunks.add(chunk);

            this.chunks.sort(new Comparator<StreamChunk>() {
                @Override
                public int compare(StreamChunk st1, StreamChunk st2) {
                    return st1.index - st2.index;
                }
            });
        }
    }

    public class StreamChunk {

        public final int index;
        public final int length;

        protected byte[] pcmData;

        protected StreamChunk(int index, int length) {
            this.index = index;
            this.length = length;
        }

        protected byte[] toBytes() {
            byte[] data = new byte[8];
            System.arraycopy(ByteUtils.toBytes(this.index), 0, data, 0, 4);
            System.arraycopy(ByteUtils.toBytes(this.length), 0, data, 4, 4);
            return data;
        }
    }
}
