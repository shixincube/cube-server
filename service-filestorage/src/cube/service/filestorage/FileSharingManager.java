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
import cube.file.operation.OfficeConvertToOperation;
import cube.service.auth.AuthService;
import cube.util.FileType;
import cube.util.FileUtils;
import org.json.JSONObject;

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

    private ConcurrentHashMap<String, SharingCodeDomain> codeDomainMap;

    public FileSharingManager(FileStorageService service) {
        this.service = service;
    }

    public void start() {
        this.codeDomainMap = new ConcurrentHashMap<>();
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

        // 默认永久有效
        SharingTagConfig config = new SharingTagConfig(contact, device, fileLabel, duration,
                password, preview, download);
        SharingTag sharingTag = new SharingTag(config);
        // 设置 URLs
        sharingTag.setURLs(authDomain.httpEndpoint, authDomain.httpsEndpoint);

        // 写入数据库
        this.service.getServiceStorage().writeSharingTag(sharingTag);

        final FileStoragePluginContext context = new FileStoragePluginContext(sharingTag);
        this.service.getExecutor().execute(() -> {
            FileStorageHook hook = ((FileStoragePluginSystem) this.service.getPluginSystem()).getCreateSharingTagHook();
            hook.apply(context);
        });

        return sharingTag;
    }

    private void processFilePreview(Contact contact, FileLabel fileLabel) {
        if (FileUtils.isDocumentType(fileLabel.getFileType())) {
            if (fileLabel.getFileType() == FileType.PDF) {

            }
            else {
                OfficeConvertTo officeConvertTo = new OfficeConvertTo(contact.getDomain().getName(),
                        fileLabel.getFileCode(), OfficeConvertToOperation.OUTPUT_FORMAT_PNG);
                AbstractModule fileProcess = this.service.getKernel().getModule(this.fileProcessorModule);
                Object result = fileProcess.notify(officeConvertTo);
                if (result instanceof JSONObject) {
                    //
                }
            }
        }
    }

    public SharingTag getSharingTag(String code, boolean urls) {
        SharingCodeDomain codeDomain = this.getDomainByCode(code);
        if (null == codeDomain) {
            return null;
        }

        SharingTag sharingTag = this.service.getServiceStorage().readSharingTag(codeDomain.domain, code);

        if (urls) {
            AuthService authService = (AuthService) this.service.getKernel().getModule(AuthService.NAME);
            AuthDomain authDomain = authService.getAuthDomain(codeDomain.domain);
            sharingTag.setURLs(authDomain.httpEndpoint, authDomain.httpsEndpoint);
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
     * @param inExpiry 是否是在有效期内的分享。
     * @param beginIndex
     * @param endIndex
     * @return
     */
    public List<SharingTag> listSharingTags(Contact contact, boolean inExpiry, int beginIndex, int endIndex) {
        if (endIndex <= beginIndex) {
            return null;
        }

        return this.service.getServiceStorage().listSharingTags(contact.getDomain().getName(),
                contact.getId(), inExpiry, beginIndex, endIndex);
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
        int end = url.substring(start + 8).indexOf("/");
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
