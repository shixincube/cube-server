/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.dispatcher.util;

import cell.util.collection.FlexibleByteBuffer;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Form 数据格式描述和解析。
 */
public class FormData {

    private static final String sContentType = "Content-Type".toLowerCase();
    private static final String sOctetStream = "octet-stream".toLowerCase();
    private static final String sJson = "json".toLowerCase();

    private FlexibleByteBuffer buf;

    private HashMap<String, String> multipart;

    private String fileName;

    private byte[] chunk;

    /**
     * 构造函数。
     *
     * @param content
     * @param offset
     * @param length
     */
    public FormData(byte[] content, int offset, int length) {
        byte[] data = new byte[length];
        System.arraycopy(content, offset, data, 0, length);
        this.buf = new FlexibleByteBuffer(1024);
        this.multipart = this.parse(data);
        this.buf = null;
    }

    /**
     * 构造函数。
     *
     * @param formFile
     * @param outputFile
     */
    public FormData(File formFile, File outputFile) {
        FileInputStream fis = null;
        FileOutputStream fos = null;

        this.buf = new FlexibleByteBuffer(1024);
        boolean isReturn = false;
        String boundary = null;
        int endBoundaryLength = 0;
        boolean segment = false;
        boolean read = false;

        try {
            fis = new FileInputStream(formFile);
            fos = new FileOutputStream(outputFile);

            int data = -1;
            while ((data = fis.read()) != -1) {
                byte b = (byte) data;

                if (read) {
                    // 第一位直接写入文件
                    fos.write(b);

                    int length = 0;
                    byte[] bytes = new byte[endBoundaryLength];
                    byte[] pending = null;
                    byte[] last = null;

                    while ((length = fis.read(bytes)) > 0) {
                        if (length < endBoundaryLength) {
                            // 结束
                            this.buf.clear();
                            if (null != pending) {
                                this.buf.put(pending);
                            }
                            if (null != last) {
                                this.buf.put(last);
                            }
                            this.buf.put(bytes, 0, length);
                            this.buf.flip();

                            // 剔除结束符
                            fos.write(this.buf.array(), 0, this.buf.limit() - endBoundaryLength - (isReturn ? 2 : 1));
                        }
                        else {
                            if (null != pending) {
                                fos.write(pending);
                            }

                            if (null == last) {
                                last = new byte[length];
                            }
                            else {
                                if (null == pending) {
                                    pending = new byte[length];
                                }

                                System.arraycopy(last, 0, pending, 0, length);
                            }

                            System.arraycopy(bytes, 0, last, 0, length);
                        }
                    }

                    break;
                }

                if (b == '\r') {
                    data = fis.read();
                    byte next = (byte) data;
                    if (next != '\n') {
                        this.buf.put(b);
                        this.buf.put(next);
                        continue;
                    }
                    else {
                        isReturn = true;
                    }
                }

                if (b == '\r' || b == '\n') {
                    this.buf.flip();

                    if (segment) {
                        String line = new String(this.buf.array(), 0, this.buf.limit());
                        line = line.toLowerCase();
                        if (line.contains("name=\"file\"") && line.contains("filename")) {
                            // 文件数据
                            segment = true;
                            int pos = line.lastIndexOf("filename");
                            this.fileName = line.substring(pos + 10, line.length() - 1);
                        }
                        else if (line.contains(sContentType) &&
                                (line.contains(sOctetStream) || !line.contains(sJson))) {
                            // 文件类型
                            segment = false;
                            // 进入读取数据流程
                            read = true;
                            // 跳过空白行
                            data = fis.read();
                            if (data != -1) {
                                b = (byte) data;
                                if (b == '\r') {
                                    // 读 '\n'
                                    fis.read();
                                }
                            }
                            else {
                                break;
                            }
                        }
                        else {
                            // 其他字段
                            // TODO
                            segment = false;
                        }
                    }
                    else {
                        if (null == boundary) {
                            boundary = new String(this.buf.array(), 0, this.buf.limit());
                            endBoundaryLength = boundary.length() + 2;
                            segment = true;
                        }
                        else {
                            String line = new String(this.buf.array(), 0, this.buf.limit());
                            if (line.equals(boundary)) {
                                segment = true;
                            }
                        }
                    }

                    this.buf.clear();
                }
                else {
                    this.buf.put(b);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Nothing
                }
            }

            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }
    }

