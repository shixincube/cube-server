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

package cube.service.test;

import cube.license.LicenseConfig;
import cube.license.LicenseTool;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;

public class License {

    protected License() {
    }

    public boolean signLicense(String path) {
        PrivateKey privateKey = LicenseTool.getPrivateKey(path);
        if (null == privateKey) {
            return false;
        }

        LicenseConfig config = LicenseTool.getLicenseConfig(new File(path, "cube.license"));
        String signContent = config.extractSignContent();

        try {
            byte[] signResult = LicenseTool.sign(signContent.getBytes(StandardCharsets.UTF_8), privateKey);
            System.out.println(new String(signResult, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean verifyLicence(String path) {
        PublicKey publicKey = LicenseTool.getPublicKeyFromCer(path);
        if (null == publicKey) {
            return false;
        }

        LicenseConfig config = LicenseTool.getLicenseConfig(new File(path, "cube.license"));
        String signContent = config.extractSignContent();

        byte[] data = signContent.getBytes(StandardCharsets.UTF_8);

        try {
            return LicenseTool.verify(data, config.signature, publicKey);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /*
    public static void main(String[] args) {
        License license = new License();

//        if (!license.signLicense("./license/")) {
//            System.out.println("Sign failed");
//            return;
//        }
//        else {
//            System.out.println("Sign success");
//        }

        if (license.verifyLicence("./license/")) {
            System.out.println("Sign is correct");
        }
        else {
            System.out.println("Sign is error");
        }
    }*/
}
