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
import cube.service.auth.AuthService;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件分享管理器。
 *
 * 一般访问 URI：
 * /sharing/{file_sharing_tag}
 */
public class FileSharingManager {

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

    public SharingTag createOrGetSharingTag(Contact contact, String fileCode) {
        AuthService authService = (AuthService) this.service.getKernel().getModule(AuthService.NAME);
        AuthDomain authDomain = authService.getAuthDomain(contact.getDomain().getName());

        SharingTag tag = this.service.getServiceStorage().readSharingTag(contact.getDomain().getName(),
                contact.getId(), fileCode, 0);
        if (null != tag) {
            tag.resetContact(contact);
            // 设置 URLs
            tag.setURLs(authDomain.httpEndpoint, authDomain.httpsEndpoint);
            return tag;
        }

        FileLabel fileLabel = this.service.getFile(contact.getDomain().getName(), fileCode);
        if (null == fileLabel) {
            Logger.w(this.getClass(), "#createOrGetSharingTag - Can NOT find file: " + fileCode);
            return null;
        }

        // 默认永久有效
        SharingTagConfig config = new SharingTagConfig(contact, fileLabel, 0, null);
        SharingTag sharingTag = new SharingTag(config);
        // 设置 URLs
        sharingTag.setURLs(authDomain.httpEndpoint, authDomain.httpsEndpoint);

        this.service.getServiceStorage().writeSharingTag(sharingTag);
        return sharingTag;
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

    public void addVisitTrace(VisitTrace trace) {
        String code = this.extractCode(trace.url);
        SharingCodeDomain codeDomain = getDomainByCode(code);

        if (null != codeDomain) {
            this.service.getServiceStorage().writeVisitTrace(codeDomain.domain, code, trace);
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