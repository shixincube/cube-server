/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.util;

import cell.util.Utils;
import cell.util.log.Logger;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class FileDownloader {

    private String workPath = "storage/downloads/";

    private int sizeLimit = 10 * 1024 * 1024;

    public FileDownloader() {
//        System.setProperty("https.protocols", "TLSv1.2,TLSv1.1");
        File path = new File(this.workPath);
        if (!path.exists()) {
            path.mkdirs();
        }
    }

    public FileDownloader(int sizeLimit) {
        this();
        if (sizeLimit > 0) {
            this.sizeLimit = sizeLimit;
        }
    }

    public File downloadFile(String fileUrl, FileDownloaderListener listener) {
        int contentLength = 0;
        String contentType = null;

        InputStream is = null;
        FileOutputStream fos = null;

        // 本地文件名
        String filename = System.currentTimeMillis() + "_" + Utils.randomString(8);

        try {
            URL url = new URL(fileUrl);
            String protocol = url.getProtocol();
            if (protocol.equalsIgnoreCase("https")) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                TrustManager[] tm = new TrustManager[]{ new DefaultX509TrustManager() };
                // 初始化
                sslContext.init(null, tm, null);
                SSLSocketFactory ssf = new TLS12SSLSocketFactory(sslContext.getSocketFactory());

                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
                conn.setDoOutput(false);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Baize/3.0");
                conn.setSSLSocketFactory(ssf);
                conn.connect();

                if (HttpURLConnection.HTTP_OK != conn.getResponseCode()) {
                    Logger.e(this.getClass(), "Can NOT connect: " + fileUrl);
                    listener.onError("Unable to connect to the server");
                    return null;
                }

                contentLength = conn.getContentLength();
                if (contentLength > 0 && contentLength > this.sizeLimit) {
                    Logger.e(this.getClass(), "The content length exceeds the limit: " + fileUrl +
                            " - " + contentLength + "/" + this.sizeLimit);
                    listener.onError("The content length exceeds the limit: " + fileUrl +
                            " - " + contentLength + "/" + this.sizeLimit);
                    return null;
                }

                contentType = conn.getContentType();
                is = conn.getInputStream();
            }
            else {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(false);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Baize/3.0");
                conn.connect();

                if (HttpURLConnection.HTTP_OK != conn.getResponseCode()) {
                    Logger.e(this.getClass(), "Can NOT connect: " + fileUrl);
                    listener.onError("Unable to connect to the server");
                    return null;
                }

                contentLength = conn.getContentLength();
                if (contentLength > 0 && contentLength > this.sizeLimit) {
                    Logger.e(this.getClass(), "The content length exceeds the limit: " + fileUrl +
                            " - " + contentLength + "/" + this.sizeLimit);
                    listener.onError("The content length exceeds the limit: " + fileUrl +
                            " - " + contentLength + "/" + this.sizeLimit);
                    return null;
                }

                contentType = conn.getContentType();
                is = conn.getInputStream();
            }

            // 解析文件类型
            FileType fileType = FileType.matchMimeType(contentType);
            filename = filename + "." + fileType.getPreferredExtension();

            File file = new File(this.workPath, filename);
            fos = new FileOutputStream(file);
            byte[] bytes = new byte[10240];
            int length = 0;
            // 复位长度
            int downloadLength = 0;
            while ((length = is.read(bytes)) > 0) {
                fos.write(bytes, 0, length);

                downloadLength += length;
                listener.onProgress(filename, downloadLength, contentLength);

                if (downloadLength > this.sizeLimit) {
                    Logger.e(this.getClass(), "The download length exceeds the limit: " + fileUrl +
                            " - " + downloadLength + "/" + this.sizeLimit);
                    listener.onError("The download length exceeds the limit: " + fileUrl +
                            " - " + downloadLength + "/" + this.sizeLimit);
                    return null;
                }
            }
            fos.flush();

            listener.onCompleted(file);

            return file;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#downloadFile", e);
            listener.onError(e.getMessage());
            return null;
        } finally {
            if (null != is) {
                try {
                    is.close();
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


    protected class TrustAnyHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession sslSession) {
            // 信任所有主机
            return true;
        }
    }

    protected class DefaultX509TrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    protected class TLS12SSLSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;

        public TLS12SSLSocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;}

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            SSLSocket socket = (SSLSocket) delegate.createSocket(s, host, port, autoClose);
            socket.setEnabledProtocols(new String[]{"TLSv1.2"}); // 强制使用TLSv1.2
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            SSLSocket socket = (SSLSocket) delegate.createSocket(host, port);
            socket.setEnabledProtocols(new String[]{"TLSv1.2"});
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            SSLSocket socket = (SSLSocket) delegate.createSocket(host, port, localHost, localPort);
            socket.setEnabledProtocols(new String[]{"TLSv1.2"});
            return socket;
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            SSLSocket socket = (SSLSocket) delegate.createSocket(host, port);
            socket.setEnabledProtocols(new String[]{"TLSv1.2"});
            return socket;
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            SSLSocket socket = (SSLSocket) delegate.createSocket(address, port, localAddress, localPort);
            socket.setEnabledProtocols(new String[]{"TLSv1.2"});
            return socket;
        }
    }

    public static void main(String[] args) {
        String fileUrl = "https://cdn.mos.cms.futurecdn.net/vChK6pTy3vN3KbYZ7UU7k3-320-80.jpg";
        FileDownloader downloader = new FileDownloader();
        File file = downloader.downloadFile(fileUrl, new FileDownloaderListener() {
            @Override
            public void onProgress(String fileName, int progressLength, int totalLength) {
                System.out.println("progress (" + fileName + ") - " + progressLength + " / " + totalLength);
            }

            @Override
            public void onCompleted(File file) {
                System.out.println("onCompleted: " + file.getAbsolutePath());
            }

            @Override
            public void onError(String error) {
                System.out.println("Error: " + error);
            }
        });
        if (null != file) {
            System.out.println("Download: " + file.getName() + " - " + file.length());
        }
        else {
            System.out.println("Download failed");
        }
    }
}
