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

public class PictureDescription {

    private Size canvasSize;

    private List<House> houseList;

    private List<Tree> treeList;

    private List<Person> personList;

    public PictureDescription(Size canvasSize) {
        this.canvasSize = canvasSize;
        this.houseList = new ArrayList<>();
        this.treeList = new ArrayList<>();
        this.personList = new ArrayList<>();
    }

    public void addHouse(House house) {
        this.houseList.add(house);
    }

    public House getHouse() {
        return this.houseList.isEmpty() ? null : this.houseList.get(0);
    }

    public List<House> getHouses() {
        return this.houseList;
    }

    public void addTree(Tree tree) {
        this.treeList.add(tree);
    }

    public Tree getTree() {
        return this.treeList.isEmpty() ? null : this.treeList.get(0);
    }

    public List<Tree> getTrees() {
        return this.treeList;
    }

    public void addPerson(Person person) {
        this.personList.add(person);
    }

    public Person getPerson() {
        return this.personList.isEmpty() ? null : this.personList.get(0);
    }

    public List<Person> getPersons() {
        return this.personList;
    }

    public List<BoundingBoxDescription> sortBySize() {
        List<BoundingBoxDescription> bbdList = new ArrayList<>();
        if (!this.houseList.isEmpty()) {
            bbdList.add(new BoundingBoxDescription(this.houseList.get(0)));
        }
        if (!this.treeList.isEmpty()) {
            bbdList.add(new BoundingBoxDescription(this.treeList.get(0)));
        }
        if (!this.personList.isEmpty()) {
            bbdList.add(new BoundingBoxDescription(this.personList.get(0)));
        }

        Collections.sort(bbdList, new BoundingBoxComparator());
        return bbdList;
    }

    public void calcFrameStructure() {

    }

    public class BoundingBoxDescription {

        public Thing thing;
        public BoundingBox bbox;

        public BoundingBoxDescription(Thing thing) {
            this.thing = thing;
            this.bbox = thing.boundingBox;
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
