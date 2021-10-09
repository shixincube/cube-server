/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.dispatcher;

import cell.api.Speakable;
import cell.core.net.Endpoint;

import java.util.HashMap;

/**
 * 导演机。
 */
public class Director {

    public Endpoint endpoint;

    public Scope scope;

    public Speakable speaker;

    protected HashMap<String, Section> sectionMap;

    public Director(Endpoint endpoint, Scope scope) {
        this.endpoint = endpoint;
        this.scope = scope;
        this.sectionMap = new HashMap();

        for (String celletName : scope.cellets) {
            this.sectionMap.put(celletName, new Section());
        }
    }

    public Section getSection(String celletName) {
        return this.sectionMap.get(celletName);
    }


    /**
     * 选择区间。
     */
    public class Section {

        public int begin = 0;

        public int end = 0;

        public int totalWeight = 0;

        public Section() {
        }

        public boolean contains(int value) {
            if (value >= this.begin && value <= this.end) {
                return true;
            }
            else {
                return false;
            }
        }
    }
}
