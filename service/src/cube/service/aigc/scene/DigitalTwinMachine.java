/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

/**
 * 数字孪生机。
 */
public class DigitalTwinMachine {

    private final static DigitalTwinMachine instance = new DigitalTwinMachine();

    private DigitalTwinMachine() {

    }

    public static DigitalTwinMachine getInstance() {
        return DigitalTwinMachine.instance;
    }
}
