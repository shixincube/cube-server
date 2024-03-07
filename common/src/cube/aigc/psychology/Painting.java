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

package cube.aigc.psychology;

import cell.util.log.Logger;
import cube.aigc.psychology.material.*;
import cube.aigc.psychology.material.house.*;
import cube.aigc.psychology.material.person.*;
import cube.aigc.psychology.material.tree.*;
import cube.common.JSONable;
import cube.vision.BoundingBox;
import cube.vision.Box;
import cube.vision.Size;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * 画面空间元素描述。
 */
public class Painting implements JSONable {

    private Attribute attribute;

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

    public Painting(Attribute attribute) {
        this.attribute = attribute;
    }

    public Painting(JSONObject json) {
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

        if (json.has("attribute")) {
            this.attribute = new Attribute(json.getJSONObject("attribute"));
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

        List<Thing> unknownList = new ArrayList<>();
        unknownList.addAll(thingList);

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
                    unknownList.remove(thing);
                    break;
                case Tree:
                case DeciduousTree:
                case DeadTree:
                case PineTree:
                case WillowTree:
                case CoconutTree:
                case Bamboo:
                    addTree((Tree) thing);
                    unknownList.remove(thing);
                    break;
                case Person:
                case Man:
                case Woman:
                case Boy:
                case Girl:
                    addPerson((Person) thing);
                    unknownList.remove(thing);
                    break;
                case Table:
                    addTable((Table) thing);
                    unknownList.remove(thing);
                    break;
                case Sun:
                    addSun((Sun) thing);
                    unknownList.remove(thing);
                    break;
                case Moon:
                    addMoon((Moon) thing);
                    unknownList.remove(thing);
                    break;
                case Star:
                    addStar((Star) thing);
                    unknownList.remove(thing);
                    break;
                case Mountain:
                    addMountain((Mountain) thing);
                    unknownList.remove(thing);
                    break;
                case Flower:
                    addFlower((Flower) thing);
                    unknownList.remove(thing);
                    break;
                case Grass:
                    addGrass((Grass) thing);
                    unknownList.remove(thing);
                    break;
                case Cloud:
                    addCloud((Cloud) thing);
                    unknownList.remove(thing);
                    break;
                case Bird:
                case Cat:
                case Dog:
                    addAnimal((Animal) thing);
                    unknownList.remove(thing);
                    break;
                default:
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
                case HouseCobbledPath:
                    buildHouse(thing);
                    unknownList.remove(thing);
                    break;
                case TreeTrunk:
                case TreeBranch:
                case TreeCanopy:
                case TreeRoot:
                case TreeFruit:
                case TreeHole:
                case TreeDrooping:
                    buildTree(thing);
                    unknownList.remove(thing);
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
                    unknownList.remove(thing);
                    break;
                default:
                    break;
            }
        }

        for (Thing thing : unknownList) {
            Logger.w(this.getClass(), "Unknown label: " + thing.label + " for building HTP");
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
                    if (house.box.detectCollision(thing.box)) {
                        house.addSidewall((Sidewall) thing);
                    }
                    break;
                case HouseRoof:
                case HouseRoofTextured:
                    if (house.box.detectCollision(thing.box)) {
                        house.setRoof((Roof) thing);
                    }
                    break;
                case HouseRoofSkylight:
                    if (house.box.detectCollision(thing.box)) {
                        house.addRoofSkylight((RoofSkylight) thing);
                    }
                    break;
                case HouseChimney:
                    if (house.box.detectCollision(thing.box)) {
                        house.addChimney((Chimney) thing);
                    }
                    break;
                case HouseDoor:
                case HouseDoorOpened:
                case HouseDoorLocked:
                    if (house.box.detectCollision(thing.box)) {
                        house.addDoor((Door) thing);
                    }
                    break;
                case HouseWindow:
                case HouseWindowOpened:
                    if (house.box.detectCollision(thing.box)) {
                        house.addWindow((Window) thing);
                    }
                    break;
                case HouseCurtain:
                case HouseCurtainOpened:
                    if (house.box.detectCollision(thing.box)) {
                        house.addCurtain((Curtain) thing);
                    }
                    break;
                case HouseWindowRailing:
                    if (house.box.detectCollision(thing.box)) {
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
                    Logger.w(this.getClass(), "#buildHouse - Unknown label: " + thing.getLabel().name + " for building house");
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
                list = this.sortByCollisionArea(this.treeList);
                ((Tree) list.getLast()).addTrunk((Trunk) thing);
                break;
            case TreeBranch:
                list = this.sortByCollisionArea(this.treeList);
                ((Tree) list.getLast()).addBranch((Branch) thing);
                break;
            case TreeCanopy:
                list = this.sortByCollisionArea(this.treeList);
                ((Tree) list.getLast()).addCanopy((Canopy) thing);
                break;
            case TreeRoot:
                list = this.sortByCollisionArea(this.treeList);
                ((Tree) list.getLast()).addRoot((Root) thing);
                break;
            case TreeFruit:
                list = this.sortByCollisionArea(this.treeList);
                ((Tree) list.getLast()).addFruit((Fruit) thing);
                break;
            case TreeHole:
                list = this.sortByCollisionArea(this.treeList);
                ((Tree) list.getLast()).addHole((Hole) thing);
                break;
            case TreeDrooping:
                list = this.sortByCollisionArea(this.treeList);
                ((Tree) list.getLast()).addDrooping((DroopingLeaves) thing);
                break;
            default:
                Logger.w(this.getClass(), "Unknown label: " + thing.getLabel().name + " for building tree");
                break;
        }
    }

