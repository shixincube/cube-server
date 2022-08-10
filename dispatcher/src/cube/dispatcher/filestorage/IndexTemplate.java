/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.dispatcher.filestorage;

import cell.util.Base64;
import cube.common.entity.FileLabel;
import cube.common.entity.SharingTag;
import cube.dispatcher.Performer;
import cube.util.CodeUtils;
import cube.util.FileSize;
import cube.util.FileType;
import cube.util.FileUtils;
import cube.vision.Color;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/**
 * 首页模板。
 */
public class IndexTemplate {

    private static final String TITLE = "${title}";

    private static final String PREVIEW_TOGGLE = "${preview_toggle}";

    private static final String PREVIEW_PAGES = "${preview_pages}";

    private static final String MAIN_TOGGLE = "${main_toggle}";

    private static final String DOWNLOAD_TOGGLE = "${download_toggle}";

    private static final String FILE_TYPE = "${file_type}";

    private static final String FILE_NAME = "${file_name}";

    private static final String FILE_SIZE = "${file_size}";

    private static final String FILE_URI_PATH = "${file_uri_path}";

    private static final String SHARING_URL = "${sharing_url}";

    private static final String SCRIPT_CONTENT = "${script_content}";

    private static final String QRCODE_IMAGE_BASE64 = "${qrcode_image_base64}";

    private final static String CSS_STYLE_VALUE_NONE = "none";
    private final static String CSS_STYLE_VALUE_BLOCK = "block";
    private final static String CSS_STYLE_VALUE_INLINE_BLOCK = "inline-block";

    private SharingTag sharingTag;

    private boolean secure;

    private String pageTraceString;

    private Path qrCodeFilePath;

    public IndexTemplate(Path qrCodeFilePath, SharingTag sharingTag, boolean secure, String pageTraceString) {
        this.qrCodeFilePath = qrCodeFilePath;
        this.sharingTag = sharingTag;
        this.secure = secure;
        this.pageTraceString = pageTraceString;
    }

