/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc;

import cube.common.JSONable;
import cube.common.Language;
import cube.common.entity.GeneratingRecord;
import org.json.JSONObject;

/**
 * 策略节点。
 */
public abstract class StrategyNode implements JSONable {

    protected final String unitName;

    protected final Language language;

    protected GeneratingRecord result;

    protected StrategyNode next;

    public StrategyNode(String unitName, Language language) {
        this.unitName = unitName;
        this.language = language;
    }

    public String getUnitName() {
        return this.unitName;
    }

    public Language getLanguage() {
        return this.language;
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

    protected String filterFirstLine(String text) {
        int index = text.indexOf("\n");
        if (index > 0) {
            String result = text.substring(index);
            while (result.startsWith("\n")) {
                result = result.substring(1);
            }
            return result;
        }
        else {
            return text;
        }
    }

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
