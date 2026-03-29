/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.util;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class FileDownloader {

    private String workPath = "cache/downloads/";

    public FileDownloader() {
        File path = new File(this.workPath);
        if (!path.exists()) {
            path.mkdirs();
        }
    }

    public File downloadFile(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            url.getProtocol();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public class TrustAnyHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }

    public class DefaultX509TrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
