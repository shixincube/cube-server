/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.aigc.psychology;

import cube.vision.BoundingBox;
import cube.vision.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WholeDescription {

    private Size canvasSize;

    private House house;

    private BoundingBox houseBox;

    private Tree tree;

    private BoundingBox treeBox;

    private Person person;

    private BoundingBox personBox;

    public WholeDescription() {
    }

    public void setHouse(House house, BoundingBox box) {
        this.house = house;
        this.houseBox = box;
    }

    public void setTree(Tree tree, BoundingBox box) {
        this.tree = tree;
        this.treeBox = box;
    }

    public void setPerson(Person person, BoundingBox box) {
        this.person = person;
        this.personBox = box;
    }

    public List<BoundingBoxDescription> sortBySize() {
        List<BoundingBoxDescription> bbdList = new ArrayList<>();
        if (null != this.house) {
            bbdList.add(new BoundingBoxDescription(this.house, this.houseBox));
        }
        if (null != this.tree) {
            bbdList.add(new BoundingBoxDescription(this.tree, this.treeBox));
        }
        if (null != this.person) {
            bbdList.add(new BoundingBoxDescription(this.person, this.personBox));
        }

        Collections.sort(bbdList, new BoundingBoxComparator());
        return bbdList;
    }

    public class BoundingBoxDescription {

        public Thing thing;
        public BoundingBox bbox;

        public BoundingBoxDescription(Thing thing, BoundingBox bbox) {
            this.thing = thing;
            this.bbox = bbox;
        }
    }

    private class BoundingBoxComparator implements Comparator<BoundingBoxDescription> {

        public BoundingBoxComparator() {
        }

        @Override
        public int compare(BoundingBoxDescription bbd1, BoundingBoxDescription bbd2) {
            return bbd1.bbox.calculateArea() - bbd2.bbox.calculateArea();
        }
    }
}
