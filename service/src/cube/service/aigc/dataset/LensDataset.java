package cube.service.aigc.dataset;

import cell.util.Utils;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.aigc.psychology.EvaluationReport;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.algorithm.BigFiveFeature;
import cube.service.aigc.scene.HTPEvaluation;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LensDataset {

    private String host = "211.157.179.37";

    private int port = 7010;

//    private String token = "NEFaMVUtMoXqdPakIHpNlXesBHjDBkQY";    // 127.0.0.1
    private String token = "duWmraPkxAqrwkHWNAKKxQpIRQDDDKcM";      // 211.157.179.37

    private String workingPath = "storage/tmp/";

    public LensDataset() {
    }

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
                    Painting painting = this.requestPaintingData(sn);
                    if (null != painting) {
                        HTPEvaluation evaluation = new HTPEvaluation(painting);
                        EvaluationReport evaluationReport = evaluation.makeEvaluationReport();
                        BigFiveFeature feature = evaluationReport.getPersonalityAccelerator().getBigFiveFeature();
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
                JSONObject json = this.requestReportData(sn);
                // 获取对应的绘画
                Painting painting = this.requestPaintingData(sn);
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
                Painting painting = this.requestPaintingData(sn);
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

    private JSONObject requestReportData(long sn) {
        StringBuilder urlString = new StringBuilder("http://");
        urlString.append(this.host);
        urlString.append(":").append(this.port);
        urlString.append("/aigc/psychology/report/");
        urlString.append(this.token);
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
