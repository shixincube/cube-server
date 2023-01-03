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

package cube.app.server.account;

/**
 * 令牌对象。
 */
public class Token {

    public final long id;

    public final long accountId;

    public final String domain;

    public final String code;

    public final String device;

    public final long creation;

    public final long expire;

    public long timestamp = System.currentTimeMillis();

    public Token(long accountId, String domain, String code, String device, long creation, long expire) {
        this.id = -1;
        this.accountId = accountId;
        this.domain = domain;
        this.code = code;
        this.device = device;
        this.creation = creation;
        this.expire = expire;
    }

    public Token(long id, long accountId, String domain, String code, String device, long creation, long expire) {
        this.id = id;
        this.accountId = accountId;
        this.domain = domain;
        this.code = code;
        this.device = device;
        this.creation = creation;
        this.expire = expire;
    }
}
