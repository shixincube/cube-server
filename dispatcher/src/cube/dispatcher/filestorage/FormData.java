/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.dispatcher.filestorage;

import cell.util.Utils;
import cell.util.collection.FlexibleByteBuffer;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Form 数据格式描述和解析。
 */
public class FormData {

    private static String sContentType = "Content-Type".toLowerCase();
    private static String sOctetStream = "octet-stream".toLowerCase();
    private static String sJson = "json".toLowerCase();

    private FlexibleByteBuffer buf;

    private HashMap<String, String> multipart;

    private String fileName;

    private byte[] stream;

    /**
     *
     * @param content
     */
    public FormData(byte[] content) {
        this.buf = new FlexibleByteBuffer(1024);
        this.multipart = this.parse(content);
        this.buf = null;
    }

    public String getFileName() {
        return this.fileName;
    }

    public byte[] getFileStream() {
        return this.stream;
    }

    public String getValue(String name) {
        return this.multipart.get(name);
    }

    private HashMap<String, String> parse(byte[] content) {
        int cursor = 0;
        // 查找占位符
        String boundary = null;

        for (int i = 0; i < content.length; ++i) {
            byte b1 = content[cursor++];
            byte b2 = content[cursor];
            if (b1 == '\r' && b2 == '\n') {
                buf.flip();
                boundary = new String(buf.array(), 0, buf.limit());
                cursor++;
                break;
            }

            buf.put(b1);
        }

        HashMap<String, String> data = new HashMap<>();

        String fileDisposition = null;

        String disposition = null;
        String cdata = null;

        long t = System.currentTimeMillis();
        ActionType action = ActionType.TO_FIND_DISPOSITION;

        while (cursor < content.length) {
            byte b = content[cursor++];

            switch (action) {
                case TO_FIND_DISPOSITION:
                    if (b == '\r') {
                        ++cursor;

                        buf.flip();
                        disposition = new String(buf.array(), 0, buf.limit(), Charset.forName("UTF-8"));
                        buf.clear();

                        action = ActionType.TO_FIND_CONTENT;
                        break;
                    }

                    buf.put(b);
                    break;

                case TO_FIND_CONTENT:
                    if (b == '\r') {
                        ++cursor;

                        if (0 == buf.position()) {
                            break;
                        }

                        buf.flip();
                        cdata = new String(buf.array(), 0, buf.limit(), Charset.forName("UTF-8"));
                        String lowerData = cdata.toLowerCase();

                        if (lowerData.indexOf(sContentType) >= 0) {
                            if (lowerData.indexOf(sOctetStream) >= 0) {
                                // 文件流
                                // 跳过空行
                                cursor += 2;
                                fileDisposition = disposition;
                                action = ActionType.TO_READ_STREAM;
                            }
                            else {
                                // TODO 处理 JSON
                                action = ActionType.TO_FIND_BOUNDARY;
                            }
                        }
                        else {
                            data.put(disposition, cdata);
                            action = ActionType.TO_FIND_BOUNDARY;
                        }

                        buf.clear();

                        break;
                    }

                    buf.put(b);
                    break;

                case TO_FIND_BOUNDARY:
                    if (b == '\r') {
                        ++cursor;

                        buf.flip();
                        String line = new String(buf.array(), 0, buf.limit(), Charset.forName("UTF-8"));
                        if (line.equals(boundary)) {
                            action = ActionType.TO_FIND_DISPOSITION;
                        }
                        else if (line.equals(boundary + "--")) {
                            action = ActionType.TO_END;
                        }

                        buf.clear();
                        break;
                    }

                    buf.put(b);
                    break;

                case TO_READ_STREAM:
                    int taillen = boundary.length() + 2;
                    if (content.length - cursor > taillen) {
                        stream = new byte[content.length - cursor - taillen - 1];
                        System.arraycopy(content, cursor - 1, stream, 0, stream.length);
                        cursor = content.length;
                        break;
                    }
                    /*if (b == '\r') {
                        // 判断是否结束
                        byte[] suspected = new byte[boundary.length()];
                        // cursor + 1 避开 \n
                        System.arraycopy(content, cursor + 1, suspected, 0, suspected.length);

                        String endFlag = new String(suspected);
                        if (boundary.equals(endFlag)) {
                            // 数据结束
                            buf.flip();
                            stream = new byte[buf.limit()];
                            System.arraycopy(buf.array(), 0, stream, 0, stream.length);
                            buf.clear();

                            // 后移一位，剔除 \n
                            ++cursor;
                            action = ActionType.TO_FIND_BOUNDARY;
                            break;
                        }
                    }*/
                    break;

                case TO_END:
                    buf.clear();
                    break;
                default:
                    break;
            }
        }

        if (null != fileDisposition) {
            int index = fileDisposition.indexOf("filename=");
            this.fileName = fileDisposition.substring(index + 10, fileDisposition.length() - 1);
        }

        HashMap<String, String> result = new HashMap<>();
        Iterator<Map.Entry<String, String>> iter = data.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> e = iter.next();
            String disp = e.getKey();
            int index = disp.indexOf("name=");
            if (index > 0) {
                String name = disp.substring(index + 6, disp.length() - 1);
                result.put(name, e.getValue().toString());
            }
        }

        return result;
    }

    enum ActionType {
        TO_FIND_DISPOSITION,
        TO_FIND_CONTENT,
        TO_FIND_BOUNDARY,
        TO_READ_STREAM,
        TO_END
    }

    public static void main(String[] args) {
        String fc = Utils.randomString(512 * 1024);

        String data = "------WebKitFormBoundaryAiKTL587TYHtWO2p\r\n" +
                "Content-Disposition: form-data; name=\"starting\"\r\n" +
                "\r\n" +
                "0\r\n" +
                "------WebKitFormBoundaryAiKTL587TYHtWO2p\r\n" +
                "Content-Disposition: form-data; name=\"ending\"\r\n" +
                "\r\n" +
                "1048576\r\n" +
                "------WebKitFormBoundaryAiKTL587TYHtWO2p\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"3周年.png\"\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "\r\n" +
                "XXX" + fc + "Z\r\n" +
                "------WebKitFormBoundaryAiKTL587TYHtWO2p--";

        long t = System.currentTimeMillis();
        FormData form = new FormData(data.getBytes());
        System.out.println("Time: " + (System.currentTimeMillis() - t));
        System.out.println(form.getValue("starting"));
        System.out.println(form.getValue("ending"));
        System.out.println(form.getFileName());
        System.out.println(form.getFileStream().length);
    }
}
