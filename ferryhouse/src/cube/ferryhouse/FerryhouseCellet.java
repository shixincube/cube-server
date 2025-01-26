/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.ferryhouse;

import cube.core.AbstractCellet;

/**
 * Ferryhouse Cellet 单元。
 */
public class FerryhouseCellet extends AbstractCellet {

    public FerryhouseCellet() {
        super(Ferryhouse.NAME);
    }

    @Override
    public boolean install() {
        return true;
    }

    @Override
    public void uninstall() {
        Ferryhouse.getInstance().quit();
    }
}
