/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
