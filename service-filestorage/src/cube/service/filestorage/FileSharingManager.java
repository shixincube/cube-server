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

package cube.service.filestorage;

import cell.util.log.Logger;
import cube.common.entity.*;
import cube.common.notice.OfficeConvertTo;
import cube.core.AbstractModule;
import cube.file.FileProcessResult;
import cube.file.operation.OfficeConvertToOperation;
import cube.service.auth.AuthService;
import cube.util.FileUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件分享管理器。
 *
 * 一般访问 URI：
 * /sharing/{file_sharing_tag}
 */
public class FileSharingManager {

    private final String fileProcessorModule = "FileProcessor";

    private FileStorageService service;

    private AuthService authService;

    private ConcurrentHashMap<String, SharingCodeDomain> codeDomainMap;

    public FileSharingManager(FileStorageService service) {
        this.service = service;
    }

    public void start() {
        this.codeDomainMap = new ConcurrentHashMap<>();
        this.authService = (AuthService) this.service.getKernel().getModule(AuthService.NAME);
    }

    public void stop() {
        this.codeDomainMap.clear();
    }

    /**
     * 创建分享标签。
     *
     * @param contact
     * @param device
     * @param fileCode
     * @param duration
     * @param password
     * @param preview
     * @param download
     * @return
     */
    public SharingTag createSharingTag(Contact contact, Device device, String fileCode, long duration,
                                       String password, boolean preview, boolean download) {
        AuthService authService = (AuthService) this.service.getKernel().getModule(AuthService.NAME);
        AuthDomain authDomain = authService.getAuthDomain(contact.getDomain().getName());

        FileLabel fileLabel = this.service.getFile(contact.getDomain().getName(), fileCode);
        if (null == fileLabel) {
            Logger.w(this.getClass(), "#createSharingTag - Can NOT find file: " + fileCode);
            return null;
        }

        // 创建配置信息
        SharingTagConfig config = new SharingTagConfig(contact, device, fileLabel, duration,
                password, preview, download);
        SharingTag sharingTag = new SharingTag(config);
        // 设置 URLs
        sharingTag.setURLs(authDomain.httpEndpoint, authDomain.httpsEndpoint);

        if (preview) {
            // 需要生成预览
            List<FileLabel> previewFiles = this.processFilePreview(contact, fileLabel);
            sharingTag.setPreviewList(previewFiles);
        }

        final FileStoragePluginContext context = new FileStoragePluginContext(sharingTag);
        this.service.getExecutor().execute(() -> {
            FileStorageHook hook = ((FileStoragePluginSystem) this.service.getPluginSystem()).getCreateSharingTagHook();
            hook.apply(context);
        });

        // 写入数据库
        this.service.getServiceStorage().writeSharingTag(sharingTag);

        return sharingTag;
    }

    private List<FileLabel> processFilePreview(Contact contact, FileLabel fileLabel) {
        List<FileLabel> fileLabels = new ArrayList<>();
        String domainName = contact.getDomain().getName();

        if (FileUtils.isDocumentType(fileLabel.getFileType())) {
            OfficeConvertTo officeConvertTo = new OfficeConvertTo(domainName,
                    fileLabel.getFileCode(), OfficeConvertToOperation.OUTPUT_FORMAT_PNG);
            AbstractModule fileProcess = this.service.getKernel().getModule(this.fileProcessorModule);
            Object result = fileProcess.notify(officeConvertTo);
            if (result instanceof JSONObject) {
                FileProcessResult processResult = new FileProcessResult((JSONObject) result);
                if (processResult.success) {
                    for (FileResult fr : processResult.getResultList()) {
                        // 生成文件码
                        String fileCode = FileUtils.makeFileCode(contact.getId(), domainName, fr.fileName);
                        // 生成文件标签
                        FileLabel label = FileUtils.makeFileLabel(domainName, fileCode, contact.getId(), fr.file);
                        // 将文件保存到存储
                        FileLabel newLabel = this.service.saveFile(label, fr.file);
                        fileLabels.add(newLabel);
                    }
                }
                else {
                    Logger.w(this.getClass(), "#processFilePreview - Make preview file failed: "
                            + fileLabel.getFileCode());
                }
            }
            else {
                Logger.w(this.getClass(), "#processFilePreview - File processor module error: "
                        + fileLabel.getFileCode());
            }
        }
        else if (FileUtils.isImageType(fileLabel.getFileType())) {

        }

        return fileLabels;
    }

