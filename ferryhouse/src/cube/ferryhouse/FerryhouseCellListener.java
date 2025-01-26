/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferryhouse;

import cell.api.Nucleus;
import cell.carpet.CellListener;
import cell.util.log.Logger;

/**
 * Cell 监听器。
 */
public class FerryhouseCellListener implements CellListener {

    public FerryhouseCellListener() {
        Logger.i(this.getClass(), "--------------------------------");
        Logger.i(this.getClass(), "Version " + Version.toVersionString());
        Logger.i(this.getClass(), "--------------------------------");
    }

    @Override
    public void cellPreinitialize(Nucleus nucleus) {

    }

    @Override
    public void cellInitialized(Nucleus nucleus) {
        (new Thread() {
            @Override
            public void run() {
                Ferryhouse.getInstance().config(nucleus);
            }
        }).start();
    }

    @Override
    public void cellDestroyed(Nucleus nucleus) {

    }
}
