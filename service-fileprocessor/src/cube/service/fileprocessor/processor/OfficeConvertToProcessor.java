/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor.processor;

import cube.common.entity.FileResult;
import cube.file.operation.OfficeConvertToOperation;
import cube.util.FileType;
import cube.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Office 文档转换。
 */
public class OfficeConvertToProcessor extends LibreOffice {

    private static String NotFindSubstituteLandscapePath = "assets/CanNotFindFile_Landscape.pdf";
    private static String NotFindSubstitutePortraitPath = "assets/CanNotFindFile_Portrait.pdf";

    private File inputFile;

    public OfficeConvertToProcessor(Path workPath, File inputFile) {
        super(workPath);
        this.inputFile = inputFile;
    }

    @Override
    public void go(ProcessorContext context) {
        OfficeConvertToProcessorContext ctx = (OfficeConvertToProcessorContext) context;
        final String outputFormat = ctx.getOperation().getOutputFormat();

        boolean success = false;

        File outputFile = null;
        FileType fileType = FileUtils.extractFileExtensionType(this.inputFile.getName());

        if (fileType == FileType.DOC || fileType == FileType.DOCX
                || fileType == FileType.PPT || fileType == FileType.PPTX
                || fileType == FileType.XLS || fileType == FileType.XLSX) {
            if (OfficeConvertToOperation.OUTPUT_FORMAT_PDF.equals(outputFormat)) {
                // 执行命令
                success = this.callConvertTo(OfficeConvertToOperation.OUTPUT_FORMAT_PDF, this.inputFile.getName(), ctx);
            }
            else if (OfficeConvertToOperation.OUTPUT_FORMAT_PNG.equals(outputFormat)) {
                // 执行命令
                // 先转 PDF
                success = this.callConvertTo(OfficeConvertToOperation.OUTPUT_FORMAT_PDF, this.inputFile.getName(), ctx);
            }

            // 判断输出文件是否存在
            // 输出文件名
            String outputFilename = FileUtils.extractFileName(this.inputFile.getName())
                    + "." + OfficeConvertToOperation.OUTPUT_FORMAT_PDF;
            outputFile = new File(this.getWorkPath().toFile(), outputFilename);
            if (!outputFile.exists()) {
                // 文件不存在
                success = false;
            }
        }
        else if (fileType == FileType.PDF) {
            outputFile = this.inputFile;
            success = outputFile.exists();
        }
        else {
            // 设置结果
            ctx.setSuccessful(success);
            return;
        }

        // 没有成功生成文件
        if (!success) {
            File substitute = null;
            if (fileType == FileType.DOC || fileType == FileType.DOCX || fileType == FileType.PDF) {
                substitute =  new File(NotFindSubstitutePortraitPath);
            }
            else {
                substitute =  new File(NotFindSubstituteLandscapePath);
            }
            // 将替身拷贝到工作目录
            try {
                Files.copy(Paths.get(substitute.getAbsolutePath()), Paths.get(outputFile.getAbsolutePath()),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 再次判断文件是否存在
            outputFile = new File(outputFile.getAbsolutePath());
            success = outputFile.exists();
        }

        if (success) {
            if (OfficeConvertToOperation.OUTPUT_FORMAT_PNG.equals(outputFormat)) {
                // 将 PDF 转为图片
                List<File> files = ImageMagick.pdf2png(this.getWorkPath().toFile(), outputFile.getName());
                if (null == files || files.isEmpty()) {
                    success = false;
                }
                else {
                    for (File file : files) {
                        FileResult result = new FileResult(file);
                        ctx.addResult(result);
                    }
                }
            }
            else {
                FileResult result = new FileResult(outputFile);
                ctx.addResult(result);
            }
        }

        // 设置结果
        ctx.setSuccessful(success);
    }
}
