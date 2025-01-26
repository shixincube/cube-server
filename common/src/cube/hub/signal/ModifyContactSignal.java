/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub.signal;

/**
 * 修改联系人。
 */
public class ModifyContactSignal extends Signal {

    public final static String NAME = "ModifyContact";

    public ModifyContactSignal() {
        super(NAME);
    }
}
