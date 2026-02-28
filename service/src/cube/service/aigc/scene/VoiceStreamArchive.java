/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cell.util.ByteUtils;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.common.entity.VoiceStreamSink;
import cube.util.AudioUtils;
import cube.util.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 语音流存档管理器。
 */
public class VoiceStreamArchive {

    public final static String Extension = "vsa";

    private final String workingPath;

    private final String streamName;

    private Header header;

    public VoiceStreamArchive(File file) {
        this.workingPath = file.getParent();
        this.streamName = FileUtils.extractFileName(file.getName());
    }

    public VoiceStreamArchive(String workingPath, String streamName, int sampleRate,
                              int sampleSizeInBits, int channels) {
        this.workingPath = workingPath;
        this.streamName = streamName;
        this.header = new Header();
        this.header.streamName = streamName;
        this.header.timestamp = System.currentTimeMillis();
        this.header.sampleRate = sampleRate;
        this.header.sampleSizeInBits = sampleSizeInBits;
        this.header.channels = channels;
    }

    public Header getHeader() {
        return this.header;
    }

    public long getTimestamp() {
        return this.header.timestamp;
    }

    public boolean exists() {
        File file = new File(this.workingPath, this.streamName + "." + Extension);
        return file.exists();
    }

    public boolean load() {
        File file = new File(this.workingPath, this.streamName + "." + Extension);
        Header header = this.readHeader(file);
        if (null == header) {
            Logger.w(this.getClass(), "#load - Reads header failed: " + file.getAbsolutePath());
            return false;
        }

        this.header = header;

        return true;
    }

