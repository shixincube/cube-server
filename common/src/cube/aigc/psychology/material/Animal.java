/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material;

import org.json.JSONObject;

/**
 * 动物。
 */
@Deprecated
public class Animal extends Thing {

    public enum Classes {
        /**
         * 鸟
         */
        Bird,

        /**
         * 猫。
         */
        Cat,

        /**
         * 狗。
         */
        Dog
    }

    private Classes classes;

    public Animal(JSONObject json) {
        super(json);

        if (Label.Bird == this.paintingLabel) {
            this.classes = Classes.Bird;
        }
        else if (Label.Cat == this.paintingLabel) {
            this.classes = Classes.Cat;
        }
        else if (Label.Dog == this.paintingLabel) {
            this.classes = Classes.Dog;
        }
    }

    public Classes getClasses() {
        return this.classes;
    }

    public int numComponents() {
        return 0;
    }
}
