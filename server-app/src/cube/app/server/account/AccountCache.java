/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server.account;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 账号缓存器。
 */
public class AccountCache {

    private final static long TIMEOUT = 10 * 60 * 1000;

    private AccountStorage storage;

    private Map<Long, Account> accountMap;

    public AccountCache(AccountStorage storage) {
        this.storage = storage;
        this.accountMap = new ConcurrentHashMap<>();
    }

    public Account getAccount(Long accountId) {
        Account account = this.accountMap.get(accountId);
        if (null != account) {
            account.timestamp = System.currentTimeMillis();
            return account;
        }

        account = this.storage.readAccount(accountId);
        if (null == account) {
            return null;
        }

        this.accountMap.put(accountId, account);
        return account;
    }

    public void onTick() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<Long, Account>> iter = this.accountMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, Account> entry = iter.next();
            if (now - entry.getValue().timestamp > TIMEOUT) {
                iter.remove();
            }
        }
    }
}