    public byte[] readPCM() {
        File file = new File(this.workingPath, this.streamName + "." + Extension);

        if (null == this.header) {
            Logger.w(this.getClass(), "#readPCM - No data loaded: " + file.getAbsolutePath());
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
            Logger.e(this.getClass(), "#readPCM", e);
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

    public boolean save(VoiceStreamSink sink, byte[] pcmData) {
        if (null == this.header) {
            this.header = new Header();
        }

        if (0 == this.header.timestamp) {
            this.header.timestamp = sink.getTimestamp();
        }

        StreamTag tag = new StreamTag(sink.getIndex(), pcmData.length);
        tag.pcmData = pcmData;
        this.header.addStream(tag);

        return true;
    }

    /**
     * 将数据归档到文件。
     *
     * @return
     */
    public File archive() {
        File output = new File(this.workingPath, this.streamName + "." + Extension);
        if (output.exists()) {
            // 覆盖写入
            if (null == this.header || null == this.header.streamTags) {
                Logger.e(this.getClass(), "#archive - No data: " + output.getAbsolutePath());
                return null;
            }

            // 读取旧数据
            Header header = this.readHeader(output);
            if (null == header) {
                Logger.e(this.getClass(), "#archive - No header data: " + output.getAbsolutePath());
                return null;
            }

            // 旧数据
            if (!this.readPCMIntoTag(output, header)) {
                Logger.e(this.getClass(), "#archive - PCM data error: " + output.getAbsolutePath());
                return null;
            }

            // 新数据覆盖旧数据
            for (StreamTag tag : this.header.streamTags) {
                header.addStream(tag);
            }

            // 覆盖旧数据
            header.sampleRate = this.header.sampleRate;
            header.sampleSizeInBits = this.header.sampleSizeInBits;
            header.channels = this.header.channels;

            FlexibleByteBuffer buf = new FlexibleByteBuffer();
            for (StreamTag tag : header.streamTags) {
                buf.put(tag.pcmData);
            }
            buf.flip();

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(output);
                // 文件头
                fos.write(header.toBytes());
                fos.write(buf.array(), 0, buf.limit());
                fos.flush();

                // 更新新数据
                this.header = header;

                return output;
            } catch (Exception e) {
                Logger.e(this.getClass(), "#archive", e);
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
            // 新文件
            if (null == this.header || null == this.header.streamTags) {
                Logger.e(this.getClass(), "#archive - No data: " + output.getAbsolutePath());
                return null;
            }

            FlexibleByteBuffer buf = new FlexibleByteBuffer();
            for (StreamTag tag : this.header.streamTags) {
                buf.put(tag.pcmData);
            }
            buf.flip();

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(output);
                // 文件头
                fos.write(this.header.toBytes());
                fos.write(buf.array(), 0, buf.limit());
                fos.flush();
                return output;
            } catch (Exception e) {
                Logger.e(this.getClass(), "#archive", e);
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

    /**
     * 计算以毫秒为单位的播放时长。
     *
     * @return
     */
    public long calculateDurationMillis() {
        byte[] pcmData = this.readPCM();
        if (null == pcmData) {
            return 0;
        }

        double frames = ((double) pcmData.length) / (((double) this.header.sampleSizeInBits) / 8.0 * (double) this.header.channels);
        return Math.round(frames / ((double) this.header.sampleRate * 1000.0));
    }

    /**
     * 转为 WAV 文件。
     *
     * @return
     */
    public File convertToWavFile() {
        if (null == this.header) {
            this.load();
        }

        byte[] pcmData = this.readPCM();
        if (null == pcmData) {
            return null;
        }

        byte[] wavData = AudioUtils.pcmToWav(pcmData);
        File output = new File(this.workingPath, this.streamName + ".wav");
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
            Logger.e(this.getClass(), "#convertToWavFile", e);
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
    public void delete() {
        File file = new File(this.workingPath, this.streamName + "." + Extension);
        if (file.exists()) {
            file.delete();
        }
    }

    private Header readHeader(File file) {
        FileInputStream fis = null;
        FlexibleByteBuffer buf = new FlexibleByteBuffer();
        try {
            fis = new FileInputStream(file);
            byte b;
            int lastSepIndex = 0;
            int index = 0;
            while ((b = (byte) fis.read()) >= 0) {
                buf.put(b);
                if (b == Header.SEP) {
                    lastSepIndex = index;
                }
                else if (b == Header.EOH) {
                    break;
                }
                ++index;
            }

            buf.flip();

            byte[] data = buf.array();
            if (data[0] != Header.BOH) {
                Logger.d(this.getClass(), "#readHeader - NO VSA format file: " + file.getAbsolutePath());
                return null;
            }

            byte[] info = new byte[lastSepIndex - 1];
            System.arraycopy(data, 1, info, 0, info.length);

            byte[] tags = new byte[buf.limit() - lastSepIndex - 2];
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

    private boolean readPCMIntoTag(File file, Header header) {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file.getAbsolutePath(), "r");
            raf.seek(header.lengthInBytes);

            for (StreamTag tag : header.streamTags) {
                byte[] bytes = new byte[tag.length];
                raf.readFully(bytes);
                tag.pcmData = bytes;
            }
            return true;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#readPCMIntoTag", e);
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

    public class Header {

        protected final static byte BOH = 0x11;
        protected final static byte EOH = 0x03;
        protected final static byte SEP = '\n';
        protected final static byte EMP = '~';

        public String version = "1.0";
        public String streamName;
        public long timestamp;

        public int sampleRate;
        public int sampleSizeInBits;
        public int channels;

        protected List<StreamTag> streamTags;

        protected int lengthInBytes;

        protected Header() {
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

            if (tags.length > 1) {
                this.streamTags = new ArrayList<>();
                int size = tags.length / 4 / 2;
                for (int i = 0; i < size; ++i) {
                    byte[] bytesIndex = new byte[4];
                    byte[] bytesLength = new byte[4];
                    System.arraycopy(tags, i * 4, bytesIndex, 0, 4);
                    System.arraycopy(tags, (i + 1) * 4, bytesLength, 0, 4);

                    int index = ByteUtils.toInt(bytesIndex);
                    int length = ByteUtils.toInt(bytesLength);
                    StreamTag tag = new StreamTag(index, length);
                    this.streamTags.add(tag);
                }
            }

            this.lengthInBytes = info.length + tags.length + 3;
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
            if (null == this.streamTags) {
                buf.put(EMP);
            }
            else {
                for (StreamTag tag : this.streamTags) {
                    buf.put(tag.toBytes());
                }
            }
            buf.put(EOH);

            buf.flip();

            byte[] result = new byte[buf.limit()];
            System.arraycopy(buf.array(), 0, result, 0, buf.limit());

            // 更新长度数据
            this.lengthInBytes = buf.limit();

            return result;
        }

        public int numStreams() {
            return (null == this.streamTags) ? 0 : this.streamTags.size();
        }

        public boolean hasStreamTag(int index) {
            if (null == this.streamTags) {
                return false;
            }

            for (StreamTag tag : this.streamTags) {
                if (tag.index == index) {
                    return true;
                }
            }

            return false;
        }

        protected void addStream(StreamTag streamTag) {
            if (null == this.streamTags) {
                this.streamTags = new ArrayList<>();
            }

            for (StreamTag tag : this.streamTags) {
                if (tag.index == streamTag.index) {
                    this.streamTags.remove(tag);
                    break;
                }
            }

            this.streamTags.add(streamTag);

            this.streamTags.sort(new Comparator<StreamTag>() {
                @Override
                public int compare(StreamTag st1, StreamTag st2) {
                    return st1.index - st2.index;
                }
            });
        }
    }

    protected class StreamTag {

        protected int index;
        protected int length;

        protected byte[] pcmData;

        public StreamTag(int index, int length) {
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
