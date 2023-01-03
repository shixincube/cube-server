/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