    public String matchLine(String input) {
        String line = input;

        if (line.contains(TITLE)) {
            line = line.replace(TITLE, sharingTag.getConfig().getFileLabel().getFileName());
        }
        else if (line.contains(PREVIEW_TOGGLE)) {
            line = line.replace(PREVIEW_TOGGLE,
                    sharingTag.getConfig().isPreview() ? CSS_STYLE_VALUE_BLOCK : CSS_STYLE_VALUE_NONE);
        }
        else if (line.contains(MAIN_TOGGLE)) {
            line = line.replace(MAIN_TOGGLE,
                    sharingTag.getConfig().isPreview() ? CSS_STYLE_VALUE_NONE : CSS_STYLE_VALUE_BLOCK);
        }
        else if (line.contains(DOWNLOAD_TOGGLE)) {
            line = line.replace(DOWNLOAD_TOGGLE,
                    sharingTag.getConfig().isDownloadAllowed() ? CSS_STYLE_VALUE_INLINE_BLOCK : CSS_STYLE_VALUE_NONE);
        }
        else if (line.contains(FILE_TYPE)) {
            line = line.replace(FILE_TYPE, parseFileType(sharingTag.getConfig().getFileLabel().getFileType()));
        }
        else if (line.contains(FILE_NAME)) {
            line = line.replace(FILE_NAME, sharingTag.getConfig().getFileLabel().getFileName());
        }
        else if (line.contains(FILE_SIZE)) {
            FileSize size = FileUtils.scaleFileSize(sharingTag.getConfig().getFileLabel().getFileSize());
            line = line.replace(FILE_SIZE, size.toString());
        }
        else if (line.contains(FILE_URI_PATH)) {
            try {
                String filename = URLEncoder.encode(sharingTag.getConfig().getFileLabel().getFileName(), "UTF-8");
                String uri = FileHandler.PATH + filename + "?sc=" + sharingTag.getCode();
                line = line.replace(FILE_URI_PATH, uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (line.contains(QRCODE_IMAGE_BASE64)) {
            String base64 = this.getQRCodeImageBase64();
            line = line.replace(QRCODE_IMAGE_BASE64, (null == base64) ? "" : base64);
        }
        else if (line.contains(SHARING_URL)) {
            if (null != this.pageTraceString) {
                line = line.replace(SHARING_URL, this.secure ? SharingTag.makeURLs(sharingTag, this.pageTraceString)[1]
                        : SharingTag.makeURLs(sharingTag, this.pageTraceString)[0]);
            }
            else {
                line = line.replace(SHARING_URL, this.secure ? sharingTag.getHttpsURL() : sharingTag.getHttpURL());
            }
        }
        else if (line.contains(SCRIPT_CONTENT)) {
            line = line.replace(SCRIPT_CONTENT, makeScriptContent());
        }
        else if (line.contains(PREVIEW_PAGES)) {
            List<FileLabel> list = sharingTag.getPreviewList();
            if (sharingTag.getConfig().isPreview() && null != list) {
                StringBuilder pages = new StringBuilder("\n");
                for (FileLabel fileLabel : list) {
                    pages.append("<div class=\"page\">");
                    pages.append("<img src=\"");
                    pages.append(getFileURL(sharingTag.getConfig().getDomain().getName(), fileLabel));
                    pages.append("\" />");
                    pages.append("</div>\n");
                }
                line = pages.toString();
            }
            else {
                line = "";
            }
        }

        return line;
    }

    private String makeScriptContent() {
        StringBuilder buf = new StringBuilder();
        buf.append("var sharingTag=");
        buf.append(this.sharingTag.toCompactJSON().toString());
        buf.append(";");
        buf.append("var appLoginURL='");
        buf.append(Performer.APP_LOGIN_URL);
        buf.append("';");
        return buf.toString();
    }

    private String getFileURL(String domain, FileLabel fileLabel) {
        String domainParam = Base64.encodeBytes(domain.getBytes(StandardCharsets.UTF_8));
        try {
            domainParam = URLEncoder.encode(domainParam, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        StringBuilder buf = new StringBuilder(this.secure ? fileLabel.getFileSecureURL() : fileLabel.getFileURL());
//        buf.append("&type=" + fileLabel.getFileType().getPreferredExtension());
        buf.append("&domain=");
        buf.append(domainParam);
        return buf.toString();
    }

    private String parseFileType(FileType fileType) {
        if (FileUtils.isImageType(fileType)) {
            return "image";
        }
        else if (FileUtils.isAudioType(fileType)) {
            return "audio";
        }
        else if (FileUtils.isVideoType(fileType)) {
            return "video";
        }

        switch (fileType) {
            case AE:
            case AI:
            case APK:
            case DMG:
            case PDF:
            case PSD:
            case RAR:
            case RP:
            case TXT:
            case XMAP:
                return fileType.getPreferredExtension();
            case DOC:
            case DOCX:
                return "doc";
            case EXE:
            case DLL:
                return "exe";
            case PPT:
            case PPTX:
                return "ppt";
            case LOG:
                return "txt";
            case XLS:
            case XLSX:
                return "xls";
            default:
                return "unknown";
        }
    }

    private String getQRCodeImageBase64() {
        String base64 = null;
        File file = new File(this.qrCodeFilePath.toFile(), this.sharingTag.getCode() + ".base64");
        if (!file.exists()) {
            // 创建二维码图片
            File qrCodeFile = new File(this.qrCodeFilePath.toFile(), this.sharingTag.getCode() + ".png");
            CodeUtils.generateQRCode(qrCodeFile,
                    this.secure ? this.sharingTag.getHttpsURL() : this.sharingTag.getHttpURL(),
                    300, 300, new Color(0, 0, 0));

            if (qrCodeFile.exists()) {
                byte[] data = null;
                try {
                    data = Files.readAllBytes(Paths.get(qrCodeFile.getAbsolutePath()));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 删除图片文件
                qrCodeFile.delete();

                // 将文件数据编码为 Base64
                base64 = Base64.encodeBytes(data);
                try {
                    Files.write(Paths.get(file.getAbsolutePath()),
                            base64.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (null == base64) {
            try {
                byte[] data = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                base64 = new String(data, StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return base64;
    }
}
