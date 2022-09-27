/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IP 查询。
 */
public final class IPSeeker {

    private static IPSeeker instance = null;

    private static final int INDEX_RECORD_LENGTH = 7;
    private static final byte REDIRECT_MODE_1 = 0x01;
    private static final byte REDIRECT_MODE_2 = 0x02;
    private static final byte STRING_END = '\0';

    private final byte[] data;
    private final long indexHead;
    private final long indexTail;
    private final String databaseVersion;

    private IPSeeker() throws IOException {
        final InputStream in = new FileInputStream("assets/ip-db.dat");
        final ByteArrayOutputStream out = new ByteArrayOutputStream(11 * 1024 * 1024); // 11MB
        final byte[] buffer = new byte[4096];
        try {
            while (true) {
                final int r = in.read(buffer);
                if (r == -1) {
                    break;
                }
                out.write(buffer, 0, r);
            }

            data = out.toByteArray();
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }

            try {
                out.close();
            } catch (IOException e) {
            }
        }

        indexHead = readLong32(0);
        indexTail = readLong32(4);
        databaseVersion = parseDatabaseVersion();
    }

    public IPSeeker(final byte[] data) {
        this.data = data;
        indexHead = readLong32(0);
        indexTail = readLong32(4);
        databaseVersion = parseDatabaseVersion();
    }

    public IPSeeker(final Path file) throws IOException {
        this(Files.readAllBytes(file));
    }

    public synchronized static IPSeeker getInstance() {
        if (null == IPSeeker.instance) {
            try {
                IPSeeker.instance = new IPSeeker();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return IPSeeker.instance;
    }

    public IPEntry findIP(final String ip) {
        final long ipNum = toNumericIP(ip);
        final QIndex idx = searchIndex(ipNum);
        if (idx == null) {
            return new IPEntry(ip);
        }
        return readIP(ip, idx);
    }

    private long getMiddleOffset(final long begin, final long end) {
        long records = (end - begin) / INDEX_RECORD_LENGTH;
        records >>= 1;
        if (records == 0) {
            records = 1;
        }
        return begin + (records * INDEX_RECORD_LENGTH);
    }

    private QIndex readIndex(final int offset) {
        final long min = readLong32(offset);
        final int record = readInt24(offset + 4);
        final long max = readLong32(record);
        return new QIndex(min, max, record);
    }

    private int readInt24(final int offset) {
        int v = data[offset] & 0xFF;
        v |= ((data[offset + 1] << 8) & 0xFF00);
        v |= ((data[offset + 2] << 16) & 0xFF0000);
        return v;
    }

    private IPEntry readIP(final String ip, final QIndex idx) {
        final int pos = idx.recordOffset + 4; // skip ip
        final byte mode = data[pos];
        final IPEntry z = new IPEntry(ip);
        if (mode == REDIRECT_MODE_1) {
            final int offset = readInt24(pos + 1);
            if (data[offset] == REDIRECT_MODE_2) {
                readMode2(z, offset);
            } else {
                final QString mainInfo = readString(offset);
                final String subInfo = readSubInfo(offset + mainInfo.length);
                z.setMainInfo(mainInfo.string);
                z.setSubInfo(subInfo);
            }
        } else if (mode == REDIRECT_MODE_2) {
            readMode2(z, pos);
        } else {
            final QString mainInfo = readString(pos);
            final String subInfo = readSubInfo(pos + mainInfo.length);
            z.setMainInfo(mainInfo.string);
            z.setSubInfo(subInfo);
        }
        return z;
    }

    private long readLong32(final int offset) {
        long v = data[offset] & 0xFFL;
        v |= (data[offset + 1] << 8L) & 0xFF00L;
        v |= ((data[offset + 2] << 16L) & 0xFF0000L);
        v |= ((data[offset + 3] << 24L) & 0xFF000000L);
        return v;
    }

    private void readMode2(final IPEntry z, final int offset) {
        final int mainInfoOffset = readInt24(offset + 1);
        final String main = readString(mainInfoOffset).string;
        final String sub = readSubInfo(offset + 4);
        z.setMainInfo(main);
        z.setSubInfo(sub);
    }

    private QString readString(final int offset) {
        int i = 0;
        final byte[] buf = new byte[128];
        for (; ; i++) {
            final byte b = data[offset + i];
            if (STRING_END == b) {
                break;
            }
            buf[i] = b;
        }
        try {
            return new QString(new String(buf, 0, i, "GB18030"), i + 1);
        } catch (final UnsupportedEncodingException e) {
            return new QString("", 0);
        }
    }

    private String readSubInfo(final int offset) {
        final byte b = data[offset];
        if ((b == REDIRECT_MODE_1) || (b == REDIRECT_MODE_2)) {
            final int areaOffset = readInt24(offset + 1);
            if (areaOffset == 0) {
                return "";
            } else {
                return readString(areaOffset).string;
            }
        } else {
            return readString(offset).string;
        }
    }

    private QIndex searchIndex(final long ip) {
        long head = indexHead;
        long tail = indexTail;
        while (tail > head) {
            final long cur = getMiddleOffset(head, tail);
            final QIndex idx = readIndex((int) cur);
            if ((ip >= idx.minIP) && (ip <= idx.maxIP)) {
                return idx;
            }
            if ((cur == head) || (cur == tail)) {
                return idx;
            }
            if (ip < idx.minIP) {
                tail = cur;
            } else if (ip > idx.maxIP) {
                head = cur;
            } else {
                return idx;
            }
        }
        return null;
    }

    private long toNumericIP(final String s) {
        final String[] parts = s.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("ip=" + s);
        }
        long n = Long.parseLong(parts[0]) << 24L;
        n += Long.parseLong(parts[1]) << 16L;
        n += Long.parseLong(parts[2]) << 8L;
        n += Long.parseLong(parts[3]);
        return n;
    }

    /**
     * database version in pattern x.x.x
     */
    public String getDatabaseVersion() {
        return databaseVersion;
    }

    private String parseDatabaseVersion() {
        Pattern dbVerPattern = Pattern.compile("(\\d+)年(\\d+)月(\\d+)日.*");
        IPEntry ipz = findIP("255.255.255.255");
        Matcher m = dbVerPattern.matcher(ipz.getSubInfo());
        if (!m.matches() || m.groupCount() != 3) {
            return "0.0.0";
        }
        return String.format("%s.%s.%s", m.group(1), m.group(2), m.group(3));
    }

    private static class QIndex {
        public final long minIP;
        public final long maxIP;
        public final int recordOffset;

        public QIndex(final long minIP, final long maxIP, final int recordOffset) {
            this.minIP = minIP;
            this.maxIP = maxIP;
            this.recordOffset = recordOffset;
        }
    }

    private static class QString {
        public final String string;
        /**
         * length including the \0 end byte
         */
        public final int length;

        public QString(final String string, final int length) {
            this.string = string;
            this.length = length;
        }
    }
}
