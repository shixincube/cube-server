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

import cube.aigc.psychology.composition.FrameStructure;
import cube.aigc.psychology.material.House;
import cube.aigc.psychology.material.Person;
import cube.aigc.psychology.material.Thing;
import cube.aigc.psychology.material.Tree;
import cube.common.JSONable;
import cube.vision.BoundingBox;
import cube.vision.Size;
import org.json.JSONArray;
import org.json.JSONObject;
import sun.tools.jconsole.AboutDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 画面空间元素描述。
 */
public class PictureDescription implements JSONable {

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

    public PictureDescription(JSONObject json) {
        this.canvasSize = new Size(json.getJSONObject("size"));
        this.houseList = new ArrayList<>();
        this.treeList = new ArrayList<>();
        this.personList = new ArrayList<>();

        JSONArray array = json.getJSONArray("houses");
        for (int i = 0; i < array.length(); ++i) {
            this.houseList.add(new House(array.getJSONObject(i)));
        }

        array = json.getJSONArray("trees");
        for (int i = 0; i < array.length(); ++i) {
            this.treeList.add(new Tree(array.getJSONObject(i)));
        }

        array = json.getJSONArray("persons");
        for (int i = 0; i < array.length(); ++i) {
            this.personList.add(new Person(array.getJSONObject(i)));
        }
    }

    public void addHouse(House house) {
        this.houseList.add(house);
    }

    public House getHouse() {
        if (this.houseList.isEmpty()) {
            return null;
        }

        // 选择面积最大的
        Thing house = this.getMaxArea(this.houseList);
        return (House) house;
    }

    public List<House> getHouses() {
        return this.houseList;
    }

    public void addTree(Tree tree) {
        this.treeList.add(tree);
    }

    public Tree getTree() {
        if (this.treeList.isEmpty()) {
            return null;
        }

        // 选择面积最大的
        Thing tree = this.getMaxArea(this.treeList);
        return (Tree) tree;
    }

    public List<Tree> getTrees() {
        return this.treeList;
    }

    public void addPerson(Person person) {
        this.personList.add(person);
    }

    public Person getPerson() {
        if (this.personList.isEmpty()) {
            return null;
        }

        // 选择面积最大的
        Thing person = this.getMaxArea(this.personList);
        return (Person) person;
    }

    public List<Person> getPersons() {
        return this.personList;
    }

    public List<Thing> sortBySize() {
        List<Thing> bbdList = new ArrayList<>();
        if (!this.houseList.isEmpty()) {
            bbdList.addAll(this.houseList);
        }
        if (!this.treeList.isEmpty()) {
            bbdList.addAll(this.treeList);
        }
        if (!this.personList.isEmpty()) {
            bbdList.addAll(this.personList);
        }

        Collections.sort(bbdList, new BoundingBoxComparator());
        return bbdList;
    }

    public FrameStructureDescription calcHouseFrameStructure() {
        House house = this.getHouse();
        if (null == house) {
            return null;
        }
        return this.calcFrameStructure(house);
    }

    public FrameStructureDescription calcTreeFrameStructure() {
        Tree tree = this.getTree();
        if (null == tree) {
            return null;
        }
        return this.calcFrameStructure(tree);
    }

    public FrameStructureDescription calcPersonFrameStructure() {
        Person person = this.getPerson();
        if (null == person) {
            return null;
        }
        return this.calcFrameStructure(person);
    }

