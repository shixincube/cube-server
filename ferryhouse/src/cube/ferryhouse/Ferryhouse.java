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

package cube.ferryhouse;

import cell.api.Nucleus;
import cell.api.Speakable;
import cell.api.TalkListener;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkError;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.ferry.FerryAction;
import cube.ferryhouse.tool.DomainTool;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Ferry 客户端。
 */
public class Ferryhouse implements TalkListener {

    public final static String FERRY = "Ferry";

    public final static String NAME = "Ferryhouse";

    private final static Ferryhouse instance = new Ferryhouse();

    private Nucleus nucleus;

    private String address;
    private int port;

    private String domain;

    private Ferryhouse() {
    }

    public static Ferryhouse getInstance() {
        return Ferryhouse.instance;
    }

    public void config(Nucleus nucleus) {
        this.nucleus = nucleus;

        Properties properties = this.loadConfig();
        if (null == properties) {
            Logger.e(this.getClass(), "#config - Can NOT find config file");
            return;
        }

        this.address = properties.getProperty("ferry.address");
        this.port = Integer.parseInt(properties.getProperty("ferry.port", "7900").trim());

        // 读取许可证
        try {
            JSONObject data = DomainTool.extractData(new File("config/licence"), "shixincube.com");
            if (null == data) {
                Logger.e(this.getClass(), "#config - Licence file error");
                System.exit(0);
                return;
            }

            this.domain = data.getString("domain");
        } catch (IOException e) {
            Logger.e(this.getClass(), "#config - Can NOT find licence file");
            System.exit(0);
            return;
        }

        this.nucleus.getTalkService().addListener(this);
        this.nucleus.getTalkService().call(this.address, this.port);
    }

    public void quit() {
        ActionDialect dialect = new ActionDialect(FerryAction.CheckOut.name);
        dialect.addParam("domain", this.domain);

        this.nucleus.getTalkService().speak(FERRY, dialect);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Properties loadConfig() {
        File file = new File("config/ferryhouse_dev.properties");
        if (!file.exists()) {
            file = new File("config/ferryhouse.properties");
        }

        try {
            return ConfigUtils.readProperties(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void onListened(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onListened(Speakable speakable, String cellet, PrimitiveInputStream primitiveInputStream) {

    }

    @Override
    public void onSpoke(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onAck(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onSpeakTimeout(Speakable speakable, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onContacted(Speakable speakable) {
        Logger.d(this.getClass(), "#onContacted");

        ActionDialect dialect = new ActionDialect(FerryAction.CheckIn.name);
        dialect.addParam("domain", this.domain);
        speakable.speak(FERRY, dialect);
    }

    @Override
    public void onQuitted(Speakable speakable) {
        Logger.d(this.getClass(), "#onQuitted");
    }

    @Override
    public void onFailed(Speakable speakable, TalkError talkError) {
        Logger.d(this.getClass(), "onFailed - " + talkError.getErrorCode());
    }
}
