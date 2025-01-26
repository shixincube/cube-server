/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.test;

import cell.carpet.CellBoot;

/**
 *
 */
public class DispatcherStop {

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        CellBoot boot = new CellBoot();
        boot.setTag("dispatcher");
        boot.stop();
    }
}
