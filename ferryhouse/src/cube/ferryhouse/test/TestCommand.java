/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferryhouse.test;

import cube.ferryhouse.command.DiskUsage;

public class TestCommand {

    public static void main(String[] args) {
        DiskUsage diskUsage = new DiskUsage();
        diskUsage.execute();
        int total = diskUsage.getTotal();
        int used = diskUsage.getUsed();
        int avail = diskUsage.getAvail();
        System.out.println("Total: " + total + diskUsage.getTotalUnit());
        System.out.println("Used: " + used + diskUsage.getUsedUnit());
        System.out.println("Avail: " + avail + diskUsage.getAvailUnit());
    }
}
