/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
