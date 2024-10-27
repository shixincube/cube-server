package cube.service.aigc.dataset;

import cell.util.log.Logger;
import cube.aigc.psychology.Indicator;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.PaintingAccelerator;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.composition.EvaluationScore;
import cube.util.FileUtils;
import cube.util.FloatUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ReportDataset {

    public ReportDataset() {
    }

    public void makeNormalizationFile(File file) {
        BufferedReader reader = null;
        FileOutputStream os = null;

        File output = new File(file.getParentFile(),
                FileUtils.extractFileName(file.getName()) + "_normalization.csv");
        Logger.i(this.getClass(), "#convertNormalFile - Output file: " + output.getAbsolutePath());

        try {
            os = new FileOutputStream(output);
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            int row = 0;
            while (null != (line = reader.readLine())) {
                if (0 == row) {
                    os.write(line.getBytes(StandardCharsets.UTF_8));
                }
                else {
                    String[] array = line.split(",");
                    double[] data = new double[array.length];
                    for (int i = 0; i < array.length; ++i) {
                        data[i] = Double.parseDouble(array[i]);
                    }
                    double[] result = FloatUtils.normalization(data, 0, 100);
                    StringBuilder buf = new StringBuilder();

                }
                ++row;

                Logger.i(this.getClass(), "#convertNormalFile - processing: " + row + " row");
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
}
