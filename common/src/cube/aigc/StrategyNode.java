/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cube.common.JSONable;
import cube.common.entity.GeneratingRecord;
import org.json.JSONObject;

/**
 * 策略节点。
 */
public abstract class StrategyNode implements JSONable {

    protected String unitName;

    protected GeneratingRecord result;

    protected StrategyNode next;

    public StrategyNode(String unitName) {
        this.unitName = unitName;
    }

    public String getUnitName() {
        return this.unitName;
    }

    public GeneratingRecord getResult() {
        return this.result;
    }

    public StrategyNode link(StrategyNode next) {
        this.next = next;
        return next;
    }

    public StrategyNode next() {
        return this.next;
    }

    public abstract String perform(GeneratingRecord input);

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
