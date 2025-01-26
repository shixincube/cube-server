/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.test;

import cell.carpet.CellBoot;

/**
 *
 */
public class Start {

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        // 使用 Boot 启动 Cell 线程
        CellBoot boot = new CellBoot();
        boot.setTag("cube-service");
        boot.start();

        boot.join();

        System.out.println("Boot exit");
    }
}
