/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
