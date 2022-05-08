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

package cube.app.server;

import cube.client.Client;
import cube.common.entity.Contact;
import cube.util.ConfigUtils;

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
            this.client.destroy();
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
    }

    public Client getClient() {
        return this.client;
    }
}
