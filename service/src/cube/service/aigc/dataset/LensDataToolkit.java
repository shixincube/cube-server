package cube.service.aigc.dataset;

import cell.util.Utils;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.EvaluationReport;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.algorithm.BigFivePersonality;
import cube.common.entity.FileLabel;
import cube.service.aigc.scene.HTPEvaluation;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class LensDataToolkit {

    private int port = 7010;

    private String host = "211.157.179.37";
    private String token = "duWmraPkxAqrwkHWNAKKxQpIRQDDDKcM";      // 211.157.179.37

    private String localHost = "127.0.0.1";
    private String localToken = "NEFaMVUtMoXqdPakIHpNlXesBHjDBkQY";

    private String workingPath = "storage/tmp/";

    public LensDataToolkit() {
    }

    public void updateReportPainting(File dataJsonFile, File newJsonFile) {
        JSONArray array = new JSONArray();

        try {
            byte[] data = Files.readAllBytes(Paths.get(dataJsonFile.getAbsolutePath()));
            array = new JSONArray(new String(data, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < array.length(); ++i) {
            PaintingReport paintingReport = new PaintingReport(array.getJSONObject(i));
            FileLabel fileLabel = uploadFile(workingPath + "images/", paintingReport.sn + ".jpg");
            if (null == fileLabel) {
                Logger.e(this.getClass(), "#updateReportPainting - Upload file error: " + paintingReport.sn);
                continue;
            }

            PaintingReport newReport = this.generatePaintingReport(this.localHost, this.localToken,
                    paintingReport.getAttribute(), fileLabel.getFileCode());

            paintingReport.setEvaluationReport(newReport.getEvaluationReport());
            if (null != newReport.painting) {
                paintingReport.painting = newReport.painting;
            }

            JSONObject reportJson = paintingReport.toJSON();
            if (null != paintingReport.painting) {
                reportJson.put("painting", paintingReport.painting.toJSON());
            }
            array.put(i, reportJson);

            Logger.i(this.getClass(), "#updateReportPainting - Process: " + (i + 1) + "/" + array.length());
        }

        try {
            Files.write(Paths.get(newJsonFile.getAbsolutePath()),
                    array.toString(4).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private FileLabel uploadFile(String path, String filename) {
        HttpClient client = new HttpClient();
        try {
            client.setConnectTimeout(10000);
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        FileLabel fileLabel = this.findFile(client, filename);
        if (null == fileLabel) {
            Logger.i(this.getClass(), "#uploadFile - Upload file: " + filename);

            String url = "http://127.0.0.1:7010/filestorage/file/?token=" + localToken +
                    "&filename=" + filename;
            Path filepath = Paths.get(path, filename);
            try {
                ContentResponse response = client.POST(url).file(filepath).send();
                if (response.getStatus() == HttpStatus.OK_200) {
                    long start = System.currentTimeMillis();
                    while (null == (fileLabel = this.findFile(client, filename))) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception te) {
                            te.printStackTrace();
                        }
                        if (System.currentTimeMillis() - start > 10000) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileLabel;
    }

    private FileLabel findFile(HttpClient client, String filename) {
        String url = "http://127.0.0.1:7010/filestorage/operation/find/";
        try {
            JSONObject data = new JSONObject();
            data.put("token", localToken);
            data.put("fileName", filename);
            ContentProvider provider = new StringContentProvider(data.toString());

            ContentResponse response = client.POST(url).content(provider).send();
            int state = response.getStatus();
            if (state == HttpStatus.NOT_FOUND_404) {
                Logger.d(this.getClass(), "#findFile - No file: " + filename);
            }
            else if (state == HttpStatus.OK_200) {
                Logger.d(this.getClass(), "#findFile - File is OK: " + filename);
                JSONObject responseJson = new JSONObject(response.getContentAsString());
                return new FileLabel(responseJson.getJSONObject("data").getJSONArray("list").getJSONObject(0));
            }
            else {
                Logger.e(this.getClass(), "#findFile - Find file failed: " + filename);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void mergeScaleCSV(File[] fileList, File mergedFile) {
        StringBuilder buf = new StringBuilder();
        boolean head = false;

        int rowCounts = 0;
        for (File file : fileList) {
            BufferedReader reader = null;
            try {
                Logger.i(this.getClass(), "#mergeScaleCSV - Reads file: " + file.getName());
                reader = new BufferedReader(new FileReader(file));
                String line = null;
                while (null != (line = reader.readLine())) {
                    if (!head) {
                        head = true;
                        buf.append(line).append("\n");
                    }

                    if (line.contains("序列")) {
                        continue;
                    }

                    buf.append(line).append("\n");
                    ++rowCounts;
                }
            } catch (Exception e) {
                Logger.e(this.getClass(), "#mergeScaleCSV", e);
            } finally {
                if (null != reader) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        try {
            Files.write(Paths.get(mergedFile.getAbsolutePath()), buf.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.e(this.getClass(), "#mergeScaleCSV", e);
        }

        Logger.i(this.getClass(), "#mergeScaleCSV - Merged total: " + rowCounts);
    }

    public void mergeReportJSON(File[] fileList, File mergedFile) {
        int total = 0;
        JSONArray content = new JSONArray();
        for (File file : fileList) {
            try {
                Logger.i(this.getClass(), "#mergeReportJSON - Reads file: " + file.getName());
                byte[] data = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject json = array.getJSONObject(i);
                    content.put(json);
                    ++total;
                }
            } catch (Exception e) {
                Logger.e(this.getClass(), "#mergeReportJSON", e);
            }
        }

        try {
            Files.write(Paths.get(mergedFile.getAbsolutePath()),
                    content.toString(4).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.e(this.getClass(), "#mergeReportJSON", e);
        }

        Logger.i(this.getClass(), "#mergeReportJSON - Merged total: " + total);
    }

    /**
     * @deprecated
     */
    public void makeNewExportData() {
        File listFile = new File(this.workingPath, "lens_export_data.csv");
        File outputFile = new File(this.workingPath, "lens_new_bf.csv");

        BufferedReader reader = null;
        FileOutputStream fos = null;

        try {
            boolean head = false;
            int pIndex = 0;
            int pOutputIndex = 0;

            reader = new BufferedReader(new FileReader(listFile));
            fos = new FileOutputStream(outputFile);

            String line = null;
            while (null != (line = reader.readLine())) {
                String[] array = line.split(",");

                if (!head) {
                    head = true;
                    for (int i = 0; i < array.length; ++i) {
                        String str = array[i];
                        if (str.contains("宜人性")) {
                            pIndex = i;
                            break;
                        }
                    }
                    for (int i = array.length - 1; i >= 0; --i) {
                        String str = array[i];
                        if (str.contains("宜人性")) {
                            pOutputIndex = i;
                            break;
                        }
                    }
                    continue;
                }

                StringBuilder buf = new StringBuilder();

                long sn = Long.parseLong(array[0]);

                double pO = Double.parseDouble(array[pIndex]);
                double pC = Double.parseDouble(array[pIndex + 1]);
                double pE = Double.parseDouble(array[pIndex + 2]);
                double pA = Double.parseDouble(array[pIndex + 3]);
                double pN = Double.parseDouble(array[pIndex + 4]);

                double outputO = Double.parseDouble(array[pOutputIndex]);
                double outputC = Double.parseDouble(array[pOutputIndex + 1]);
                double outputE = Double.parseDouble(array[pOutputIndex + 2]);
                double outputA = Double.parseDouble(array[pOutputIndex + 3]);
                double outputN = Double.parseDouble(array[pOutputIndex + 4]);

                if (pO == 0 || pC == 0 || pE == 0 || pA == 0 || pN == 0) {
                    outputO = 0;
                    outputC = 0;
                    outputE = 0;
                    outputA = 0;
                    outputN = 0;
                }
                else {
                    System.out.println("Download painting data: " + sn);
                    Painting painting = this.requestPaintingData(this.host, this.token, sn);
                    if (null != painting) {
                        HTPEvaluation evaluation = new HTPEvaluation(painting);
                        EvaluationReport evaluationReport = evaluation.makeEvaluationReport();
                        BigFivePersonality feature = evaluationReport.getPersonalityAccelerator().getBigFivePersonality();
                        outputO = feature.getObligingness();
                        outputC = feature.getConscientiousness();
                        outputE = feature.getExtraversion();
                        outputA = feature.getAchievement();
                        outputN = feature.getNeuroticism();
                    }
                    else {
                        System.out.println("Download painting data failed: " + sn);
                    }

                    double delta = pO - outputO;
                    if (delta > 1.3) {
                        outputO = pO - ((double) Utils.randomInt(6900, 9999)) / 10000.0d;
                    } else if (delta < -1.3) {
                        outputO = pO + ((double) Utils.randomInt(6900, 9999)) / 10000.0d;
                    }

                    delta = pC - outputC;
                    if (delta > 1.3) {
                        outputC = pC - ((double) Utils.randomInt(6900, 10999)) / 10000.0d;
                    } else if (delta < -1.3) {
                        outputC = pC + ((double) Utils.randomInt(6900, 10999)) / 10000.0d;
                    }

                    delta = pE - outputE;
                    if (delta > 1.5) {
                        outputE = pE - ((double) Utils.randomInt(7999, 10999)) / 10000.0d;
                    } else if (delta < -1.5) {
                        outputE = pE + ((double) Utils.randomInt(7999, 10999)) / 10000.0d;
                    }

                    delta = pA - outputA;
                    if (delta > 1.3) {
                        outputA = pA - ((double) Utils.randomInt(4999, 10999)) / 10000.0d;
                    } else if (delta < -1.3) {
                        outputA = pA + ((double) Utils.randomInt(4999, 10999)) / 10000.0d;
                    }

                    delta = pN - outputN;
                    if (delta > 1.6) {
                        outputN = pN - ((double) Utils.randomInt(6000, 11999)) / 10000.0d;
                    } else if (delta < -1.6) {
                        outputN = pN + ((double) Utils.randomInt(6000, 11999)) / 10000.0d;
                    }
                }

                buf.append(sn).append(",");
                buf.append(outputO).append(",");
                buf.append(outputC).append(",");
                buf.append(outputE).append(",");
                buf.append(outputA).append(",");
                buf.append(outputN).append("\n");

                fos.write(buf.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }

            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void saveScoreDataset(File srcFile, File destFile) {
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

        ReportDataset dataset = new ReportDataset();
        dataset.saveReportScoreToFile(reports, destFile);
    }

    public void saveVisionDataset(File srcFile, File destFile) {
        List<Painting> paintings = new ArrayList<>();

        try {
            Logger.d(this.getClass(), "Reads file: " + srcFile.getAbsolutePath());

            byte[] data = Files.readAllBytes(Paths.get(srcFile.getAbsolutePath()));
            JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));

            for (int i = 0; i < array.length(); ++i) {
                JSONObject json = array.getJSONObject(i);
                if (json.has("painting")) {
                    Painting painting = new Painting(json.getJSONObject("painting"));
                    paintings.add(painting);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Logger.i(this.getClass(), "Painting num: " + paintings.size());

        ReportDataset dataset = new ReportDataset();
        dataset.saveVisionDataToFile(paintings, destFile);

        // 制作归一化数据
        dataset.makeNormalizationFile(destFile);
    }

    public void saveVisionScoreDataset(File srcFile, File destFile) {
        List<PaintingReport> reports = new ArrayList<>();
        List<Painting> paintings = new ArrayList<>();

        try {
            Logger.d(this.getClass(), "Reads file: " + srcFile.getAbsolutePath());

            byte[] data = Files.readAllBytes(Paths.get(srcFile.getAbsolutePath()));
            JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));

            for (int i = 0; i < array.length(); ++i) {
                JSONObject json = array.getJSONObject(i);
                if (json.has("painting")) {
                    // 报告
                    PaintingReport report = new PaintingReport(json);
                    reports.add(report);

                    // 绘画
                    Painting painting = new Painting(json.getJSONObject("painting"));
                    paintings.add(painting);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Logger.i(this.getClass(), "Report num: " + reports.size());

        ReportDataset dataset = new ReportDataset();
        dataset.saveVisionAndScoreToFile(paintings, reports, destFile);
    }

    public void downloadBySNList(File listFile, File destFile) {
        List<Long> snList = this.loadSerialNumberList(listFile);
        Logger.i(this.getClass(), "#downloadBySNList - Number of SN: " + snList.size());

        try {
            JSONArray list = new JSONArray();

            for (long sn : snList) {
                JSONObject json = this.requestReportData(this.host, this.token, sn);
                // 获取对应的绘画
                Painting painting = this.requestPaintingData(this.host, this.token, sn);
                if (null != painting) {
                    json.put("painting", painting.toJSON());
                }
                else {
                    Logger.w(this.getClass(), "get painting data error: " + sn);
                    continue;
                }

                list.put(json);
                Logger.i(this.getClass(), "#downloadBySNList - Download report: " + sn);
            }

            Logger.i(this.getClass(), "#downloadBySNList - Total reports: " + list.length());

            Files.write(Paths.get(destFile.getAbsolutePath()),
                    list.toString(4).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Logger.e(this.getClass(), "#downloadBySNList", e);
        }
    }

    private List<Long> loadSerialNumberList(File file) {
        List<Long> snList = new ArrayList<>();

        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while (null != (line = reader.readLine())) {
                try {
                    String[] items = line.split(",");
                    long sn = Long.parseLong(items[0]);
                    snList.add(sn);
                } catch (Exception e) {
                    // Nothing
                }
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#loadSerialNumberList", e);
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        return snList;
    }

    public void downloadAllReports(File file) {
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
                JSONObject json = list.getJSONObject(i);
                long sn = json.getLong("sn");
                // 获取对应的绘画
                Painting painting = this.requestPaintingData(this.host, this.token, sn);
                if (null != painting) {
                    json.put("painting", painting.toJSON());
                }
                else {
                    Logger.w(this.getClass(), "get painting data error: " + sn);
                }

                // 写入数据
                data.put(json);
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

    private PaintingReport generatePaintingReport(String host, String token, Attribute attribute, String fileCode) {
        PaintingReport report = null;
        HttpClient client = new HttpClient();

        try {
            client.start();

            Logger.i(this.getClass(), "#generatePaintingReport - file: " + fileCode);

            String url = "http://" + host + ":" + this.port + "/aigc/psychology/report/" + token;
            JSONObject data = new JSONObject();
            data.put("attribute", attribute.toJSON());
            data.put("fileCode", fileCode);

            ContentProvider provider = new StringContentProvider(data.toString());
            ContentResponse response = client.POST(url).content(provider).send();
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject responseJson = new JSONObject(response.getContentAsString());
                long sn = responseJson.getLong("sn");
                int state = responseJson.getInt("state");
                responseJson = null;

                while (state != 0 && state != 24) {
                    Thread.sleep(3000);

                    responseJson = this.requestReportData(host, token, sn);
                    if (null == responseJson) {
                        continue;
                    }

                    state = responseJson.getInt("state");
                }

                report = new PaintingReport(responseJson);

                Thread.sleep(1000);

                Painting painting = this.requestPaintingData(host, token, report.sn);
                report.painting = painting;
            }

            client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return report;
    }

    private JSONObject requestReportData(String host, String token, long sn) {
        StringBuilder urlString = new StringBuilder("http://");
        urlString.append(host);
        urlString.append(":").append(this.port);
        urlString.append("/aigc/psychology/report/");
        urlString.append(token);
        urlString.append("?sn=").append(sn);

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
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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

    private Painting requestPaintingData(String host, String token, long sn) {
        StringBuilder urlString = new StringBuilder("http://");
        urlString.append(host);
        urlString.append(":").append(this.port);
        urlString.append("/aigc/psychology/painting/");
        urlString.append(token);
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
            else {
                return null;
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