    private void buildPerson(Thing thing) {
        if (null == this.personList || this.personList.isEmpty()) {
            // 没有识别出人，但是出现了人的元素，创建 Person
            Logger.i(this.getClass(), "#buildPerson - No person material");

            BoundingBox boundingBox = new BoundingBox(thing.boundingBox.x - 1, thing.boundingBox.y - 1,
                    thing.boundingBox.width + 2, thing.boundingBox.height + 2);
            Box box = new Box(thing.box.x0 - 1, thing.box.y0 - 1, thing.box.x1 + 1, thing.box.y1 + 1);
            Person person = new Person(boundingBox, box);

            this.personList = new ArrayList<>();
            this.personList.add(person);
        }

        LinkedList<Thing> list = this.sortByCollisionArea(this.personList);
        Person person = (Person) list.getLast();

        switch (thing.getLabel()) {
            case PersonHead:
                person.setHead((Head) thing);
                person.refreshBox(thing.boundingBox, thing.box);
                break;
            case PersonBraid:
                person.addBraid((Braid) thing);
                break;
            case PersonHair:
            case PersonStraightHair:
            case PersonShortHair:
            case PersonCurlyHair:
            case PersonStandingHair:
                person.addHair((Hair) thing);
                person.refreshBox(thing.boundingBox, thing.box);
                break;
            case PersonCap:
                person.setCap((Cap) thing);
                break;
            case PersonEye:
                person.addEye((Eye) thing);
                person.refreshBox(thing.boundingBox, thing.box);
                break;
            case PersonEyebrow:
                person.addEyebrow((Eyebrow) thing);
                person.refreshBox(thing.boundingBox, thing.box);
                break;
            case PersonNose:
                person.setNose((Nose) thing);
                person.refreshBox(thing.boundingBox, thing.box);
                break;
            case PersonEar:
                person.addEar((Ear) thing);
                person.refreshBox(thing.boundingBox, thing.box);
                break;
            case PersonMouth:
                person.setMouth((Mouth) thing);
                person.refreshBox(thing.boundingBox, thing.box);
                break;
            case PersonBody:
                person.setBody((Body) thing);
                person.refreshBox(thing.boundingBox, thing.box);
                break;
            case PersonArm:
                person.addArm((Arm) thing);
                person.refreshBox(thing.boundingBox, thing.box);
                break;
            case PersonPalm:
                person.addPalm((Palm) thing);
                person.refreshBox(thing.boundingBox, thing.box);
                break;
            case PersonLeg:
                person.addLeg((Leg) thing);
                person.refreshBox(thing.boundingBox, thing.box);
                break;
            case PersonFoot:
                person.addFoot((Foot) thing);
                person.refreshBox(thing.boundingBox, thing.box);
                break;
            case PersonSkirt:
                person.setSkirt((Skirt) thing);
                person.refreshBox(thing.boundingBox, thing.box);
                break;
            case PersonMask:
                person.setMask((Mask) thing);
                break;
            case PersonHairAccessories:
                person.addHairAccessory((HairAccessory) thing);
                break;
            case PersonItem:
                person.addItem((Item) thing);
                break;
            case PersonGlasses:
                person.setGlasses((Glasses) thing);
                break;
            default:
                Logger.w(this.getClass(), "Unknown label: " + thing.getLabel().name + " for building person");
                break;
        }
    }

    private LinkedList<Thing> sortByCollisionArea(List<? extends Thing> list) {
        LinkedList<Thing> result = new LinkedList<>(list);
        result.sort(new Comparator<Thing>() {
            @Override
            public int compare(Thing t1, Thing t2) {
                return t1.area - t2.area;
            }
        });
        return result;
    }