    public int numMultipart() {
        return this.multipart.size();
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

    private HashMap<String, String> analysis(byte[] content) {
        HashMap<String, String> result = new HashMap<>();

        String rawString = new String(content, StandardCharsets.UTF_8);
        String boundary = rawString.substring(0, rawString.indexOf("\n")).trim();
        String[] parts = rawString.split(boundary);

        for (String data : parts) {
            data = data.trim();
            if (data.length() <= 1) {
                continue;
            }

            String key = null;
            String value = null;

            boolean processFile = false;

            String[] tmp = data.split("\n");
            for (String line : tmp) {
                line = line.trim();
                if (line.length() <= 3) {
                    continue;
                }

                if (line.indexOf("name=\"file\";") > 0) {
                    this.fileName = line.substring(line.indexOf("filename=") + 10, line.length() - 1);
                    processFile = true;
                    break;
                }
                else if (line.indexOf("name=") > 0) {
                    key = line.substring(line.indexOf("name=") + 6, line.length() - 1);
                    processFile = false;
                }
                else {
                    value = line;
                }
            }

            if (processFile) {
                if (tmp.length >= 4) {
                    StringBuilder buf = new StringBuilder();
                    for (int i = 3; i < tmp.length; ++i) {
                        String text = tmp[i].trim();
                        if (text.contains(boundary)) {
                            break;
                        }
                        buf.append(text);
                        buf.append("\n");
                    }
                    String fileData = buf.delete(buf.length() - 1, buf.length()).toString();
                    this.chunk = fileData.getBytes(StandardCharsets.UTF_8);
                }

                processFile = false;
            }

            if (null != key && null != value) {
                result.put(key, value);
            }
        }

        return result;
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

        //long t = System.currentTimeMillis();
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

                        if (lowerData.contains(sContentType)) {
                            if (lowerData.contains(sOctetStream) || !lowerData.contains(sJson)) {
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


    public static void main(String[] args) {
        /*StringBuilder buf = new StringBuilder(Utils.randomString(32 * 1024));
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
         */

//        String data3 = "-----------------------------8008900863708314687188379310\r\n" +
//                "Content-Disposition: form-data; name=\"cid\"\r\n" +
//                "\r\n" +
//                "67890001\r\n" +
//                "-----------------------------8008900863708314687188379310\r\n" +
//                "Content-Disposition: form-data; name=\"file\"; filename=\"TeamViewer.png\"\r\n" +
//                "Content-Type: image/png\r\n" +
//                "\r\n" +
//                "TEST-PNG\r\n" +
//                "CubeForm\r\n" +
//                "-----------------------------8008900863708314687188379310--\r\n";
//
//        byte[] bytes = data3.getBytes();
//        FormData form = new FormData(bytes, 0, bytes.length);
//        System.out.println("filename : " + form.fileName);
//        System.out.println("------------------------------------------------");
//        System.out.println(new String(form.chunk, StandardCharsets.UTF_8));
//        System.out.println("------------------------------------------------");
//
//        for (Map.Entry<String, String> e : form.multipart.entrySet()) {
//            System.out.println(e.getKey() + " = " + e.getValue());
//        }

        /*File[] files = new File[] {
                new File("dispatcher/cube-hub-files/data1.form"),
                new File("dispatcher/cube-hub-files/data2.form")
        };
        if (!files[0].exists()) {
            try {
                Files.write(Paths.get(files[0].getAbsolutePath()), data1.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!files[1].exists()) {
            try {
                Files.write(Paths.get(files[1].getAbsolutePath()), data2.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < files.length; ++i) {
            File formFile = files[i];
            File outputFile = new File("dispatcher/cube-hub-files/" + formFile.getName() + ".dat");
            FormData formData = new FormData(formFile, outputFile);
            System.out.println("Filename: " + formData.getFileName());
            System.out.println("File: " + outputFile.getName() + " - " + outputFile.length());
        }*/

        /*String[] list = new String[] { data1, data2 };

        for (String data : list) {
            long t = System.currentTimeMillis();
            byte[] bytes = data.getBytes();
            FormData form = new FormData(bytes, 0, bytes.length);
            System.out.println("\nTime: " + (System.currentTimeMillis() - t));
            System.out.println("cursor: " + form.getValue("cursor"));
            System.out.println("size: " + form.getValue("size"));
            System.out.println("filename: " + form.getFileName());
            System.out.println("chunk: " + form.getFileChunk().length
                    + " - [" + (char)form.getFileChunk()[0] + "][" + (char)form.getFileChunk()[form.getFileChunk().length - 1] + "]");
        }*/
    }
}