    private FrameStructureDescription calcFrameStructure(Thing thing) {
        int halfHeight = (int) (this.canvasSize.height * 0.5);
        int halfWidth = (int) (this.canvasSize.width * 0.5);

        BoundingBox topSpaceBox = new BoundingBox(0, 0,
                this.canvasSize.width, halfHeight);
        BoundingBox bottomSpaceBox = new BoundingBox(0, halfHeight,
                this.canvasSize.width, halfHeight);
        BoundingBox leftSpaceBox = new BoundingBox(0, 0,
                halfWidth, this.canvasSize.height);
        BoundingBox rightSpaceBox = new BoundingBox(halfWidth, 0,
                halfWidth, this.canvasSize.height);

        FrameStructureDescription fsd = new FrameStructureDescription();

        // 判读上下空间
        int topArea = topSpaceBox.calculateCollisionArea(thing.getBoundingBox());
        int bottomArea = bottomSpaceBox.calculateCollisionArea(thing.getBoundingBox());
        if (topArea > bottomArea) {
            fsd.addFrameStructure(FrameStructure.WholeTopSpace);
        }
        else {
            fsd.addFrameStructure(FrameStructure.WholeBottomSpace);
        }

        // 判断左右空间
        int leftArea = leftSpaceBox.calculateCollisionArea(thing.getBoundingBox());
        int rightArea = rightSpaceBox.calculateCollisionArea(thing.getBoundingBox());
        if (leftArea > rightArea) {
            fsd.addFrameStructure(FrameStructure.WholeLeftSpace);
        }
        else {
            fsd.addFrameStructure(FrameStructure.WholeRightSpace);
        }

        // 中间区域
        int paddingWidth = Math.round(((float) this.canvasSize.width) / 6.0f);
        int paddingHeight = Math.round(((float) this.canvasSize.height) / 6.0f);
        BoundingBox centerBox = new BoundingBox(paddingWidth, paddingHeight,
                this.canvasSize.width - paddingWidth * 2,
                this.canvasSize.height - paddingHeight * 2);
        halfHeight = (int) (centerBox.height * 0.5);
        halfWidth = (int) (centerBox.width * 0.5);
        BoundingBox topLeftBox = new BoundingBox(centerBox.x, centerBox.y, halfWidth, halfHeight);
        BoundingBox topRightBox = new BoundingBox(centerBox.x + halfWidth, centerBox.y,
                halfWidth, halfHeight);
        BoundingBox bottomLeftBox = new BoundingBox(centerBox.x, centerBox.y + halfHeight,
                halfWidth, halfHeight);
        BoundingBox bottomRightBox = new BoundingBox(centerBox.x + halfWidth, centerBox.y + halfHeight,
                halfWidth, halfHeight);
        int topLeftArea = topLeftBox.calculateCollisionArea(thing.getBoundingBox());
        int topRightArea = topRightBox.calculateCollisionArea(thing.getBoundingBox());
        int bottomLeftArea = bottomLeftBox.calculateCollisionArea(thing.getBoundingBox());
        int bottomRightArea = bottomRightBox.calculateCollisionArea(thing.getBoundingBox());

        List<AreaDesc> list = new ArrayList<>(4);
        list.add(new AreaDesc(topLeftArea, FrameStructure.CenterTopLeftSpace));
        list.add(new AreaDesc(topRightArea, FrameStructure.CenterTopRightSpace));
        list.add(new AreaDesc(bottomLeftArea, FrameStructure.CenterBottomLeftSpace));
        list.add(new AreaDesc(bottomRightArea, FrameStructure.CenterBottomRightSpace));

        // 面积从小到达排列
        Collections.sort(list, new Comparator<AreaDesc>() {
            @Override
            public int compare(AreaDesc ad1, AreaDesc ad2) {
                return ad1.area - ad2.area;
            }
        });

        fsd.addFrameStructure(list.get(list.size() - 1).structure);
        return fsd;
    }

    private Thing getMaxArea(List<? extends Thing> list) {
        Thing maxAreaThing = null;
        int area = 0;
        for (Thing thing : list) {
            int ta = thing.getBoundingBox().calculateArea();
            if (ta > area) {
                area = ta;
                maxAreaThing = thing;
            }
        }
        return maxAreaThing;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("size", this.canvasSize.toJSON());

        JSONArray houseArray = new JSONArray();
        for (House house : this.houseList) {
            houseArray.put(house.toJSON());
        }
        json.put("houses", houseArray);

        JSONArray treeArray = new JSONArray();
        for (Tree tree : this.treeList) {
            treeArray.put(tree.toJSON());
        }
        json.put("trees", treeArray);

        JSONArray personArray = new JSONArray();
        for (Person person : this.personList) {
            personArray.put(person.toJSON());
        }
        json.put("persons", personArray);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    public class FrameStructureDescription {

        private List<FrameStructure> frameStructures;

        private FrameStructureDescription() {
            this.frameStructures = new ArrayList<>();
        }

        protected void addFrameStructure(FrameStructure structure) {
            if (this.frameStructures.contains(structure)) {
                return;
            }
            this.frameStructures.add(structure);
        }

        public boolean isWholeTop() {
            return this.frameStructures.contains(FrameStructure.WholeTopSpace);
        }

        public boolean isWholeBottom() {
            return this.frameStructures.contains(FrameStructure.WholeBottomSpace);
        }
    }

    private class AreaDesc {
        protected int area;
        protected FrameStructure structure;

        protected AreaDesc(int area, FrameStructure structure) {
            this.area = area;
            this.structure = structure;
        }
    }

    private class BoundingBoxComparator implements Comparator<Thing> {

        public BoundingBoxComparator() {
        }

        @Override
        public int compare(Thing bbd1, Thing bbd2) {
            return bbd1.getBoundingBox().calculateArea() - bbd2.getBoundingBox().calculateArea();
        }
    }
}