    private LinkedList<Thing> sortByCollisionArea(List<? extends Thing> list, BoundingBox box) {
        LinkedList<Thing> result = new LinkedList<>(list);
        result.sort(new Comparator<Thing>() {
            @Override
            public int compare(Thing t1, Thing t2) {
                int area1 = t1.boundingBox.calculateCollisionArea(box);
                int area2 = t2.boundingBox.calculateCollisionArea(box);
                return area1 - area2;
            }
        });
        return result;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public Size getCanvasSize() {
        return this.canvasSize;
    }

    /**
     * 是否是有效的绘画。
     *
     * @return
     */
    public boolean isValid() {
        return !(null == this.houseList && null == this.treeList && null == this.personList);
    }

    /**
     * 获取所有画面元素。
     *
     * @return
     */
    public List<Thing> getAllThings() {
        List<Thing> list = new ArrayList<>();
        if (null != this.houseList) {
            list.addAll(this.houseList);
        }
        if (null != this.treeList) {
            list.addAll(this.treeList);
        }
        if (null != this.personList) {
            list.addAll(this.personList);
        }
        if (null != this.sunList) {
            list.addAll(this.sunList);
        }
        if (null != this.moonList) {
            list.addAll(this.moonList);
        }
        if (null != this.starList) {
            list.addAll(this.starList);
        }
        if (null != this.mountainList) {
            list.addAll(this.mountainList);
        }
        if (null != this.flowerList) {
            list.addAll(this.flowerList);
        }
        if (null != this.grassList) {
            list.addAll(this.grassList);
        }
        if (null != this.cloudList) {
            list.addAll(this.cloudList);
        }
        if (null != this.animalList) {
            list.addAll(this.animalList);
        }
        return list;
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

    public boolean hasSun() {
        return (null != this.sunList);
    }

    public void addMoon(Moon moon) {
        if (null == this.moonList) {
            this.moonList = new ArrayList<>();
        }
        this.moonList.add(moon);
    }

    public boolean hasMoon() {
        return (null != this.moonList);
    }

    public void addStar(Star star) {
        if (null == this.starList) {
            this.starList = new ArrayList<>();
        }
        this.starList.add(star);
    }

    public boolean hasStar() {
        return (null != this.starList);
    }

    public int numStars() {
        return (null != this.starList) ? this.starList.size() : 0;
    }

    public void addMountain(Mountain mountain) {
        if (null == this.mountainList) {
            this.mountainList = new ArrayList<>();
        }
        this.mountainList.add(mountain);
    }

    public boolean hasMountain() {
        return (null != this.mountainList);
    }

    public int numMountains() {
        return (null != this.mountainList) ? this.mountainList.size() : 0;
    }

    public void addFlower(Flower flower) {
        if (null == this.flowerList) {
            this.flowerList = new ArrayList<>();
        }
        this.flowerList.add(flower);
    }

    public boolean hasFlower() {
        return (null != this.flowerList);
    }

    public int numFlowers() {
        return (null != this.flowerList) ? this.flowerList.size() : 0;
    }

    public void addGrass(Grass grass) {
        if (null == this.grassList) {
            this.grassList = new ArrayList<>();
        }
        this.grassList.add(grass);
    }

    public boolean hasGrass() {
        return (null != this.grassList);
    }

    public int numGrasses() {
        return (null != this.grassList) ? this.grassList.size() : 0;
    }

    public void addCloud(Cloud cloud) {
        if (null == this.cloudList) {
            this.cloudList = new ArrayList<>();
        }
        this.cloudList.add(cloud);
    }

    public boolean hasCloud() {
        return (null != this.cloudList);
    }

    public void addAnimal(Animal animal) {
        if (null == this.animalList) {
            this.animalList = new ArrayList<>();
        }
        this.animalList.add(animal);
    }

    public int numAnimals() {
        return (null != this.animalList) ? this.animalList.size() : 0;
    }

    public boolean hasAnimal() {
        return (null != this.animalList);
    }

    public boolean hasBird() {
        if (null == this.animalList) {
            return false;
        }

        for (Animal animal : this.animalList) {
            if (animal.getClasses() == Animal.Classes.Bird) {
                return true;
            }
        }

        return false;
    }

    public boolean hasDog() {
        if (null == this.animalList) {
            return false;
        }

        for (Animal animal : this.animalList) {
            if (animal.getClasses() == Animal.Classes.Dog) {
                return true;
            }
        }

        return false;
    }

    public boolean hasCat() {
        if (null == this.animalList) {
            return false;
        }

        for (Animal animal : this.animalList) {
            if (animal.getClasses() == Animal.Classes.Cat) {
                return true;
            }
        }

        return false;
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

        Collections.sort(bbdList, new AreaComparator());
        return bbdList;
    }

    private Thing getMaxArea(List<? extends Thing> list) {
        Thing maxAreaThing = null;
        int area = 0;
        for (Thing thing : list) {
            int ta = thing.area;
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


    private class AreaComparator implements Comparator<Thing> {

        public AreaComparator() {
        }

        @Override
        public int compare(Thing thing1, Thing thing2) {
            return thing1.area - thing2.area;
        }
    }
}
