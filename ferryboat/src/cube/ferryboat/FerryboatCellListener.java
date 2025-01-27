/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferryboat;

import cell.api.Nucleus;
import cell.carpet.CellListener;
import cell.util.log.Logger;

/**
 * 监听器。
 */
public class FerryboatCellListener implements CellListener {

    public FerryboatCellListener() {
        Logger.i(this.getClass(), "--------------------------------");
        Logger.i(this.getClass(), "Version " + Version.toVersionString());
        Logger.i(this.getClass(), "--------------------------------");
    }

    @Override
    public void cellPreinitialize(Nucleus nucleus) {
    }

    @Override
    public void cellInitialized(Nucleus nucleus) {
        Logger.i(this.getClass(), "#cellInitialized");

        (new Thread() {
            @Override
            public void run() {
                Ferryboat.getInstance().start(nucleus);
            }
        }).start();
    }

    @Override
    public void cellDestroyed(Nucleus nucleus) {
        Logger.i(this.getClass(), "#cellDestroyed");

        Ferryboat.getInstance().stop();
    }
}
