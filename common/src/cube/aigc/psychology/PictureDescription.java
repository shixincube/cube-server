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

import cell.util.log.Logger;
import cube.aigc.psychology.composition.FrameStructure;
import cube.aigc.psychology.material.*;
import cube.aigc.psychology.material.house.*;
import cube.aigc.psychology.material.person.*;
import cube.aigc.psychology.material.tree.*;
import cube.common.JSONable;
import cube.vision.BoundingBox;
import cube.vision.Size;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * 画面空间元素描述。
 */
public class PictureDescription implements JSONable {

    private Size canvasSize;

    private List<House> houseList;

    private List<Tree> treeList;

    private List<Person> personList;

    private List<Table> tableList;

    private List<Sun> sunList;

    private List<Moon> moonList;

    private List<Star> starList;

    private List<Mountain> mountainList;

    private List<Flower> flowerList;

    private List<Grass> grassList;

    private List<Cloud> cloudList;

    private List<Animal> animalList;

    public PictureDescription(Size canvasSize) {
        this.canvasSize = canvasSize;
    }

    public PictureDescription(JSONObject json) {
        this.canvasSize = new Size(json.getJSONObject("size"));

        if (json.has("materials")) {
            this.parseMaterials(json.getJSONArray("materials"));
        }

        if (json.has("houses")) {
            this.houseList = new ArrayList<>();
            this.parseList(json.getJSONArray("houses"), this.houseList);
        }
        if (json.has("trees")) {
            this.treeList = new ArrayList<>();
            this.parseList(json.getJSONArray("trees"), this.treeList);
        }
        if (json.has("persons")) {
            this.personList = new ArrayList<>();
            this.parseList(json.getJSONArray("persons"), this.personList);
        }
        if (json.has("suns")) {
            this.sunList = new ArrayList<>();
            this.parseList(json.getJSONArray("suns"), this.sunList);
        }
        if (json.has("moons")) {
            this.moonList = new ArrayList<>();
            this.parseList(json.getJSONArray("moons"), this.moonList);
        }
        if (json.has("stars")) {
            this.starList = new ArrayList<>();
            this.parseList(json.getJSONArray("stars"), this.starList);
        }
        if (json.has("mountains")) {
            this.mountainList = new ArrayList<>();
            this.parseList(json.getJSONArray("mountains"), this.mountainList);
        }
        if (json.has("flowers")) {
            this.flowerList = new ArrayList<>();
            this.parseList(json.getJSONArray("flowers"), this.flowerList);
        }
        if (json.has("grasses")) {
            this.grassList = new ArrayList<>();
            this.parseList(json.getJSONArray("grasses"), this.grassList);
        }
        if (json.has("clouds")) {
            this.cloudList = new ArrayList<>();
            this.parseList(json.getJSONArray("clouds"), this.cloudList);
        }
        if (json.has("animals")) {
            this.animalList = new ArrayList<>();
            this.parseList(json.getJSONArray("animals"), this.animalList);
        }
    }

    private void parseList(JSONArray array, List targetList) {
        Classification classification = new Classification();

        for (int i = 0; i < array.length(); ++i) {
            Thing thing = classification.recognize(array.getJSONObject(i));
            if (null == thing) {
                continue;
            }

            targetList.add(thing);
        }
    }

    private void parseMaterials(JSONArray array) {
        Classification classification = new Classification();

        List<Thing> thingList = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            Thing thing = classification.recognize(array.getJSONObject(i));
            if (null == thing) {
                continue;
            }

            thingList.add(thing);
        }

        // 解析一级素材
        for (Thing thing : thingList) {
            Label label = thing.getLabel();
            switch (label) {
                case House:
                case Bungalow:
                case Villa:
                case Building:
                case Fairyland:
                case Temple:
                case Grave:
                    addHouse((House) thing);
                    break;
                case Tree:
                case DeciduousTree:
                case DeadTree:
                case PineTree:
                case WillowTree:
                case CoconutTree:
                case Bamboo:
                    addTree((Tree) thing);
                    break;
                case Person:
                case Man:
                case Woman:
                case Boy:
                case Girl:
                    addPerson((Person) thing);
                    break;
                case Table:
                    addTable((Table) thing);
                    break;
                case Sun:
                    addSun((Sun) thing);
                    break;
                case Moon:
                    addMoon((Moon) thing);
                    break;
                case Star:
                    addStar((Star) thing);
                    break;
                case Mountain:
                    addMountain((Mountain) thing);
                    break;
                case Flower:
                    addFlower((Flower) thing);
                    break;
                case Grass:
                    addGrass((Grass) thing);
                    break;
                case Cloud:
                    addCloud((Cloud) thing);
                    break;
                case Bird:
                case Cat:
                case Dog:
                    addAnimal((Animal) thing);
                    break;
                default:
                    Logger.w(this.getClass(), "Unknown label: " + label.name);
                    break;
            }
        }

