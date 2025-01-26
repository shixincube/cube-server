/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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


    public static void main(String[] args) {
        License license = new License();

        // 生成签名串
        if (!license.signLicense("./license/")) {
            System.out.println("Sign failed");
            return;
        }
        else {
            System.out.println("Sign success");
        }

        // 校验签名串
//        if (license.verifyLicence("./license/")) {
//            System.out.println("Sign is correct");
//        }
//        else {
//            System.out.println("Sign is error");
//        }
    }
}
