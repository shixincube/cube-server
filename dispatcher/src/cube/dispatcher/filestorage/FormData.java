/*
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

    private byte[] chunk;

    /**
     *
     * @param content
     */
    public FormData(byte[] content, int offset, int length) {
        byte[] data = new byte[length];
        System.arraycopy(content, 0, data, 0, length);
        this.buf = new FlexibleByteBuffer(1024);
        this.multipart = this.parse(data);
        this.buf = null;
    }

    public String getFileName() {
        return this.fileName;
    }

    public byte[] getFileChunk() {
        return this.chunk;
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
            else if (b1 == '\n') {
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

        boolean rn = false;

        long t = System.currentTimeMillis();
        ActionType action = ActionType.TO_FIND_DISPOSITION;

        while (cursor < content.length) {
            byte b = content[cursor++];

            switch (action) {
                case TO_FIND_DISPOSITION:
                    if (b == '\r' || b == '\n') {
                        if (b == '\r') {
                            ++cursor;
                            rn = true;
                        }

                        buf.flip();
                        disposition = new String(buf.array(), 0, buf.limit(), Charset.forName("UTF-8"));
                        buf.clear();

                        action = ActionType.TO_FIND_CONTENT;
                        break;
                    }

                    buf.put(b);
                    break;

                case TO_FIND_CONTENT:
                    if (b == '\r' || b == '\n') {
                        if (b == '\r') {
                            ++cursor;
                        }

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
                    if (b == '\r' || b == '\n') {
                        if (b == '\r') {
                            ++cursor;
                        }

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
                    int tailLen = boundary.length() + 2;
                    if (content.length - cursor > tailLen) {
                        int chunkLen = content.length - cursor - tailLen - (rn ? 1 : -1);
                        chunk = new byte[chunkLen];
                        System.arraycopy(content, cursor - (rn ? 1 : 2), chunk, 0, chunk.length);
                        cursor = content.length;
                        break;
                    }
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


    /*public static void main(String[] args) {
        StringBuilder buf = new StringBuilder(Utils.randomString(32 * 1024));
        buf.append("\r\n").append(Utils.randomString(16 * 1024));
        buf.append("\n").append(Utils.randomString(16 * 1024));
        String fc = buf.toString();

        String data1 = "------WebKitFormBoundaryAiKTL587TYHtWO2p\r\n" +
                "Content-Disposition: form-data; name=\"cursor\"\r\n" +
                "\r\n" +
                "0\r\n" +
                "------WebKitFormBoundaryAiKTL587TYHtWO2p\r\n" +
                "Content-Disposition: form-data; name=\"size\"\r\n" +
                "\r\n" +
                (fc.length() + 2) + "\r\n" +
                "------WebKitFormBoundaryAiKTL587TYHtWO2p\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"3周年.png\"\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "\r\n" +
                "#" + fc + "%\r\n" +
                "------WebKitFormBoundaryAiKTL587TYHtWO2p--";

        String data2 = "-----------------------------131022975738869420362846154883\n" +
                "Content-Disposition: form-data; name=\"cid\"\n" +
                "\n" +
                "10000\n" +
                "-----------------------------131022975738869420362846154883\n" +
                "Content-Disposition: form-data; name=\"fileSize\"\n" +
                "\n" +
                "705113\n" +
                "-----------------------------131022975738869420362846154883\n" +
                "Content-Disposition: form-data; name=\"cursor\"\n" +
                "\n" +
                "0\n" +
                "-----------------------------131022975738869420362846154883\n" +
                "Content-Disposition: form-data; name=\"size\"\n" +
                "\n" +
                "524289\n" +
                "-----------------------------131022975738869420362846154883\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"3周年.png\"\n" +
                "Content-Type: application/octet-stream\n" +
                "\n" +
                "#PNG\n" +
                "\u001A\n" +
                "IHDR" + "���I(dt�Wk\u0000n|�������6�Z��\\�Z�A��i�c�\u001Az�;I�� ����[9�+�\u0002E��\u0012�mb\u001F�=��3�)\u0002������\u0018J���(�p_F*�NYs�\u001F\u001B\u0001����Ł\u0014�2���h��/\u0017��S�\u00001�Q���\u0000��љ��\t@33���\u0000k\u0001\u0019��3�i��0��\u0013~�ȣ(�V>c�����} \u0019\u001Cr�_�������!\u0018\u001C�ba�2�'��{�ҹ�oQ\u001F?{�v\u0019�\u0010h\u0002�\u0000p%\n" +
                "-----------------------------131022975738869420362846154883--";

        String[] list = new String[] { data1, data2 };

        for (String data : list) {
            long t = System.currentTimeMillis();
            byte[] bytes = data.getBytes();
            FormData form = new FormData(bytes,0, bytes.length);
            System.out.println("\nTime: " + (System.currentTimeMillis() - t));
            System.out.println("cursor: " + form.getValue("cursor"));
            System.out.println("size: " + form.getValue("size"));
            System.out.println("filename: " + form.getFileName());
            System.out.println("chunk: " + form.getFileChunk().length
                    + " - [" + (char)form.getFileChunk()[0] + "][" + (char)form.getFileChunk()[form.getFileChunk().length - 1] + "]");
        }
    }*/
}
