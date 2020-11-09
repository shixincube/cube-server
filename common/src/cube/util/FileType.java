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

package cube.util;

/**
 * 文件类型。
 */
public enum FileType {

    BMP("bmp", new byte[] { 0x42, 0x4d }),

    GIF("gif", new byte[] { 0x47, 0x49, 0x46, 0x38 }),

    JPEG("jpg", new byte[] { (byte)0xff, (byte)0xd8, (byte)0xff, (byte)0xe0 }),

    PNG("png", new byte[] { (byte)0x89, 0x50, 0x4e, 0x47 }),

    UNKNOWN("unknown", new byte[0]);

    private String extension;

    private byte[] magicNumber;

    FileType(String extension, byte[] magicNumber) {
        this.extension = extension;
        this.magicNumber = magicNumber;
    }

    public String getExtension() {
        return this.extension;
    }

    public byte[] getMagicNumber() {
        return this.magicNumber;
    }

    public static FileType parse(String extension) {
        if (JPEG.extension.equalsIgnoreCase(extension)) return JPEG;
        else if (PNG.extension.equalsIgnoreCase(extension)) return PNG;
        else if (GIF.extension.equalsIgnoreCase(extension)) return GIF;
        else if (BMP.extension.equalsIgnoreCase(extension)) return BMP;
        else return UNKNOWN;
    }

    /**
     * 根据文件魔数提取文件类型。
     *
     * @param data
     * @return
     */
    public static FileType extractFileType(byte[] data) {
        if (data.length < 4) {
            return UNKNOWN;
        }

        byte b1 = data[0];
        byte b2 = data[1];
        byte b3 = data[2];
        byte b4 = data[3];

        if (b1 == JPEG.magicNumber[0] && b2 == JPEG.magicNumber[1]
                && b3 == JPEG.magicNumber[2] && b4 == JPEG.magicNumber[3]) {
            return JPEG;
        }
        else if (b1 == PNG.magicNumber[0] && b2 == PNG.magicNumber[1]
                && b3 == PNG.magicNumber[2] && b4 == PNG.magicNumber[3]) {
            return PNG;
        }
        else if (b1 == GIF.magicNumber[0] && b2 == GIF.magicNumber[1]
                && b3 == GIF.magicNumber[2] && b4 == GIF.magicNumber[3]) {
            return GIF;
        }
        else if (b1 == BMP.magicNumber[0] && b2 == BMP.magicNumber[1]) {
            return BMP;
        }
        else {
            return UNKNOWN;
        }
    }
}
