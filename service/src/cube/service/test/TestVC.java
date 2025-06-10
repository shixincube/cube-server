package cube.service.test;

import cell.util.Utils;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.common.entity.VerificationCode;
import cube.util.HttpClientFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class TestVC {

    public static void main(String[] args) {
        method2();
    }

    public static void method1() {
        String phoneNumber = "18511617300";

        VerificationCode verificationCode = new VerificationCode(Utils.randomString(32),
                "+86", "CN",
                phoneNumber, System.currentTimeMillis(), 15 * 60 * 1000);
        verificationCode.setCode(Utils.randomNumberString(4));

        final String token = "UiTOoigtfSSPBMWmoeQmGXEwNDDlbBtk";
        final String url = "http://211.157.179.38:8080/api/sms/vcode";
        HttpClient httpClient = null;
        try {
            JSONObject json = verificationCode.toJSON();
            json.put("token", token);
            StringContentProvider provider = new StringContentProvider(json.toString());

            httpClient = HttpClientFactory.getInstance().borrowHttpClient();
            ContentResponse response = httpClient.POST(url)
                    .header("Content-Type", "application/json")
                    .timeout(10, TimeUnit.SECONDS)
                    .content(provider)
                    .send();
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject result = new JSONObject(response.getContentAsString());
                int state = result.getInt("state");
                if (state != HttpStatus.OK_200) {
                    String message = result.has("error") ? result.getString("error") : result.getString("message");
                    Logger.w(TestVC.class, "#requestVerificationCode - Error: " + message);
                    // 返回
                    return;
                }
            }
            else {
                Logger.w(TestVC.class, "#requestVerificationCode - Failed:\n" + json.toString(4) +
                        "\n -> \n" + response.getContentAsString());
                // 返回
                return;
            }
        } catch (Exception e) {
            Logger.e(TestVC.class, "#requestVerificationCode", e);
        } finally {
            if (null != httpClient) {
                HttpClientFactory.getInstance().returnHttpClient(httpClient);
            }
        }
    }

    public static void method2() {
        String phoneNumber = "13488687986";

        VerificationCode verificationCode = new VerificationCode(Utils.randomString(32),
                "+86", "CN",
                phoneNumber, System.currentTimeMillis(), 15 * 60 * 1000);
        verificationCode.setCode(Utils.randomNumberString(4));

        final String token = "UiTOoigtfSSPBMWmoeQmGXEwNDDlbBtk";
        final String urlString = "http://211.157.179.38:8080/api/sms/vcode";

        HttpURLConnection connection = null;

        FlexibleByteBuffer buf = new FlexibleByteBuffer(10 * 1024);

        try {
            JSONObject json = verificationCode.toJSON();
            json.put("token", token);

            InputStream inputStream = new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8));

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            // 连接
            connection.connect();

            OutputStream os = connection.getOutputStream();
            byte[] outputBuf = new byte[8192];
            int length = 0;
            while ((length = inputStream.read(outputBuf)) > 0) {
                os.write(outputBuf, 0, length);
            }
            os.flush();
            // 关闭流
            os.close();

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                byte[] bytes = new byte[8192];
                while ((length = is.read(bytes)) > 0) {
                    buf.put(bytes, 0, length);
                }
            }
            else {
                Logger.w(TestVC.class, "#requestVerificationCode - Failed:\n" + json.toString(4) +
                        "\n -> \nstate: " + code);
                return;
            }

            buf.flip();

            JSONObject result = new JSONObject(new String(buf.array(), 0, buf.limit(), StandardCharsets.UTF_8));
            int state = result.getInt("state");
            if (state != HttpStatus.OK_200) {
                String message = result.has("error") ? result.getString("error") : result.getString("message");
                Logger.w(TestVC.class, "#requestVerificationCode - Error: " + message);
            }
            else {
                Logger.d(TestVC.class, "#requestVerificationCode - Success:\n" + json.toString(4) +
                        "\n -> \n" + result.toString(4));
            }
        } catch (Exception e) {
            Logger.e(TestVC.class, "#requestVerificationCode", e);
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
    }
}
