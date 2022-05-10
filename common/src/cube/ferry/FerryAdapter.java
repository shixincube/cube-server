/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2022 Cube Team.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.ferry;

import cell.adapter.CelletAdapter;
import cube.util.ConfigUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * 数据摆渡适配器。
 */
public class FerryAdapter extends CelletAdapter {

    public FerryAdapter(String host, int port) {
        super("FerryAdapter", host, port, true);
    }

    public void start() {
        this.setup();
    }

    public void stop() {
        this.teardown();
    }

    public static FerryAdapter create() {
        return FerryAdapter.create(null);
    }

    public static FerryAdapter create(File configFile) {
        String host = "0.0.0.0";
        int port = 6900;

        if (null != configFile) {
            try {
                Properties properties = ConfigUtils.readProperties(configFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FerryAdapter adapter = new FerryAdapter(host, port);
        return adapter;
    }
}
