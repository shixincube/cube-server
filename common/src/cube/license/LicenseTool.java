/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.license;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

/**
 * 许可证工具。
 */
public class LicenseTool {

    /* 生成私匙库
    # validity：私钥的有效期多少天
    # alias：私钥别称
    # keyalg：指定加密算法，默认是DSA
    # keystore: 指定私钥库文件的名称(生成在当前目录)
    # storepass：指定私钥库的密码(获取keystore信息所需的密码)
    # keypass：指定别名条目的密码(私钥的密码)
    keytool -genkeypair -storetype JKS -keysize 512 -validity 799 -alias "BaizeLicense" -keyalg "RSA" -keystore "BaizeLicense.keystore" -storepass "cubeteam" -keypass "cube-2024" -dname "CN=Xu, OU=Yang, O=Cube, L=gz, ST=gd, C=CN"
    */

    /* 生成证书
    # alias：私钥别称
    # keystore：指定私钥库的名称(在当前目录查找)
    # storepass: 指定私钥库的密码
    # file：证书名称
    keytool -exportcert -alias "BaizeLicense" -keystore "BaizeLicense.keystore" -storepass "cubeteam" -file "license.cer"
    */

    /* 生成公匙库
    # alias：公钥别称
    # file：证书名称
    # keystore：公钥文件名称
    # storepass：指定私钥库的密码
    keytool -import -alias "BaizeLicensePublicKey" -file "license.cer" -keystore "public-key.keystore" -storepass "cubeteam"
    */

    //非对称密钥算法
    private static final String KEY_ALGORITHM = "SHA1withRSA";

    private LicenseTool() {
    }

    public static LicenseConfig getLicenseConfig(File file) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        return new LicenseConfig(file);
    }

    public static PrivateKey getPrivateKey(String path) {
        FileInputStream fis = null;
        PrivateKey privateKey = null;

        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            fis = new FileInputStream(new File(path, CertificateInfo.PRIVATE_KEY_FILE));
            keyStore.load(fis, CertificateInfo.KEYSTORE_PASSWORD.toCharArray());
            privateKey = (PrivateKey) keyStore.getKey(CertificateInfo.PRIVATE_ALIAS, CertificateInfo.KEY_PASSWORD.toCharArray());
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }

        return privateKey;
    }

    public static byte[] sign(byte[] data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance(KEY_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data);
        return Base64.getEncoder().encode(signature.sign());
    }


    /**
     * 通过证书文件获取公钥。
     *
     * @return 返回公钥。
     */
    public static PublicKey getPublicKeyFromCer(String path) {
        PublicKey publicKey = null;
        FileInputStream fis = null;

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            fis = new FileInputStream(new File(path, CertificateInfo.CER_FILE));
            Certificate certificate = cf.generateCertificate(fis);
            publicKey = certificate.getPublicKey();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }

        return publicKey;
    }

    /**
     * 获取证书超期时间。
     *
     * @param path
     * @return
     */
    public static Date getExpiration(String path) {
        Date expiration = null;
        FileInputStream fis = null;

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            fis = new FileInputStream(new File(path, CertificateInfo.CER_FILE));
            Certificate certificate = cf.generateCertificate(fis);
            expiration = ((X509Certificate) certificate).getNotAfter();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Nothing
                }
            }
        }

        return expiration;
    }

    /**
     * 验证签名。
     *
     * @param data
     * @param sign
     * @param publicKey
     * @return
     */
    public static boolean verify(byte[] data, String sign, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance(KEY_ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(Base64.getDecoder().decode(sign));
    }
}
