/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2020-2025 Ambrose Xu.
 */

package cube.service.cv;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.entity.BarCode;
import cube.common.entity.FileLabel;
import cube.common.notice.GetFile;
import cube.common.notice.SaveFile;
import cube.core.AbstractModule;
import cube.service.aigc.AIGCService;
import cube.util.CodeUtils;
import cube.util.FileUtils;
import cube.util.PrintUtils;
import cube.vision.Size;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 辅助工具。
 */
public class ToolKit {

    private final static ToolKit instance = new ToolKit();

    private AIGCService service;

    private AbstractModule fileStorageService;

    private final String workingPath = "storage/tmp/";

    private ToolKit() {
    }

    public final static ToolKit getInstance() {
        return ToolKit.instance;
    }

    public void setService(AIGCService service, AbstractModule fileStorageService) {
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    public FileLabel makeBarCodeA4Paper(AuthToken authToken, List<BarCode> list) {
        StringBuilder buf = new StringBuilder();
        for (BarCode barCode : list) {
            buf.append(barCode.data);
        }

        String prefix = Long.toString(Math.abs(Utils.hashStringMurmur(buf.toString())));
        String fileCode = FileUtils.makeFileCode(prefix, authToken.getDomain(),
                String.format("%d", list.size()));

        JSONObject fileLabelJson = this.fileStorageService.notify(new GetFile(authToken.getDomain(), fileCode));
        if (null != fileLabelJson) {
            // 文件已创建
            return new FileLabel(fileLabelJson);
        }

        List<File> fileList = new ArrayList<>();
        for (int i = 0; i < list.size(); ++i) {
            String filename = prefix + "_" + (i + 1);
            BarCode barCode = list.get(i);
            barCode.setLayer(BarCode.Small);
            File file = this.makeBarCodePaperFile(filename, barCode, PrintUtils.PaperA4Small, false);
            fileList.add(file);
        }

        File outputFile = new File(this.workingPath + prefix + ".pdf");
        if (outputFile.exists()) {
            outputFile.delete();
        }

        File path = new File(this.workingPath);
        // 合并到PDF
        this.mergeImageToPDF(path.getAbsolutePath() + "/" + prefix + "*.{png}", outputFile.getAbsolutePath());
        if (!outputFile.exists()) {
            for (File file : fileList) {
                file.delete();
            }

            Logger.e(this.getClass(), "#makeBarCodeA4Paper - Create PDF file error");
            return null;
        }

        for (File file : fileList) {
            file.delete();
        }

        // 保存到文件系统
        return this.saveTo(authToken, fileCode, outputFile);
    }

    public FileLabel makeBarCodePaper(AuthToken authToken, BarCode barCode, Size paperSize) {
        String fileCode = FileUtils.makeFileCode(barCode.data, authToken.getDomain(),
                String.format("%dx%d_%s_%s", barCode.width, barCode.height,
                        (null != barCode.header) ? barCode.header : "",
                        (null != barCode.footer) ? barCode.footer : ""));

        JSONObject fileLabelJson = this.fileStorageService.notify(new GetFile(authToken.getDomain(), fileCode));
        if (null != fileLabelJson) {
            // 文件已创建
            return new FileLabel(fileLabelJson);
        }

        File file = this.makeBarCodePaperFile(fileCode, barCode, paperSize, true);
        if (null == file) {
            Logger.w(this.getClass(), "#makeBarCodeA4Paper - Create file failed");
            return null;
        }

        return this.saveTo(authToken, fileCode, file);
    }

    private File makeBarCodePaperFile(String filename, BarCode barCode, Size paperSize, boolean landscape) {
        BufferedImage barCodeImage = CodeUtils.generateBarCode(barCode.data, barCode.width, barCode.height,
                barCode.header, barCode.footer, barCode.fontSize);
        if (null == barCodeImage) {
            Logger.e(this.getClass(), "#makeBarCodeA4PaperFile - Generate bar code image failed");
            return null;
        }

        int offsetX = paperSize.width - barCode.width;
        int offsetY = paperSize.height - barCode.height - 10;
        BufferedImage paper = PrintUtils.createPaper(paperSize, barCodeImage, offsetX, offsetY);

        if (!landscape) {
            int newWidth = paper.getHeight();
            int newHeight = paper.getWidth();
            // 旋转 90
            AffineTransform affineTransform = new AffineTransform();
            affineTransform.rotate(Math.toRadians(90), newWidth / 2, newHeight / 2);

            BufferedImage newImage = new BufferedImage(newWidth, newHeight, paper.getType());
            Graphics2D g2d = newImage.createGraphics();
            g2d.setTransform(affineTransform);
            g2d.drawImage(paper, (newWidth - paper.getWidth()) / 2, (newHeight - paper.getHeight()) / 2, null);
            g2d.dispose();

            paper = newImage;
        }

        /* 对码不能进行缩放
        if (scale != 1) {
            int width = (int) (paper.getWidth() * scale);
            int height = (int) (paper.getHeight() * scale);
            Image data = paper.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);

            BufferedImage newImage = new BufferedImage(width, height, paper.getType());
            Graphics2D g2d = newImage.createGraphics();
            g2d.drawImage(data, 0, 0, null);
            g2d.dispose();

            paper = newImage;
        }*/

        String ext = "png";
        File file = new File(this.workingPath, filename + "." + ext);
        if (file.exists()) {
            try {
                file.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            ImageIO.write(paper, ext, file);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#makeBarCodeA4PaperFile - Writer image data failed", e);
            return null;
        }

        return file;
    }

    public FileLabel makeBarCode(AuthToken authToken, BarCode barCode) {
        String fileCode = FileUtils.makeFileCode(barCode.data, authToken.getDomain(),
                String.format("%dx%d_%s_%s", barCode.width, barCode.height,
                        (null != barCode.header) ? barCode.header : "",
                        (null != barCode.footer) ? barCode.footer : ""));

        JSONObject fileLabelJson = this.fileStorageService.notify(new GetFile(authToken.getDomain(), fileCode));
        if (null != fileLabelJson) {
            // 文件已创建
            return new FileLabel(fileLabelJson);
        }

        BufferedImage image = CodeUtils.generateBarCode(barCode.data, barCode.width, barCode.height,
                barCode.header, barCode.footer, barCode.fontSize);
        if (null == image) {
            Logger.e(this.getClass(), "#makeBarCode - Generate bar code image failed");
            return null;
        }

        String ext = "jpg";
        File file = new File(this.workingPath, fileCode + "." + ext);
        if (file.exists()) {
            try {
                file.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            ImageIO.write(image, ext, file);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#makeBarCode - Writer image data failed", e);
            return null;
        }

        return this.saveTo(authToken, fileCode, file);
    }

    private FileLabel saveTo(AuthToken authToken, String fileCode, File file) {
        // 创建文件标签
        FileLabel fileLabel = FileUtils.makeFileLabel(authToken.getDomain(), fileCode, authToken.getContactId(), file);

        try {
            JSONObject fileJson = this.fileStorageService.notify(new SaveFile(file.getAbsolutePath(), fileLabel));
            return new FileLabel(fileJson);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#saveTo - File storage service save failed", e);
            return null;
        } finally {
            // 删除临时文件
            if (file.exists()) {
                try {
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean mergeImageToPDF(String source, String output) {
        // 创建命令
        // "-quality", "100"
        // Linux: /usr/bin/convert
        // Mac: /usr/local/bin/convert
        ProcessBuilder pb = new ProcessBuilder("/usr/bin/convert", source, output);
        pb.directory(new File(this.workingPath));

        Process process = null;
        BufferedReader stdError = null;
        int status = -1;

        try {
            String line = null;
            process = pb.start();
            stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = stdError.readLine()) != null) {
                if (line.length() > 0) {
                    Logger.w(this.getClass(), "#mergeImageToPDF - " + line);
                }
            }

            try {
                status = process.waitFor();
            } catch (InterruptedException e) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != stdError) {
                try {
                    stdError.close();
                } catch (IOException e) {
                }
            }

            if (null != process) {
                process.destroy();
            }
        }

        return (status == 1 || status == 0);
    }
}