/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferryhouse.test;

import cell.carpet.CellBoot;

/**
 *
 */
public class Stop {

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        CellBoot boot = new CellBoot();
        boot.setTag("cube-ferryhouse");
        boot.stop();
    }
}