        // 解析 HTP 内容
        for (Thing thing : thingList) {
            Label label = thing.getLabel();
            switch (label) {
                case HouseSidewall:
                case HouseRoof:
                case HouseRoofTextured:
                case HouseRoofSkylight:
                case HouseChimney:
                case HouseDoor:
                case HouseDoorOpened:
                case HouseDoorLocked:
                case HouseWindow:
                case HouseWindowOpened:
                case HouseCurtain:
                case HouseCurtainOpened:
                case HouseWindowRailing:
                case HouseSmoke:
                case HouseFence:
                case HousePath:
                case HouseCurvePath:
                    buildHouse(thing);
                    break;
                case TreeTrunk:
                case TreeBranch:
                case TreeCanopy:
                case TreeRoot:
                case TreeFruit:
                case TreeHole:
                case TreeDrooping:
                    buildTree(thing);
                    break;
                case PersonHead:
                case PersonHair:
                case PersonStraightHair:
                case PersonShortHair:
                case PersonCurlyHair:
                case PersonStandingHair:
                case PersonCap:
                case PersonEye:
                case PersonEyebrow:
                case PersonNose:
                case PersonEar:
                case PersonMouth:
                case PersonBody:
                case PersonArm:
                case PersonPalm:
                case PersonLeg:
                case PersonFoot:
                case PersonSkirt:
                case PersonMask:
                case PersonHairAccessories:
                case PersonItem:
                case PersonGlasses:
                    buildPerson(thing);
                    break;
                default:
                    Logger.w(this.getClass(), "Unknown label: " + label.name + " for building HTP");
                    break;
            }
        }
    }

    private void buildHouse(Thing thing) {
        if (null == this.houseList) {
            Logger.i(this.getClass(), "#buildHouse - No house material");
            return;
        }

        for (House house : this.houseList) {
            switch (thing.getLabel()) {
                case HouseSidewall:
                    if (house.getBoundingBox().detectCollision(thing.getBoundingBox())) {
                        house.addSidewall((Sidewall) thing);
                    }
                    break;
                case HouseRoof:
                case HouseRoofTextured:
                    if (house.getBoundingBox().detectCollision(thing.getBoundingBox())) {
                        house.setRoof((Roof) thing);
                    }
                    break;
                case HouseRoofSkylight:
                    if (house.getBoundingBox().detectCollision(thing.getBoundingBox())) {
                        house.addRoofSkylight((RoofSkylight) thing);
                    }
                    break;
                case HouseChimney:
                    if (house.getBoundingBox().detectCollision(thing.getBoundingBox())) {
                        house.addChimney((Chimney) thing);
                    }
                    break;
                case HouseDoor:
                case HouseDoorOpened:
                case HouseDoorLocked:
                    if (house.getBoundingBox().detectCollision(thing.getBoundingBox())) {
                        house.addDoor((Door) thing);
                    }
                    break;
                case HouseWindow:
                case HouseWindowOpened:
                    if (house.getBoundingBox().detectCollision(thing.getBoundingBox())) {
                        house.addWindow((Window) thing);
                    }
                    break;
                case HouseCurtain:
                case HouseCurtainOpened:
                    if (house.getBoundingBox().detectCollision(thing.getBoundingBox())) {
                        house.addCurtain((Curtain) thing);
                    }
                    break;
                case HouseWindowRailing:
                    if (house.getBoundingBox().detectCollision(thing.getBoundingBox())) {
                        house.addWindowRailing((WindowRailing) thing);
                    }
                    break;
                case HouseSmoke:
                    house.addSmoke((Smoke) thing);
                    break;
                case HouseFence:
                    house.addFence((Fence) thing);
                    break;
                case HousePath:
                case HouseCurvePath:
                case HouseCobbledPath:
                    house.addPath((Path) thing);
                    break;
                default:
                    Logger.w(this.getClass(), "Unknown label: " + thing.getLabel().name + " for building house");
                    break;
            }
        }
    }

    private void buildTree(Thing thing) {
        if (null == this.treeList || this.treeList.isEmpty()) {
            Logger.i(this.getClass(), "#buildTree - No tree material");
            return;
        }

        LinkedList<Thing> list = null;

        switch (thing.getLabel()) {
            case TreeTrunk:
                list = this.sortByCollisionArea(this.treeList, thing.getBoundingBox());
                ((Tree) list.getLast()).addTrunk((Trunk) thing);
                break;
            case TreeBranch:
                list = this.sortByCollisionArea(this.treeList, thing.getBoundingBox());
                ((Tree) list.getLast()).addBranch((Branch) thing);
                break;
            case TreeCanopy:
                list = this.sortByCollisionArea(this.treeList, thing.getBoundingBox());
                ((Tree) list.getLast()).addCanopy((Canopy) thing);
                break;
            case TreeRoot:
                list = this.sortByCollisionArea(this.treeList, thing.getBoundingBox());
                ((Tree) list.getLast()).addRoot((Root) thing);
                break;
            case TreeFruit:
                list = this.sortByCollisionArea(this.treeList, thing.getBoundingBox());
                ((Tree) list.getLast()).addFruit((Fruit) thing);
                break;
            case TreeHole:
                list = this.sortByCollisionArea(this.treeList, thing.getBoundingBox());
                ((Tree) list.getLast()).addHole((Hole) thing);
                break;
            case TreeDrooping:
                list = this.sortByCollisionArea(this.treeList, thing.getBoundingBox());
                ((Tree) list.getLast()).addDrooping((DroopingLeaves) thing);
                break;
            default:
                Logger.w(this.getClass(), "Unknown label: " + thing.getLabel().name + " for building tree");
                break;
        }
    }

    private void buildPerson(Thing thing) {
        if (null == this.personList || this.personList.isEmpty()) {
            Logger.i(this.getClass(), "#buildPerson - No person material");
            return;
        }

        LinkedList<Thing> list = null;

        switch (thing.getLabel()) {
            case PersonHead:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).setHead((Head) thing);
                break;
            case PersonBraid:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).addBraid((Braid) thing);
                break;
            case PersonHair:
            case PersonStraightHair:
            case PersonShortHair:
            case PersonCurlyHair:
            case PersonStandingHair:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).addHair((Hair) thing);
                break;
            case PersonCap:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).setCap((Cap) thing);
                break;
            case PersonEye:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).addEye((Eye) thing);
                break;
            case PersonEyebrow:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).addEyebrow((Eyebrow) thing);
                break;
            case PersonNose:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).setNose((Nose) thing);
                break;
            case PersonEar:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).addEar((Ear) thing);
                break;
            case PersonMouth:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).setMouth((Mouth) thing);
                break;
            case PersonBody:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).setBody((Body) thing);
                break;
            case PersonArm:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).addArm((Arm) thing);
                break;
            case PersonPalm:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).addPalm((Palm) thing);
                break;
            case PersonLeg:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).addLeg((Leg) thing);
                break;
            case PersonFoot:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).addFoot((Foot) thing);
                break;
            case PersonSkirt:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).setSkirt((Skirt) thing);
                break;
            case PersonMask:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).setMask((Mask) thing);
                break;
            case PersonHairAccessories:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).addHairAccessory((HairAccessory) thing);
                break;
            case PersonItem:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).addItem((Item) thing);
                break;
            case PersonGlasses:
                list = this.sortByCollisionArea(this.personList, thing.getBoundingBox());
                ((Person) list.getLast()).setGlasses((Glasses) thing);
                break;
            default:
                Logger.w(this.getClass(), "Unknown label: " + thing.getLabel().name + " for building person");
                break;
        }
    }

    private LinkedList<Thing> sortByCollisionArea(List<? extends Thing> list, BoundingBox box) {
        LinkedList<Thing> result = new LinkedList<>(list);
        Collections.sort(result, new Comparator<Thing>() {
            @Override
            public int compare(Thing t1, Thing t2) {
                int area1 = t1.getBoundingBox().calculateCollisionArea(box);
                int area2 = t2.getBoundingBox().calculateCollisionArea(box);
                return area1 - area2;
            }
        });
        return result;
    }

    public void addHouse(House house) {
        if (null == this.houseList) {
            this.houseList = new ArrayList<>();
        }
        this.houseList.add(house);
    }

    public House getHouse() {
        if (null == this.houseList || this.houseList.isEmpty()) {
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
        if (null == this.treeList) {
            this.treeList = new ArrayList<>();
        }
        this.treeList.add(tree);
    }

    public Tree getTree() {
        if (null == this.treeList || this.treeList.isEmpty()) {
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
        if (null == this.personList) {
            this.personList = new ArrayList<>();
        }
        this.personList.add(person);
    }

    public Person getPerson() {
        if (null == this.personList || this.personList.isEmpty()) {
            return null;
        }

        // 选择面积最大的
        Thing person = this.getMaxArea(this.personList);
        return (Person) person;
    }

    public List<Person> getPersons() {
        return this.personList;
    }

    public void addTable(Table table) {
        if (null == this.tableList) {
            this.tableList = new ArrayList<>();
        }
        this.tableList.add(table);
    }

    public void addSun(Sun sun) {
        if (null == this.sunList) {
            this.sunList = new ArrayList<>();
        }
        this.sunList.add(sun);
    }

    public void addMoon(Moon moon) {
        if (null == this.moonList) {
            this.moonList = new ArrayList<>();
        }
        this.moonList.add(moon);
    }

    public void addStar(Star star) {
        if (null == this.starList) {
            this.starList = new ArrayList<>();
        }
        this.starList.add(star);
    }

    public void addMountain(Mountain mountain) {
        if (null == this.mountainList) {
            this.mountainList = new ArrayList<>();
        }
        this.mountainList.add(mountain);
    }

    public void addFlower(Flower flower) {
        if (null == this.flowerList) {
            this.flowerList = new ArrayList<>();
        }
        this.flowerList.add(flower);
    }

    public void addGrass(Grass grass) {
        if (null == this.grassList) {
            this.grassList = new ArrayList<>();
        }
        this.grassList.add(grass);
    }

    public void addCloud(Cloud cloud) {
        if (null == this.cloudList) {
            this.cloudList = new ArrayList<>();
        }
        this.cloudList.add(cloud);
    }

    public void addAnimal(Animal animal) {
        if (null == this.animalList) {
            this.animalList = new ArrayList<>();
        }
        this.animalList.add(animal);
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

        if (null != this.houseList) {
            JSONArray houseArray = new JSONArray();
            for (House house : this.houseList) {
                houseArray.put(house.toJSON());
            }
            json.put("houses", houseArray);
        }

        if (null != this.treeList) {
            JSONArray treeArray = new JSONArray();
            for (Tree tree : this.treeList) {
                treeArray.put(tree.toJSON());
            }
            json.put("trees", treeArray);
        }

        if (null != this.personList) {
            JSONArray personArray = new JSONArray();
            for (Person person : this.personList) {
                personArray.put(person.toJSON());
            }
            json.put("persons", personArray);
        }

        if (null != this.sunList) {
            JSONArray sunArray = new JSONArray();
            for (Sun sun : this.sunList) {
                sunArray.put(sun.toJSON());
            }
            json.put("suns", sunArray);
        }

        if (null != this.moonList) {
            JSONArray moonArray = new JSONArray();
            for (Moon moon : this.moonList) {
                moonArray.put(moon.toJSON());
            }
            json.put("moons", moonArray);
        }

        if (null != this.starList) {
            JSONArray starArray = new JSONArray();
            for (Star star : this.starList) {
                starArray.put(star.toJSON());
            }
            json.put("stars", starArray);
        }

        if (null != this.mountainList) {
            JSONArray mountainArray = new JSONArray();
            for (Mountain mountain : this.mountainList) {
                mountainArray.put(mountain.toJSON());
            }
            json.put("mountains", mountainArray);
        }

        if (null != this.flowerList) {
            JSONArray flowerArray = new JSONArray();
            for (Flower flower : this.flowerList) {
                flowerArray.put(flower.toJSON());
            }
            json.put("flowers", flowerArray);
        }

        if (null != this.grassList) {
            JSONArray grassArray = new JSONArray();
            for (Grass grass : this.grassList) {
                grassArray.put(grass.toJSON());
            }
            json.put("grasses", grassArray);
        }

        if (null != this.cloudList) {
            JSONArray cloudArray = new JSONArray();
            for (Cloud cloud : this.cloudList) {
                cloudArray.put(cloud.toJSON());
            }
            json.put("clouds", cloudArray);
        }

        if (null != this.animalList) {
            JSONArray animalArray = new JSONArray();
            for (Animal animal : this.animalList) {
                animalArray.put(animal.toJSON());
            }
            json.put("animals", animalArray);
        }

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
