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

import cell.util.Utils;
import cell.util.log.Logger;
import com.sun.org.apache.xerces.internal.dom.ChildNode;
import cube.common.entity.*;
import cube.common.notice.MakeThumb;
import cube.common.notice.OfficeConvertTo;
import cube.common.notice.ProcessImage;
import cube.core.AbstractModule;
import cube.file.FileProcessResult;
import cube.file.operation.OfficeConvertToOperation;
import cube.file.operation.WatermarkOperation;
import cube.service.auth.AuthService;
import cube.service.contact.ContactManager;
import cube.util.FileUtils;
import cube.vision.Color;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    private ConcurrentHashMap<String, SharingTagTotal> sharingTagTotalMap;

    public FileSharingManager(FileStorageService service) {
        this.service = service;
    }

    public void start() {
        this.codeDomainMap = new ConcurrentHashMap<>();
        this.sharingTagTotalMap = new ConcurrentHashMap<>();
        this.authService = (AuthService) this.service.getKernel().getModule(AuthService.NAME);
    }

    public void stop() {
        this.codeDomainMap.clear();
    }

    /**
     * 创建分享标签。
     *
     * @param contact 联系人。
     * @param device 创建分享的设备。
     * @param fileCode 文件码。
     * @param duration 有效时长。
     * @param password 访问密码。
     * @param preview 是否生成预览图。
     * @param previewWatermark 预览图是否使用水印。
     * @param download 是否允许下载原文件。
     * @param traceDownload 是否追踪下载行为。
     * @return 返回创建的分享标签。
     */
    public SharingTag createSharingTag(Contact contact, Device device, String fileCode, long duration,
                                       String password, boolean preview, String previewWatermark,
                                       boolean download, boolean traceDownload) {
        AuthService authService = (AuthService) this.service.getKernel().getModule(AuthService.NAME);
        AuthDomain authDomain = authService.getAuthDomain(contact.getDomain().getName());

        FileLabel fileLabel = this.service.getFile(contact.getDomain().getName(), fileCode);
        if (null == fileLabel) {
            Logger.w(this.getClass(), "#createSharingTag - Can NOT find file: " + fileCode);
            return null;
        }

        // 创建配置信息
        SharingTagConfig config = new SharingTagConfig(contact, device, fileLabel, duration,
                password, preview, download, traceDownload);
        SharingTag sharingTag = new SharingTag(config);
        // 设置 URLs
        sharingTag.setURLs(authDomain.httpEndpoint, authDomain.httpsEndpoint);

        if (preview) {
            // 需要生成预览
            List<FileLabel> previewFiles = this.processFilePreview(sharingTag, contact, fileLabel, previewWatermark);
            if (null != previewFiles && !previewFiles.isEmpty()) {
                sharingTag.setPreviewList(previewFiles);
            }
            else {
                config.setPreview(false);
            }
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

    private List<FileLabel> processFilePreview(SharingTag sharingTag, Contact contact, FileLabel fileLabel,
                                               String watermark) {
        List<FileLabel> fileLabels = new ArrayList<>();
        String domainName = contact.getDomain().getName();

        if (FileUtils.isDocumentType(fileLabel.getFileType())) {
            // 文档转 PNG 图片
            OfficeConvertTo officeConvertTo = new OfficeConvertTo(domainName,
                    fileLabel.getFileCode(), OfficeConvertToOperation.OUTPUT_FORMAT_PNG);
            AbstractModule fileProcess = this.service.getKernel().getModule(this.fileProcessorModule);
            Object result = fileProcess.notify(officeConvertTo);
            if (result instanceof JSONObject) {
                FileProcessResult processResult = new FileProcessResult((JSONObject) result);
                if (processResult.success) {
                    for (FileResult fr : processResult.getResultList()) {
                        // 生成文件码
                        String fileName = sharingTag.getId().toString() + fr.fileName;
                        String fileCode = FileUtils.makeFileCode(contact.getId(), domainName, fileName);
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
            MakeThumb makeThumb = new MakeThumb(fileLabel.getDomain().getName(), fileLabel.getFileCode(), 80);
            AbstractModule fileProcess = this.service.getKernel().getModule(this.fileProcessorModule);
            Object result = fileProcess.notify(makeThumb);
            if (null != result) {
                FileThumbnail thumbnail = (FileThumbnail) result;
                fileLabels.add(thumbnail.getFileLabel());
            }
            else {
                Logger.w(this.getClass(), "#processFilePreview - Make image thumb failed: "
                        + fileLabel.getFileCode());
            }
        }

        // 覆盖水印
        if (!fileLabels.isEmpty() && null != watermark) {
            List<FileLabel> watermarkList = new ArrayList<>();
            // 依次创建水印
            for (FileLabel label : fileLabels) {
                // 文本约束
                TextConstraint constraint = new TextConstraint();
                constraint.pointSize = 26;
                constraint.color = new Color(128, 128, 128);
                // 图片加水印
                WatermarkOperation operation = new WatermarkOperation(watermark, constraint);
                ProcessImage processImage = new ProcessImage(label, operation);
                AbstractModule fileProcess = this.service.getKernel().getModule(this.fileProcessorModule);
                Object result = fileProcess.notify(processImage);
                if (null != result) {
                    JSONObject resultJson = (JSONObject) result;
                    FileProcessResult processResult = new FileProcessResult(resultJson);
                    if (processResult.hasResult()) {
                        // 水印文件
                        File file = processResult.getResultList().get(0).file;
                        // 创建文件码
                        String fileCode = FileUtils.makeFileCode(contact.getId(), domainName,
                                sharingTag.getId().toString() + file.getName());
                        // 创建标签
                        FileLabel watermarkFileLabel = FileUtils.makeFileLabel(domainName, fileCode, contact.getId(), file);
                        // 保存标签
                        watermarkFileLabel = this.service.saveFile(watermarkFileLabel, file);
                        if (null != watermarkFileLabel) {
                            watermarkList.add(watermarkFileLabel);
                            // 删除临时文件
                            file.delete();
                        }
                        else {
                            Logger.w(this.getClass(), "#processFilePreview - Save file to storage failed: "
                                    + domainName + " - " + contact.getId() + " - " + file.getName());
                        }

                        // 删除中间文件
                        this.service.deleteFile(domainName, label);
                    }
                    else {
                        Logger.w(this.getClass(), "#processFilePreview - Make watermark result is error: "
                                + domainName + " - " + contact.getId() + " - " + label.getFileName());
                        // 创建水印失败，使用原文件
                        watermarkList.add(label);
                    }
                }
                else {
                    Logger.w(this.getClass(), "#processFilePreview - Make watermark file failed: "
                            + domainName + " - " + contact.getId() + " - " + label.getFileName());
                    // 创建水印失败，使用原文件
                    watermarkList.add(label);
                }
            }

            fileLabels.clear();
            fileLabels.addAll(watermarkList);
        }

        return fileLabels;
    }

    /**
     * 取消分享标签。
     *
     * @param contact 指定操作的联系人。
     * @param code 指定分享码。
     * @return 返回已取消的分享码。
     */
    public SharingTag cancelSharingTag(Contact contact, String code) {
        // 取消标签
        List<FileLabel> previewList = this.service.getServiceStorage().cancelSharingTag(contact.getDomain().getName(), code);
        if (null != previewList) {
            // 删除预览图
            for (FileLabel fileLabel : previewList) {
                this.service.deleteFile(contact.getDomain().getName(), fileLabel);
            }
        }

        SharingTag sharingTag = this.service.getServiceStorage().readSharingTag(contact.getDomain().getName(), code);
        List<FileLabel> list = sharingTag.getPreviewList();
        if (null != list) {
            list.clear();
        }
        return sharingTag;
    }

    /**
     * 删除分享标签。
     *
     * @param contact 指定操作的联系人。
     * @param code 指定分享码。
     * @return 返回已删除的分享码。
     */
    public SharingTag deleteSharingTag(Contact contact, String code) {
        // 删除标签
        List<FileLabel> previewList = this.service.getServiceStorage().deleteSharingTag(contact.getDomain().getName(), code);
        if (null != previewList) {
            // 删除预览图
            for (FileLabel fileLabel : previewList) {
                this.service.deleteFile(contact.getDomain().getName(), fileLabel);
            }
        }

        SharingTag sharingTag = this.service.getServiceStorage().readSharingTag(contact.getDomain().getName(), code);
        List<FileLabel> list = sharingTag.getPreviewList();
        if (null != list) {
            list.clear();
        }
        return sharingTag;
    }

    /**
     * 获取指定的分享标签。
     *
     * @param code 指定分享码。
     * @param refresh 是否刷新 URL 地址信息。
     * @return 返回分享标签。
     */
    public SharingTag getSharingTag(String code, boolean refresh) {
        SharingCodeDomain codeDomain = this.getDomainByCode(code);
        if (null == codeDomain) {
            return null;
        }

        SharingTag sharingTag = this.service.getServiceStorage().readSharingTag(codeDomain.domain, code);
        if (null == sharingTag) {
            return null;
        }

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
     * 计算分享标签的数量。
     *
     * @param contact
     * @param valid
     * @return
     */
    public int countSharingTags(Contact contact, boolean valid) {
        String key = contact.getUniqueKey();
        SharingTagTotal total = this.sharingTagTotalMap.get(key);
        if (null == total) {
            int numValid = this.service.getServiceStorage().countSharingTag(contact.getDomain().getName(),
                    contact.getId(), true);
            int numInvalid = this.service.getServiceStorage().countSharingTag(contact.getDomain().getName(),
                    contact.getId(), false);
            total = new SharingTagTotal(numValid, numInvalid);
            this.sharingTagTotalMap.put(key, total);
        }

        return valid ? total.valid.get() : total.invalid.get();
    }

    private void refreshSharingTagTotal(Contact contact) {
        String key = contact.getUniqueKey();
        SharingTagTotal total = this.sharingTagTotalMap.get(key);
        if (null == total) {
            int numValid = this.service.getServiceStorage().countSharingTag(contact.getDomain().getName(),
                    contact.getId(), true);
            int numInvalid = this.service.getServiceStorage().countSharingTag(contact.getDomain().getName(),
                    contact.getId(), false);
            total = new SharingTagTotal(numValid, numInvalid);
            this.sharingTagTotalMap.put(key, total);
        }
        else {
            long now = System.currentTimeMillis();
            if (now - total.timestamp > 1000) {
                int numValid = this.service.getServiceStorage().countSharingTag(contact.getDomain().getName(),
                        contact.getId(), true);
                int numInvalid = this.service.getServiceStorage().countSharingTag(contact.getDomain().getName(),
                        contact.getId(), false);
                total.valid.set(numValid);
                total.invalid.set(numInvalid);
                total.timestamp = now;
            }
        }
    }

    /**
     * 列举分享标签。
     *
     * @param contact 标签的联系人。
     * @param valid 是否是在有效期内的分享。
     * @param beginIndex 数据起始索引。
     * @param endIndex 数据结束索引。
     * @param descending 是否降序。
     * @return 返回分享标签列表。
     */
    public List<SharingTag> listSharingTags(Contact contact, boolean valid,
                                            int beginIndex, int endIndex, boolean descending) {
        if (endIndex <= beginIndex) {
            return null;
        }

        List<SharingTag> list = this.service.getServiceStorage().listSharingTags(contact.getDomain().getName(),
                contact.getId(), valid, beginIndex, endIndex, descending);

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

        // 刷新数量数据
        this.refreshSharingTagTotal(contact);

        return list;
    }

    /**
     * 列举分享访问记录。
     *
     * @param contact 联系人。
     * @param sharingCode 分享码。
     * @param beginIndex 查询起始位置索引。
     * @param endIndex 查询结束位置索引。
     * @return 返回满足条件的查询结果列表。
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

        return this.service.getServiceStorage().listVisitTraces(contact.getDomain().getName(), contact.getId(),
                sharingCode, beginIndex, endIndex);
    }

    /**
     * 遍历全部下一级分享访问记录。
     *
     * @param parent 访问记录的父级联系人。
     * @param sharingCode 分享码。
     * @return 返回访问记录列表。
     */
    public List<VisitTrace> traverseVisitTrace(Contact parent, String sharingCode) {
        List<VisitTrace> result = this.service.getServiceStorage().queryVisitTraceByParent(
                parent.getDomain().getName(), sharingCode, parent.getId());
        return result;
    }

    /**
     * 计算分享记录访问数量。
     *
     * @param contact 记录所属联系人。
     * @param sharingCode 分享码。
     * @return 返回分享码的访问数量。
     */
    public int countSharingVisitTrace(Contact contact, String sharingCode) {
        return this.service.getServiceStorage().countVisitTraces(contact.getDomain().getName(), sharingCode);
    }

    /**
     * 记录访问痕迹。
     *
     * @param trace 访问痕迹。
     */
    public void traceVisit(VisitTrace trace) {
        String code = this.extractCode(trace.url);
        SharingCodeDomain codeDomain = getDomainByCode(code);

        if (null != codeDomain) {
            // 解析 Event Param
            if (null != trace.eventParam) {
                if (trace.eventParam.has("token")) {
                    String token = trace.eventParam.getString("token");
                    Contact contact = ContactManager.getInstance().getContact(token);
                    if (null != contact) {
                        trace.contactId = contact.getId();
                        trace.contactDomain = contact.getDomain().getName();
                    }
                }
                else if (trace.eventParam.has("id") && trace.eventParam.has("domain")) {
                    trace.contactId = trace.eventParam.getLong("id");
                    trace.contactDomain = trace.eventParam.getString("domain");
                }
            }

            // 写入记录
            this.service.getServiceStorage().writeVisitTrace(codeDomain.domain, code, trace);

            final FileStoragePluginContext context = new FileStoragePluginContext(trace);
            this.service.getExecutor().execute(() -> {
                FileStorageHook hook = ((FileStoragePluginSystem) this.service.getPluginSystem()).getTraceHook();
                hook.apply(context);
            });
        }
        else {
            Logger.w(this.getClass(), "#traceVisit - Can NOT find domain by code: " + code);
        }
    }

    /**
     * 计算指定分享码的追踪链。
     *
     * @param contact 联系人。
     * @param sharingCode 分享码。
     * @param traceDepth 遍历深度。
     * @return
     */
    public TransmissionChain calcTraceChain(Contact contact, String sharingCode, int traceDepth) {
        TransmissionChain chain = new TransmissionChain(sharingCode);

        // 浏览记录
        ChainNode viewNode = new ChainNode(Utils.generateSerialNumber(), contact.getDomain().getName(),
                TraceEvent.View);
        // 下载记录
        ChainNode extractNode = new ChainNode(Utils.generateSerialNumber(), contact.getDomain().getName(),
                TraceEvent.Extract);
        // 复制链接记录
        ChainNode shareNode = new ChainNode(Utils.generateSerialNumber(), contact.getDomain().getName(),
                TraceEvent.Share);

        List<VisitTrace> traceList = this.service.getServiceStorage().queryVisitTraceBySharer(
                contact.getDomain().getName(), sharingCode, contact.getId());

        for (VisitTrace visitTrace : traceList) {
            if (visitTrace.event.equals(TraceEvent.View)) {
                ChainNode child = this.findNode(viewNode.getChildren(), visitTrace);
                if (null == child) {
                    viewNode.addChild(new ChainNode(visitTrace));
                }
                else {
                    child.incrementTotal();
                }
            }
            else if (visitTrace.event.equals(TraceEvent.Extract)) {
                ChainNode child = this.findNode(extractNode.getChildren(), visitTrace);
                if (null == child) {
                    extractNode.addChild(new ChainNode(visitTrace));
                }
                else {
                    child.incrementTotal();
                }
            }
            else if (visitTrace.event.equals(TraceEvent.Share)) {
                ChainNode child = this.findNode(shareNode.getChildren(), visitTrace);
                if (null == child) {
                    shareNode.addChild(new ChainNode(visitTrace));
                }
                else {
                    child.incrementTotal();
                }
            }
        }

        chain.addNode(viewNode);
        chain.addNode(extractNode);
        chain.addNode(shareNode);

        // 计算二链
        ChainNode fissionNode = new ChainNode(Utils.generateSerialNumber(), contact.getDomain().getName(),
                "Fission");

        traceList = this.service.getServiceStorage().queryVisitTraceByParent(contact.getDomain().getName(),
                sharingCode, contact.getId());
        // 进行数据分类
        for (VisitTrace visitTrace : traceList) {
            /*
            // 浏览记录
            ChainNode viewNode = new ChainNode(Utils.generateSerialNumber(), contact.getDomain().getName(),
                    TraceEvent.View);
            // 下载记录
            ChainNode extractNode = new ChainNode(Utils.generateSerialNumber(), contact.getDomain().getName(),
                    TraceEvent.Extract);
            // 复制链接记录
            ChainNode shareNode = new ChainNode(Utils.generateSerialNumber(), contact.getDomain().getName(),
                    TraceEvent.Share);
             */
        }
        List<ChainNode> nodeList = this.compressNodes(traceList);
        fissionNode.addChildren(nodeList);

        chain.addNode(fissionNode);

        return chain;
    }

    public SharingReportor createSharingReportor() {
        return new SharingReportor(this.service.serviceStorage);
    }

    private List<ChainNode> compressNodes(List<VisitTrace> visitTraceList) {
        List<ChainNode> list = new ArrayList<>();
        for (VisitTrace visitTrace : visitTraceList) {
            ChainNode node = this.findNode(list, visitTrace);
            if (null == node) {
                node = new ChainNode(visitTrace);
                list.add(node);
            }
            else {
                node.incrementTotal();
            }
        }
        return list;
    }

    private ChainNode findNode(List<ChainNode> list, VisitTrace targetTrace) {
        for (ChainNode node : list) {
            VisitTrace trace = node.getVisitTrace();
            if (null == trace) {
                continue;
            }

            if (targetTrace.event.equals(trace.event) && targetTrace.address.equals(trace.address)) {
                return node;
            }
        }
        return null;
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


    /**
     * 分享码对应的域。
     */
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


    /**
     * 分享标签总数。
     */
    public class SharingTagTotal {

        public final AtomicInteger valid;

        public final AtomicInteger invalid;

        public long timestamp = System.currentTimeMillis();

        public SharingTagTotal(int valid, int invalid) {
            this.valid = new AtomicInteger(valid);
            this.invalid = new AtomicInteger(invalid);
        }
    }
}
