/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.ferryhouse.tool;

import cell.util.Base64;
import cell.util.collection.FlexibleByteBuffer;
import cube.util.CipherUtils;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class LicenceTool {

    private LicenceTool() {
    }

    public static void createFile(String domain, long beginning, String password, File output) throws IOException {
        JSONObject data = new JSONObject();
        data.put("domain", domain);
        data.put("beginning", beginning);
        data.put("duration", 180L * 24 * 60 * 60 * 1000);
        data.put("limit", 20);

        String dataString = data.toString();
        byte[] dataBytes = CipherUtils.encrypt(dataString.getBytes(StandardCharsets.UTF_8),
                password.getBytes(StandardCharsets.UTF_8));
        // 密文转 Base64
        String base64String = Base64.encodeBytes(dataBytes);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(output);
            fos.write(base64String.getBytes(StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void writeFile(JSONObject data, String password, File output) throws IOException {
        String dataString = data.toString();
        byte[] dataBytes = CipherUtils.encrypt(dataString.getBytes(StandardCharsets.UTF_8),
                password.getBytes(StandardCharsets.UTF_8));
        // 密文转 Base64
        String base64String = Base64.encodeBytes(dataBytes);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(output);
            fos.write(base64String.getBytes(StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * 从文件提取数据。
     *
     * @param file
     * @return
     */
    public static JSONObject extractData(File file, String password) throws IOException {
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
            FlexibleByteBuffer buf = new FlexibleByteBuffer(256);
            byte[] bytes = new byte[256];
            int length = 0;
            while ((length = fis.read(bytes)) > 0) {
                buf.put(bytes, 0, length);
            }
            buf.flip();

            byte[] data = new byte[buf.limit()];
            System.arraycopy(buf.array(), 0, data, 0, data.length);

            // 从 Base64 还原
            byte[] base64Bytes = Base64.decode(new String(data, StandardCharsets.UTF_8));

            byte[] dataBytes = CipherUtils.decrypt(base64Bytes, password.getBytes(StandardCharsets.UTF_8));
            String dataString = new String(dataBytes, StandardCharsets.UTF_8);
            return new JSONObject(dataString);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void main(String[] args) {
        File outputFile = new File("config/licence-first");
        System.out.println("Licence: " + outputFile.getAbsolutePath());
        try {
            LicenceTool.createFile("first-prototype-box",
                    System.currentTimeMillis(),
                    "shixincube.com",
                    outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            JSONObject data = LicenceTool.extractData(outputFile, "shixincube.com");
            System.out.println("Domain: " + data.getString("domain"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
