/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
