/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.app.server;

import cube.client.Client;
import cube.common.entity.Contact;
import cube.util.ConfigUtils;
import cube.util.HttpClientFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * 管理器。
 */
public class Manager {

    private final static Manager instance = new Manager();

    private Client client;

    private Manager() {
    }

    public static Manager getInstance() {
        return Manager.instance;
    }

    public boolean start(Properties properties) {
        String address = properties.getProperty("client.address");
        String account = properties.getProperty("client.account");
        String password = properties.getProperty("client.password");
        String id = properties.getProperty("client.pretender.id", "11000");
        String domain = properties.getProperty("client.pretender.domain");

        this.client = new Client(address, account, password);
        if (!this.client.waitReady()) {
            if (null != this.client) {
                this.client.destroy();
            }
            return false;
        }

        Contact contact = new Contact(Long.parseLong(id), domain);
        this.client.prepare(contact, false);

        return true;
    }

    public void stop() {
        if (null != this.client) {
            this.client.destroy();
            this.client = null;
        }

        HttpClientFactory.getInstance().close();
    }

    public Client getClient() {
        return this.client;
    }
}
