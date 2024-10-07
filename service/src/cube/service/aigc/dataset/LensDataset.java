package cube.service.aigc.dataset;

import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.PaintingReport;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LensDataset {

    private String host = "127.0.0.1";

    private int port = 7010;

    private String token = "NEFaMVUtMoXqdPakIHpNlXesBHjDBkQY";

    private String workPath = "./storage/tmp/";

    public LensDataset() {
    }



    public void saveAsDataset(File srcFile, File destFile) {
        List<PaintingReport> reports = new ArrayList<>();

        try {
            Logger.d(this.getClass(), "Reads file: " + srcFile.getAbsolutePath());

            byte[] data = Files.readAllBytes(Paths.get(srcFile.getAbsolutePath()));
            JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));

            for (int i = 0; i < array.length(); ++i) {
                JSONObject json = array.getJSONObject(i);
                PaintingReport report = new PaintingReport(json);
                reports.add(report);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Logger.i(this.getClass(), "Report num: " + reports.size());

        ReportDataset dataset = new ReportDataset(reports);
        dataset.saveReportScoreToFile(destFile);
    }

    public void writeToFile(File file) {
        JSONArray data = new JSONArray();
        int pageIndex = 0;

        while (true) {
            Logger.i(this.getClass(), "read report data: " + pageIndex);
            JSONArray list = this.requestReportData(pageIndex);
            ++pageIndex;
            if (null == list) {
                break;
            }

            for (int i = 0; i < list.length(); ++i) {
                data.put(list.getJSONObject(i));

                // 获取对应的绘画
                
            }
        }

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(data.toString(4).getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            Logger.i(this.getClass(), "write report data to file: " + file.getName());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != outputStream) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private JSONArray requestReportData(int pageIndex) {
        int size = 10;

        StringBuilder urlString = new StringBuilder("http://");
        urlString.append(this.host);
        urlString.append(":").append(this.port);
        urlString.append("/aigc/psychology/report/");
        urlString.append(this.token);
        urlString.append("?page=").append(pageIndex);
        urlString.append("&size=").append(size);
        urlString.append("&state=0");

        HttpURLConnection connection = null;

        FlexibleByteBuffer buf = new FlexibleByteBuffer(100 * 1024);

        try {
            URL url = new URL(urlString.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setUseCaches(true);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            // 连接
            connection.connect();
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                byte[] bytes = new byte[10240];
                int length = 0;
                while ((length = is.read(bytes)) > 0) {
                    buf.put(bytes, 0, length);
                }
            }

            buf.flip();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }

        try {
            JSONObject data = new JSONObject(new String(buf.array(), 0, buf.limit()));
            JSONArray list = data.getJSONArray("list");
            if (list.length() == 0) {
                return null;
            }

            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private Painting requestPaintingData(long sn) {
        StringBuilder urlString = new StringBuilder("http://");
        urlString.append(this.host);
        urlString.append(":").append(this.port);
        urlString.append("/aigc/psychology/painting/");
        urlString.append(this.token);
        urlString.append("?sn=").append(sn);

        HttpURLConnection connection = null;

        FlexibleByteBuffer buf = new FlexibleByteBuffer(100 * 1024);

        Painting painting = null;

        try {
            URL url = new URL(urlString.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Connection", "keep-alive");
            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setUseCaches(true);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            // 连接
            connection.connect();
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                byte[] bytes = new byte[10240];
                int length = 0;
                while ((length = is.read(bytes)) > 0) {
                    buf.put(bytes, 0, length);
                }
            }

            buf.flip();

            JSONObject json = new JSONObject(new String(buf.array(), 0, buf.limit(), StandardCharsets.UTF_8));
            painting = new Painting(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }

        return painting;
    }
}
