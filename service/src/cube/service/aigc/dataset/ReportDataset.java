package cube.service.aigc.dataset;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.psychology.*;
import cube.aigc.psychology.composition.BigFiveFactor;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.aigc.psychology.composition.HexagonDimension;
import cube.aigc.psychology.composition.HexagonDimensionScore;
import cube.service.aigc.scene.HTPEvaluation;
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

    public final static int OUTPUT_SCL = 1;

    public final static int OUTPUT_PANAS = 2;

    public final static int OUTPUT_BFP = 3;

    public ReportDataset() {
    }

    public void splitTrainTestDataset(File sourceDataset, File destTrainDataset, File destTestDataset, float testRatio) {
        List<String> lines = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(sourceDataset));
            String line = null;
            while (null != (line = reader.readLine())) {
                lines.add(line);
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
        }

        String header = lines.remove(0);
        int numTest = Math.round(lines.size() * testRatio);
        // 生成测试集索引
        List<Integer> testIndies = new ArrayList<>();
        while (testIndies.size() < numTest) {
            int index = Utils.randomInt(0, lines.size() - 1);
            if (!testIndies.contains(index)) {
                testIndies.add(index);
            }
        }

        FileOutputStream trainFile = null;
        FileOutputStream testFile = null;
        try {
            trainFile = new FileOutputStream(destTrainDataset);
            testFile = new FileOutputStream(destTestDataset);

            trainFile.write(header.getBytes(StandardCharsets.UTF_8));
            trainFile.write("\n".getBytes(StandardCharsets.UTF_8));

            testFile.write(header.getBytes(StandardCharsets.UTF_8));
            testFile.write("\n".getBytes(StandardCharsets.UTF_8));

            for (int i = 0; i < lines.size(); ++i) {
                if (testIndies.contains(i)) {
                    testFile.write((lines.get(i) + "\n").getBytes(StandardCharsets.UTF_8));
                }

                trainFile.write((lines.get(i) + "\n").getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != trainFile) {
                try {
                    trainFile.close();
                } catch (IOException e) {
                }
            }
            if (null != testFile) {
                try {
                    testFile.close();
                } catch (IOException e) {
                }
            }
        }

        Logger.i(this.getClass(), "Total: " + lines.size() + ", train: " + lines.size()
                + ", test: " + numTest);
    }

    public void makeEvaluationDatasetFromScaleData(int outputType, File reportJsonFile, File scaleCSVFile,
                                                   File datasetFile, boolean resetEvaluation) throws Exception {
        Map<Long, ScaleDataRow> rowMap = new LinkedHashMap<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(scaleCSVFile));
            String line = null;
            while (null != (line = reader.readLine())) {
                if (line.contains("序列")) {
                    continue;
                }
                ScaleDataRow row = new ScaleDataRow(line, false);
                if (!row.isValid()) {
                    continue;
                }
                rowMap.put(row.sn, row);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != reader) {
                reader.close();
            }
        }

        byte[] data = Files.readAllBytes(Paths.get(reportJsonFile.getAbsolutePath()));
        JSONArray reportJSONArray = new JSONArray(new String(data, StandardCharsets.UTF_8));

        int total = 0;

        // 表头
        StringBuilder buf = new StringBuilder();
        buf.append("gender").append(",").append("age").append(",");
        for (Indicator indicator : Indicator.sortByPriority()) {
            if (indicator == Indicator.Unknown || indicator == Indicator.Psychosis) {
                continue;
            }
            buf.append(indicator.code.toLowerCase(Locale.ROOT)).append(",");
        }
        buf.append("|");
        switch (outputType) {
            case OUTPUT_SCL:
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
                break;
            case OUTPUT_PANAS:
                buf.append(",").append("positive_affect");
                buf.append(",").append("negative_affect");
                break;
            case OUTPUT_BFP:
                for (BigFiveFactor bfp : BigFiveFactor.values()) {
                    buf.append(",").append(bfp.code.toLowerCase(Locale.ROOT));
                }
                break;
            default:
                break;
        }
        buf.append("\n");

        for (int i = 0; i < reportJSONArray.length(); ++i) {
            JSONObject json = reportJSONArray.getJSONObject(i);
            JSONObject paintingJson = json.getJSONObject("painting");

            // Outputs
            ScaleDataRow row = rowMap.get(json.getLong("sn"));
            if (null == row) {
                Logger.e(this.getClass(), "#makeEvaluationDatasetFromScaleData - No scale data: " + json.getLong("sn"));
                continue;
            }

            // Inputs
            EvaluationReport report = null;
            if (resetEvaluation) {
                Painting painting = new Painting(paintingJson);
                if (!painting.isValid()) {
                    Logger.w(this.getClass(), "#makeEvaluationDatasetFromScaleData - Painting is NOT valid: " + json.getLong("sn"));
                    continue;
                }
                HTPEvaluation evaluation = new HTPEvaluation(0, painting);
                report = evaluation.makeEvaluationReport();
            }
            else {
                PaintingReport paintingReport = new PaintingReport(json);
                report = paintingReport.getEvaluationReport();
            }

            List<EvaluationScore> list = report.getFullEvaluationScores();
            double[] scores = new double[list.size()];
            for (int n = 0; n < scores.length; ++n) {
                EvaluationScore score = list.get(n);
                scores[n] = score.calcScore();
            }

            // 归一化
            boolean skip = false;
            // Version 1 使用 softmax
//             scores = FloatUtils.softmax(scores);
//            for (double s : scores) {
//                if (Math.abs(s) <= 0.0001) {
//                    skip = true;
//                    break;
//                }
//            }
            if (skip) {
                Logger.w(this.getClass(), "#makeEvaluationDatasetFromScaleData - Skip: " + json.getLong("sn"));
                continue;
            }

            buf.append(row.getGender()).append(",");
            buf.append(row.getAge()).append(",");

            for (double score : scores) {
                buf.append(score).append(",");
            }
            buf.append("|");
            // 归一化
            double[] outputs = null;
            switch (outputType) {
                case OUTPUT_SCL:
                    outputs = row.splitSCL(0.1);
                    break;
                case OUTPUT_PANAS:
                    outputs = row.splitPANAS(0.01);
                    break;
                case OUTPUT_BFP:
                    outputs = row.splitBFP(0.1);
                    break;
                default:
                    break;
            }
            for (double value : outputs) {
                buf.append(",").append(value);
            }
            buf.append("\n");

            ++total;
        }

        Files.write(Paths.get(datasetFile.getAbsolutePath()), buf.toString().getBytes(StandardCharsets.UTF_8));

        Logger.i(this.getClass(), "#makeEvaluationDatasetFromScaleData - Total: " + total + "/" + reportJSONArray.length());
    }

    public void makePaintingDatasetFromScaleData(File reportJsonFile, File scaleCSVFile, File datasetFile) throws Exception {
        Map<Long, ScaleDataRow> rowMap = new LinkedHashMap<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(scaleCSVFile));
            String line = null;
            while (null != (line = reader.readLine())) {
                if (line.contains("序列")) {
                    continue;
                }
                ScaleDataRow row = new ScaleDataRow(line, true);
                rowMap.put(row.sn, row);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != reader) {
                reader.close();
            }
        }

        byte[] data = Files.readAllBytes(Paths.get(reportJsonFile.getAbsolutePath()));
        JSONArray reportJSONArray = new JSONArray(new String(data, StandardCharsets.UTF_8));

        StringBuilder buf = new StringBuilder();
        int total = 0;

        for (int i = 0; i < reportJSONArray.length(); ++i) {
            ++total;

            JSONObject json = reportJSONArray.getJSONObject(i);
            JSONObject paintingJson = json.getJSONObject("painting");

            // Outputs
            ScaleDataRow row = rowMap.get(json.getLong("sn"));
            if (null == row) {
                Logger.e(this.getClass(), "#makePaintingDatasetFromScaleData - No scale data: " + json.getLong("sn"));
                continue;
            }

            // Inputs
//            PaintingReport report = new PaintingReport(json);
            Painting painting = new Painting(paintingJson);
//            report.painting = painting;
            PaintingAccelerator accelerator = new PaintingAccelerator(painting);

            if (!accelerator.isValid()) {
                Logger.w(this.getClass(), "#makePaintingDatasetFromScaleData - Painting is NOT valid: " + json.getLong("sn"));
                continue;
            }

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
                buf.append(",").append("positive_affect");
                buf.append(",").append("negative_affect");
                for (BigFiveFactor bfp : BigFiveFactor.values()) {
                    buf.append(",").append(bfp.code.toLowerCase(Locale.ROOT));
                }
                buf.append("\n");
            }

            buf.append(accelerator.formatParameterAsCSV(true));
            buf.append(",").append("|");
            // 归一化
            row.normalization();
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
                    double[] data = new double[array.length - 1];
                    int separatorIndex = 0;
                    int dataIndex = 0;
                    for (int i = 0; i < array.length; ++i) {
                        String str = array[i];
                        if (str.equals("|")) {
                            separatorIndex = i;
                            continue;
                        }
                        data[dataIndex] = Double.parseDouble(str);
                        ++dataIndex;
                    }
                    // 归一化
                    double[] result = FloatUtils.normalization(data, 0, 100);
                    StringBuilder buf = new StringBuilder();
                    int ti = result.length - 1;
                    for (int i = 0; i < result.length; ++i) {
                        if (separatorIndex == i) {
                            buf.append("|,");
                        }
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
            Logger.e(this.getClass(), "#makeNormalizationFile", e);

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

        Attribute attribute = new Attribute("male", 18, false);

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

                buf.append(accelerator.formatParameterAsCSV(false));
                buf.append(",").append("|");
                List<EvaluationScore> scores = report.getEvaluationReport().getEvaluationScores();
                scores = this.alignScores(scores);
                for (EvaluationScore score : scores) {
//                    buf.append(",").append(score.calcScore());
                    buf.append(",").append(score.getRate(attribute).value);
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

                content = accelerator.formatParameterAsCSV(false);
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

        public ScaleDataRow(String csvString, boolean softmax) {
            String[] array = csvString.split(",");
            this.sn = Long.parseLong(array[0]);
            this.data = new double[19];

            this.data[0] = array[2].contains("女") ? 0.1 : 0.9;
            this.data[1] = Double.parseDouble(array[3]) * 0.01;

            if (softmax) {
                double[] base = FloatUtils.softmax(new double[]{ this.data[0], this.data[1] });
                this.data[0] = base[0];
                this.data[1] = base[1];
            }

            for (int i = 0; i < 10; ++i) {
                this.data[i + 2] = Double.parseDouble(array[i + 4]);
            }
            this.data[12] = Double.parseDouble(array[11 + 4]);
            this.data[13] = Double.parseDouble(array[12 + 4]);
            for (int i = 14; i < 19; ++i) {
                this.data[i] = Double.parseDouble(array[i + 1 + 2]);
            }
        }

        public boolean isValid() {
            if (this.data[2] == this.data[3] &&
                this.data[3] == this.data[4] &&
                this.data[4] == this.data[5] &&
                this.data[5] == this.data[6] ) {
                return false;
            }
            return true;
        }

        public double getGender() {
            return this.data[0];
        }

        public double getAge() {
            return this.data[1];
        }

        public double[] splitSCL(double scale) {
            double[] values = new double[10];
            for (int i = 0; i < values.length; ++i) {
                values[i] = this.data[i + 2] * scale;
            }
            return values;
        }

        public double[] splitPANAS(double scale) {
            double[] values = new double[2];
            values[0] = this.data[12] * scale;
            values[1] = this.data[13] * scale;
            return values;
        }

        public double[] splitBFP(double scale) {
            double[] values = new double[5];
            values[0] = this.data[14] * scale;
            values[1] = this.data[15] * scale;
            values[2] = this.data[16] * scale;
            values[3] = this.data[17] * scale;
            values[4] = this.data[18] * scale;
            return values;
        }

        public double[] normalization() {
            double[] result = new double[this.data.length];
            for (int i = 0; i < 10; ++i) {
                result[i] = this.data[i + 2] / 10.0d;
            }
            result[10] = this.data[12] / 100.0d;
            result[11] = this.data[13] / 100.0d;
            for (int i = 12; i < 17; ++i) {
                result[i] = this.data[i + 2] / 10.0d;
            }
            return result;
        }
    }
}