    /**
     * 获取指定的分享标签。
     *
     * @param code
     * @param refresh
     * @return
     */
    public SharingTag getSharingTag(String code, boolean refresh) {
        SharingCodeDomain codeDomain = this.getDomainByCode(code);
        if (null == codeDomain) {
            return null;
        }

        SharingTag sharingTag = this.service.getServiceStorage().readSharingTag(codeDomain.domain, code);

        if (refresh) {
            AuthService authService = (AuthService) this.service.getKernel().getModule(AuthService.NAME);
            AuthDomain authDomain = authService.getAuthDomain(codeDomain.domain);
            // 设置标签的 URLs
            sharingTag.setURLs(authDomain.httpEndpoint, authDomain.httpsEndpoint);

            // 重置 URL 地址
            sharingTag.getConfig().getFileLabel().resetURLsAddress(authDomain.httpEndpoint.getHost(),
                    authDomain.httpsEndpoint.getHost());

            List<FileLabel> fileLabelList = sharingTag.getPreviewList();
            if (null != fileLabelList) {
                for (FileLabel fileLabel : fileLabelList) {
                    // 重置 URL 地址
                    fileLabel.resetURLsAddress(authDomain.httpEndpoint.getHost(),
                            authDomain.httpsEndpoint.getHost());
                }
            }
        }

        return sharingTag;
    }

    /**
     * 列举分享标签。
     *
     * @param contact
     * @return
     */

    /**
     * 列举分享标签。
     *
     * @param contact
     * @param valid 是否是在有效期内的分享。
     * @param beginIndex
     * @param endIndex
     * @return
     */
    public List<SharingTag> listSharingTags(Contact contact, boolean valid, int beginIndex, int endIndex) {
        if (endIndex <= beginIndex) {
            return null;
        }

        List<SharingTag> list = this.service.getServiceStorage().listSharingTags(contact.getDomain().getName(),
                contact.getId(), valid, beginIndex, endIndex);

        AuthDomain authDomain = this.authService.getAuthDomain(contact.getDomain().getName());

        for (SharingTag tag : list) {
            tag.setURLs(authDomain.httpEndpoint, authDomain.httpsEndpoint);

            // 重置 URL 地址
            tag.getConfig().getFileLabel().resetURLsAddress(authDomain.httpEndpoint.getHost(),
                    authDomain.httpsEndpoint.getHost());

            List<FileLabel> fileLabelList = tag.getPreviewList();
            if (null != fileLabelList) {
                for (FileLabel fileLabel : fileLabelList) {
                    // 重置 URL 地址
                    fileLabel.resetURLsAddress(authDomain.httpEndpoint.getHost(),
                            authDomain.httpsEndpoint.getHost());
                }
            }
        }

        return list;
    }

    /**
     * 列举分享访问记录。
     *
     * @param contact
     * @param sharingCode
     * @param beginIndex
     * @param endIndex
     * @return
     */
    public List<VisitTrace> listSharingVisitTrace(Contact contact, String sharingCode, int beginIndex, int endIndex) {
        if (endIndex <= beginIndex) {
            return null;
        }

        // 校验分享码
        long contactId = this.service.getServiceStorage().readSharingContactId(contact.getDomain().getName(),
                sharingCode);
        if (0 == contactId || contactId != contact.getId().longValue()) {
            return null;
        }

        return this.service.getServiceStorage().listVisitTraces(contact.getDomain().getName(),
                sharingCode, beginIndex, endIndex);
    }

    public void traceVisit(VisitTrace trace) {
        String code = this.extractCode(trace.url);
        SharingCodeDomain codeDomain = getDomainByCode(code);

        if (null != codeDomain) {
            // 写入记录
            this.service.getServiceStorage().writeVisitTrace(codeDomain.domain, code, trace);

            final FileStoragePluginContext context = new FileStoragePluginContext(trace);
            this.service.getExecutor().execute(() -> {
                FileStorageHook hook = ((FileStoragePluginSystem) this.service.getPluginSystem()).getTraceHook();
                hook.apply(context);
            });
        }
        else {
            Logger.w(this.getClass(), "#addVisitTrace - Can NOT find domain by code: " + code);
        }
    }

    private SharingCodeDomain getDomainByCode(String code) {
        SharingCodeDomain codeDomain = this.codeDomainMap.get(code);
        if (null != codeDomain) {
            return codeDomain;
        }

        String domain = this.service.getServiceStorage().querySharingCodeDomain(code);
        if (null == domain) {
            return null;
        }

        codeDomain = new SharingCodeDomain(code, domain);
        this.codeDomainMap.put(code, codeDomain);
        return codeDomain;
    }

    private String extractCode(String url) {
        int start = url.indexOf("sharing/");
        String param = url.substring(start + 8);
        int end = param.indexOf("/");
        if (end < 0) {
            end = param.indexOf("?");
        }

        if (end > 0) {
            return url.substring(start + 8, start + 8 + end);
        }
        else {
            return url.substring(start + 8);
        }
    }

    public class SharingCodeDomain {

        public final String code;

        public final String domain;

        public final long timestamp;

        public SharingCodeDomain(String code, String domain) {
            this.code = code;
            this.domain = domain;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
