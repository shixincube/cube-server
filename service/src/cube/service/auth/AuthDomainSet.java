/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.auth;

import cube.common.entity.AuthDomain;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 授权域集合。
 */
public final class AuthDomainSet {

    public final String domain;

    private ConcurrentHashMap<String, AuthDomain> authDomainMap;

    public AuthDomainSet(String domain) {
        this.domain = domain;
        this.authDomainMap = new ConcurrentHashMap<>();
    }

    public void addAuthDomain(AuthDomain authDomain) {
        this.authDomainMap.put(authDomain.appKey, authDomain);
    }

    public void removeAuthDomain(String appKey) {
        this.authDomainMap.remove(appKey);
    }

    public AuthDomain getAuthDomain(String appKey) {
        return this.authDomainMap.get(appKey);
    }
}
