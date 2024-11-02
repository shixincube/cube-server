package cube.service.aigc.dataset;

import cell.util.log.Logger;
import cube.aigc.psychology.Indicator;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.PaintingAccelerator;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.composition.BigFivePersonality;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.aigc.psychology.composition.HexagonDimension;
import cube.aigc.psychology.composition.HexagonDimensionScore;
import cube.util.FileUtils;
import cube.util.FloatUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ReportDataset {

    public ReportDataset() {
    }

    public void makeDatasetFromScaleData(File reportJsonFile, File scaleCSVFile, File datasetFile) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get(reportJsonFile.getAbsolutePath()));
        JSONArray reportJSONArray = new JSONArray(new String(data, StandardCharsets.UTF_8));

        Map<Long, ScaleDataRow> rowMap = new LinkedHashMap<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(scaleCSVFile));
            String line = null;
            while (null != (line = reader.readLine())) {
                if (line.contains("序列")) {
                    continue;
                }
                ScaleDataRow row = new ScaleDataRow(line);
                rowMap.put(row.sn, row);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != reader) {
                reader.close();
            }
        }

        StringBuilder buf = new StringBuilder();
        int total = 0;

        for (int i = 0; i < reportJSONArray.length(); ++i) {
            ++total;

            JSONObject json = reportJSONArray.getJSONObject(i);
            JSONObject paintingJson = json.getJSONObject("painting");

            // Outputs
            ScaleDataRow row = rowMap.get(json.getLong("sn"));
            if (null == row) {
                Logger.e(this.getClass(), "#makeDatasetFromScaleData - No scale data: " + json.getLong("sn"));
                continue;
            }

            // Inputs
//            PaintingReport report = new PaintingReport(json);
            Painting painting = new Painting(paintingJson);
//            report.painting = painting;
            PaintingAccelerator accelerator = new PaintingAccelerator(painting);

            if (1 == total) {
                buf.append(accelerator.formatCSVHead());
                buf.append(",").append("|");
                buf.append(",").append("somatization");
                buf.append(",").append("obsession");
                buf.append(",").append("interpersonal");
                buf.append(",").append("depression");
                buf.append(",").append("anxiety");
                buf.append(",").append("hostile");
                buf.append(",").append("horror");
                buf.append(",").append("paranoid");
                buf.append(",").append("psychosis");
                buf.append(",").append("sleep_diet");
                buf.append(",").append("scl_90_total");
                buf.append(",").append("positive_affect");
                buf.append(",").append("negative_affect");
                for (BigFivePersonality bfp : BigFivePersonality.values()) {
                    buf.append(",").append(bfp.code.toLowerCase(Locale.ROOT));
                }
                buf.append("\n");
            }

            buf.append(accelerator.formatParameterAsCSV());
            buf.append(",").append("|");
            for (double value : row.data) {
                buf.append(",").append(value);
            }
            buf.append("\n");
        }

        Files.write(Paths.get(datasetFile.getAbsolutePath()), buf.toString().getBytes(StandardCharsets.UTF_8));
    }

    public void makeNormalizationFile(File file) {
        BufferedReader reader = null;
        FileOutputStream os = null;

        File output = new File(file.getParentFile(),
                FileUtils.extractFileName(file.getName()) + "_normalization.csv");
        Logger.i(this.getClass(), "#makeNormalizationFile - Output file: " + output.getAbsolutePath());

        try {
            os = new FileOutputStream(output);
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            int row = 0;
            while (null != (line = reader.readLine())) {
                if (0 == row) {
                    os.write(line.getBytes(StandardCharsets.UTF_8));
                    os.write("\n".getBytes(StandardCharsets.UTF_8));
                }
                else {
                    String[] array = line.split(",");
                    double[] data = new double[array.length];
                    for (int i = 0; i < array.length; ++i) {
                        data[i] = Double.parseDouble(array[i]);
                    }
                    // 归一化
                    double[] result = FloatUtils.normalization(data, 0, 100);
                    StringBuilder buf = new StringBuilder();
                    int ti = result.length - 1;
                    for (int i = 0; i < result.length; ++i) {
                        buf.append(result[i]);
                        if (i != ti) {
                            buf.append(",");
                        }
                    }
                    buf.append("\n");
                    os.write(buf.toString().getBytes(StandardCharsets.UTF_8));
                }
                ++row;

                Logger.i(this.getClass(), "#makeNormalizationFile - processing: " + row + " row");
            }
        } catch (Exception e) {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                }
            }

            if (null != os) {
                try {
                    os.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    public boolean saveVisionAndScoreToFile(List<Painting> paintingList, List<PaintingReport> reportList, File file) {
        if (reportList.size() != paintingList.size()) {
            Logger.w(this.getClass(), "#saveVisionAndIndicatorToFile - List length error");
            return false;
        }

        FileOutputStream os = null;
        boolean head = false;

        int inputLen = 0;
        int outputLen = 0;

        try {
            os = new FileOutputStream(file);

            for (int i = 0; i < paintingList.size(); ++i) {
                Painting painting = paintingList.get(i);
                PaintingReport report = reportList.get(i);

                PaintingAccelerator accelerator = new PaintingAccelerator(painting);

                StringBuilder buf = new StringBuilder();

                if (!head) {
                    head = true;
                    buf.append(accelerator.formatCSVHead());

                    String[] array = buf.toString().split(",");
                    inputLen = array.length;

                    // 以 | 分隔
                    buf.append(",").append("|");

                    for (Indicator indicator : Indicator.sortByPriority()) {
                        buf.append(",").append(indicator.code.toLowerCase(Locale.ROOT));
                        outputLen += 1;
                    }
                    for (HexagonDimension hd : HexagonDimension.values()) {
                        buf.append(",").append(hd.name.toLowerCase(Locale.ROOT));
                        outputLen += 1;
                    }
                    buf.append("\n");
                    os.write(buf.toString().getBytes(StandardCharsets.UTF_8));

                    buf = new StringBuilder();
                }

                buf.append(accelerator.formatParameterAsCSV());
                buf.append(",").append("|");
                List<EvaluationScore> scores = report.getEvaluationReport().getEvaluationScores();
                scores = this.alignScores(scores);
                for (EvaluationScore score : scores) {
//                    buf.append(",").append(score.calcScore());
                    buf.append(",").append(score.getRate().value);
                }
                HexagonDimensionScore dimensionScore = report.getDimensionScore();
                for (HexagonDimension hd : HexagonDimension.values()) {
                    int score = dimensionScore.getDimensionScore(hd);
                    buf.append(",").append(score);
                }
                buf.append("\n");
                os.write(buf.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Logger.e(this.getClass(), "#saveVisionAndIndicatorToFile", e);
        } finally {
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }

        Logger.i(this.getClass(), "#saveVisionAndIndicatorToFile - Input params length: " + inputLen);
        Logger.i(this.getClass(), "#saveVisionAndIndicatorToFile - Output params length: " + outputLen);

        return true;
    }

    public void saveVisionDataToFile(List<Painting> paintings, File file) {
        FileOutputStream fs = null;
        boolean head = false;
        int columns = 0;

        try {
            fs = new FileOutputStream(file);

            for (Painting painting : paintings) {
                String content = null;

                PaintingAccelerator accelerator = new PaintingAccelerator(painting);
                if (!head) {
                    head = true;
                    content = accelerator.formatCSVHead();
                    // 写入头
                    fs.write(content.getBytes(StandardCharsets.UTF_8));
                    fs.write("\n".getBytes(StandardCharsets.UTF_8));

                    columns = content.split(",").length;

                    Logger.i(this.getClass(), "#saveVisionDataToFile - columns: " + columns);
                }

                content = accelerator.formatParameterAsCSV();
                int num = content.split(",").length;
                if (columns != num) {
                    content = null;
                    Logger.w(this.getClass(), "#saveVisionDataToFile - The number of columns is inconsistent : " +
                            columns + " != " + num);
                }

                if (null != content) {
                    fs.write(content.getBytes(StandardCharsets.UTF_8));
                    fs.write("\n".getBytes(StandardCharsets.UTF_8));
                }
            }

            fs.flush();
        } catch (Exception e) {
            Logger.e(this.getClass(), "#saveVisionDataToFile", e);
        } finally {
            if (null != fs) {
                try {
                    fs.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void saveReportScoreToFile(List<PaintingReport> reports, File file) {
        FileOutputStream stream = null;

        try {
            StringBuilder buf = new StringBuilder();
            for (Indicator indicator : Indicator.sortByPriority()) {
                buf.append(indicator.code).append(",");
            }
            buf.append("\n");

            stream = new FileOutputStream(file);
            stream.write(buf.toString().getBytes(StandardCharsets.UTF_8));

            buf = new StringBuilder();
            for (PaintingReport report : reports) {
                List<EvaluationScore> scores = report.getEvaluationReport().getEvaluationScores();
                if (scores.size() < 5) {
                    Logger.w(this.getClass(), "#saveReportScoreToFile - score list is empty: " + report.sn);
                    continue;
                }
                scores = this.alignScores(scores);
                for (EvaluationScore score : scores) {
                    buf.append(score.calcScore()).append(",");
                }
                buf.append("\n");
            }
            stream.write(buf.toString().getBytes(StandardCharsets.UTF_8));
            stream.flush();
        } catch (Exception e) {
            Logger.e(this.getClass(), "#saveReportScoreToFile", e);
        } finally {
            try {
                stream.close();
            } catch (Exception e) {
            }
        }
    }

    private List<EvaluationScore> alignScores(List<EvaluationScore> scores) {
        List<EvaluationScore> result = new ArrayList<>();

        List<Indicator> indicatorList = Indicator.sortByPriority();
        for (Indicator indicator : indicatorList) {
            EvaluationScore score = null;
            for (EvaluationScore es : scores) {
                if (es.indicator == indicator) {
                    score = es;
                    break;
                }
            }

            if (null != score) {
                result.add(score);
            }
            else {
                result.add(new EvaluationScore(indicator));
            }
        }

        return result;
    }


    public class ScaleDataRow {

        public long sn;

        public double[] data;

        public ScaleDataRow(String csvString) {
            String[] array = csvString.split(",");
            this.sn = Long.parseLong(array[0]);
            this.data = new double[18];
            for (int i = 0; i < this.data.length; ++i) {
                this.data[i] = Double.parseDouble(array[i + 4]);
            }
        }
    }
}
