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

import java.util.HashMap;
import java.util.Map;

/**
 * 在线联系人。
 */
public class OnlineAccount extends Account {

    private Map<String, Token> deviceTokenMap;

    private Map<String, String> tokenDeviceMap;

    public OnlineAccount(Account account, String device, Token token) {
        this(account.id, account.domain, account.account, account.phone, account.password, account.name, account.avatar, account.state);
        this.addDevice(device, token);
    }

    public OnlineAccount(long id, String domain, String account, String phone, String password, String name, String avatar, int state) {
        super(id, domain, account, phone, password, name, avatar, state);

        synchronized (this) {
            this.deviceTokenMap = new HashMap<>();
            this.tokenDeviceMap = new HashMap<>();
        }
    }

    public void addDevice(String device, Token token) {
        synchronized (this) {
            this.deviceTokenMap.put(device, token);
            this.tokenDeviceMap.put(token.code, device);
        }
    }

    public void removeDevice(String device) {
        synchronized (this) {
            Token token = this.deviceTokenMap.remove(device);
            if (null != token) {
                this.tokenDeviceMap.remove(token.code);
            }
        }
    }

    public int numDevices() {
        synchronized (this) {
            return this.deviceTokenMap.size();
        }
    }

    public String getDevice(String tokenCode) {
        synchronized (this) {
            return this.tokenDeviceMap.get(tokenCode);
        }
    }

    public Token getToken(String tokenCode) {
        synchronized (this) {
            String device = this.tokenDeviceMap.get(tokenCode);
            if (null == device) {
                return null;
            }

            return this.deviceTokenMap.get(device);
        }
    }
}
