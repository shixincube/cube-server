/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.dispatcher;

import cell.api.Nucleus;
import cell.carpet.CellListener;
import cell.util.log.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Timer;

/**
 * 网关容器的监听器。
 */
public class DispatcherListener implements CellListener {

    private Timer timer;

    public DispatcherListener() {
    }

    @Override
    public void cellPreinitialize(Nucleus nucleus) {
        Performer performer = new Performer(nucleus);

        // 从配置文件加载配置数据
        this.config(performer);

        nucleus.setParameter("performer", performer);
    }

    @Override
    public void cellInitialized(Nucleus nucleus) {
        Performer performer = (Performer) nucleus.getParameter("performer");
        performer.start();

        this.timer = new Timer();
        this.timer.schedule(new Daemon(performer), 10L * 1000L, 30L * 1000L);
    }

    @Override
    public void cellDestroyed(Nucleus nucleus) {
        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }

        Performer performer = (Performer) nucleus.getParameter("performer");
        performer.stop();
    }

    private void config(Performer performer) {
        File file = new File("config/dispatcher.properties");
        if (!file.exists()) {
            file = new File("dispatcher.properties");
            if (!file.exists()) {
                Logger.w(this.getClass(), "Can NOT find config file");
                return;
            }
        }

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String prefix = "director.";
        for (int i = 1; i <= 10; ++i) {
            String keyAddress = prefix + (i) + ".address";
            if (!properties.containsKey(keyAddress)) {
                continue;
            }

            String keyPort = prefix + (i) + ".port";
            String keyCellets = prefix + (i) + ".cellets";
            String keyWeight = prefix + (i) + ".weight";

            String address = properties.getProperty(keyAddress);
            int port = Integer.parseInt(properties.getProperty(keyPort, "6000"));

            String celletString = properties.getProperty(keyCellets);
            String[] cellets = celletString.split(",");
            int weight = Integer.parseInt(properties.getProperty(keyWeight, "5"));

            // 设定范围
            Scope scope = new Scope();
            for (String cellet : cellets) {
                scope.cellets.add(cellet.trim());
            }
            scope.weight = weight;
            // 添加导演机节点
            performer.addDirector(address, port, scope);

            Logger.i(this.getClass(), "Add director point " + address + ":" + port + " #" + weight);
        }
    }
}
