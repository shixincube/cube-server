/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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
