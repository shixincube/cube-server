/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

import cell.util.log.Logger;
import cube.aigc.psychology.algorithm.PaintingType;
import cube.aigc.psychology.composition.Line;
import cube.aigc.psychology.composition.Texture;
import cube.aigc.psychology.material.*;
import cube.aigc.psychology.material.house.*;
import cube.aigc.psychology.material.other.OtherSet;
import cube.aigc.psychology.material.person.*;
import cube.aigc.psychology.material.tree.*;
import cube.common.JSONable;
import cube.common.entity.FileLabel;
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

    public final long timestamp;

    public FileLabel fileLabel;

    private PaintingType type;

    private long elapsed;

    private Attribute attribute;

    private Size canvasSize;

    private List<House> houseList;

    private List<Tree> treeList;

    private List<Person> personList;

    private OtherSet otherSet;

    private List<Texture> quadrants;

    private Texture whole;

    private Line line;

    private JSONArray materials;

    public Painting(Attribute attribute) {
        this.timestamp = System.currentTimeMillis();
        this.type = PaintingType.HouseTreePerson;
        this.attribute = attribute;
        this.otherSet = new OtherSet();
        this.quadrants = new ArrayList<>();
    }

    public Painting(JSONObject json) {
        this.timestamp = System.currentTimeMillis();
        this.type = json.has("type") ?
                PaintingType.parse(json.getString("type")) : PaintingType.HouseTreePerson;
        this.elapsed = json.getLong("elapsed");
        this.canvasSize = new Size(json.getJSONObject("size"));

        this.otherSet = json.has("others") ? new OtherSet(json.getJSONArray("others")) : new OtherSet();

        if (json.has("materials")) {
            this.materials = json.getJSONArray("materials");
            this.parseMaterials(this.materials);
        }

        if (json.has("texture")) {
            this.quadrants = new ArrayList<>();
            this.parseQuadrants(json.getJSONObject("texture").getJSONArray("quadrants"));
            this.whole = new Texture(json.getJSONObject("texture").getJSONObject("whole"));
        }

        if (json.has("line")) {
            this.line = new Line(json.getJSONObject("line"));
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

        if (json.has("attribute")) {
            this.attribute = new Attribute(json.getJSONObject("attribute"));
        }
    }

    public List<Material> getMaterials() {
        List<Material> materialList = new ArrayList<>();
        for (int i = 0; i < this.materials.length(); ++i) {
            Material material = new Material(this.materials.getJSONObject(i));
            materialList.add(material);
        }
        return materialList;
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

    private void parseQuadrants(JSONArray array) {
        for (int i = 0; i < array.length(); ++i) {
            Texture texture = new Texture(array.getJSONObject(i));
            this.quadrants.add(texture);
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
                case StickMan:
                    addPerson((Person) thing);
                    unknownList.remove(thing);
                    break;
                default:
                    if (Label.isOther(label)) {
                        this.otherSet.add(thing);
                        unknownList.remove(thing);
                    }
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
                case PersonSideFace:
                case PersonBraid:
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
        if (null == this.houseList || this.houseList.isEmpty()) {
            Logger.i(this.getClass(), "#buildHouse - No house material, backward reasoning - " + thing.label);

            BoundingBox boundingBox = new BoundingBox(thing.boundingBox.x, thing.boundingBox.y,
                    thing.boundingBox.width, thing.boundingBox.height);
            Box box = new Box(thing.box.x0, thing.box.y0, thing.box.x1, thing.box.y1);
            House house = new House(boundingBox, box);

            this.houseList = new ArrayList<>();
            this.houseList.add(house);
        }

        LinkedList<Thing> list = this.sortByCollisionArea(this.houseList, thing.boundingBox);
        House house = (House) list.getLast();

        switch (thing.getLabel()) {
            case HouseSidewall:
                house.addSidewall((Sidewall) thing);
                if (house.isBackwardReasoning()) {
                    house.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case HouseRoof:
            case HouseRoofTextured:
                house.addRoof((Roof) thing);
                if (house.isBackwardReasoning()) {
                    house.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case HouseRoofSkylight:
                house.addRoofSkylight((RoofSkylight) thing);
                if (house.isBackwardReasoning()) {
                    house.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case HouseChimney:
                house.addChimney((Chimney) thing);
                break;
            case HouseDoor:
            case HouseDoorOpened:
            case HouseDoorLocked:
                house.addDoor((Door) thing);
                if (house.isBackwardReasoning()) {
                    house.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case HouseWindow:
            case HouseWindowOpened:
                house.addWindow((Window) thing);
                if (house.isBackwardReasoning()) {
                    house.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case HouseCurtain:
            case HouseCurtainOpened:
                house.addCurtain((Curtain) thing);
                if (house.isBackwardReasoning()) {
                    house.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case HouseWindowRailing:
                house.addWindowRailing((WindowRailing) thing);
                if (house.isBackwardReasoning()) {
                    house.refreshBox(thing.boundingBox, thing.box);
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

        house.refreshArea();
    }

    private void buildTree(Thing thing) {
        if (null == this.treeList || this.treeList.isEmpty()) {
            // 没有识别出树，但是出现了树的元素，创建 Tree
            Logger.i(this.getClass(), "#buildTree - No tree material, backward reasoning - " + thing.label);

            BoundingBox boundingBox = new BoundingBox(thing.boundingBox.x - 1, thing.boundingBox.y - 1,
                    thing.boundingBox.width + 2, thing.boundingBox.height + 2);
            Box box = new Box(thing.box.x0 - 1, thing.box.y0 - 1, thing.box.x1 + 1, thing.box.y1 + 1);
            Tree tree = new Tree(boundingBox, box);

            this.treeList = new ArrayList<>();
            this.treeList.add(tree);
        }

        LinkedList<Thing> list = this.sortByCollisionArea(this.treeList, thing.boundingBox);
        Tree tree = (Tree) list.getLast();

        switch (thing.getLabel()) {
            case TreeTrunk:
                tree.addTrunk((Trunk) thing);
                if (tree.isBackwardReasoning()) {
                    tree.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case TreeBranch:
                tree.addBranch((Branch) thing);
                if (tree.isBackwardReasoning()) {
                    tree.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case TreeCanopy:
                tree.addCanopy((Canopy) thing);
                if (tree.isBackwardReasoning()) {
                    tree.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case TreeRoot:
                tree.addRoot((Root) thing);
                if (tree.isBackwardReasoning()) {
                    tree.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case TreeFruit:
                tree.addFruit((Fruit) thing);
                if (tree.isBackwardReasoning()) {
                    tree.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case TreeHole:
                tree.addHole((Hole) thing);
                if (tree.isBackwardReasoning()) {
                    tree.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case TreeDrooping:
                tree.addDrooping((DroopingLeaves) thing);
                if (tree.isBackwardReasoning()) {
                    tree.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            default:
                Logger.w(this.getClass(), "Unknown label: " + thing.getLabel().name + " for building tree");
                break;
        }

        tree.refreshArea();
    }

    private void buildPerson(Thing thing) {
        if (null == this.personList || this.personList.isEmpty()) {
            // 没有识别出人，但是出现了人的元素，创建 Person
            Logger.i(this.getClass(), "#buildPerson - No person material, backward reasoning - " + thing.label);

            BoundingBox boundingBox = new BoundingBox(thing.boundingBox.x - 1, thing.boundingBox.y - 1,
                    thing.boundingBox.width + 2, thing.boundingBox.height + 2);
            Box box = new Box(thing.box.x0 - 1, thing.box.y0 - 1, thing.box.x1 + 1, thing.box.y1 + 1);
            Person person = new Person(boundingBox, box);

            this.personList = new ArrayList<>();
            this.personList.add(person);
        }

        LinkedList<Thing> list = this.sortByCollisionArea(this.personList, thing.boundingBox);
        Person person = (Person) list.getLast();

        switch (thing.getLabel()) {
            case PersonSideFace:
                person.setSideFace((Head) thing);
                break;
            case PersonHead:
                person.setHead((Head) thing);
                if (person.isBackwardReasoning()) {
                    person.refreshBox(thing.boundingBox, thing.box);
                }
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
                if (person.isBackwardReasoning()) {
                    person.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case PersonCap:
                person.setCap((Cap) thing);
                break;
            case PersonEye:
                person.addEye((Eye) thing);
                if (person.isBackwardReasoning()) {
                    person.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case PersonEyebrow:
                person.addEyebrow((Eyebrow) thing);
                if (person.isBackwardReasoning()) {
                    person.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case PersonNose:
                person.setNose((Nose) thing);
                if (person.isBackwardReasoning()) {
                    person.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case PersonEar:
                person.addEar((Ear) thing);
                if (person.isBackwardReasoning()) {
                    person.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case PersonMouth:
                person.setMouth((Mouth) thing);
                if (person.isBackwardReasoning()) {
                    person.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case PersonBody:
                person.setBody((Body) thing);
                if (person.isBackwardReasoning()) {
                    person.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case PersonArm:
                person.addArm((Arm) thing);
                if (person.isBackwardReasoning()) {
                    person.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case PersonPalm:
                person.addPalm((Palm) thing);
                if (person.isBackwardReasoning()) {
                    person.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case PersonLeg:
                person.addLeg((Leg) thing);
                if (person.isBackwardReasoning()) {
                    person.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case PersonFoot:
                person.addFoot((Foot) thing);
                if (person.isBackwardReasoning()) {
                    person.refreshBox(thing.boundingBox, thing.box);
                }
                break;
            case PersonSkirt:
                person.setSkirt((Skirt) thing);
                if (person.isBackwardReasoning()) {
                    person.refreshBox(thing.boundingBox, thing.box);
                }
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
                person.setGlasses((Eyeglass) thing);
                break;
            default:
                Logger.w(this.getClass(), "Unknown label: " + thing.getLabel().name + " for building person");
                break;
        }

        person.refreshArea();
    }

    /**
     * 从低到高。
     *
     * @param list
     * @return
     */
    private LinkedList<Thing> sortByArea(List<? extends Thing> list) {
        LinkedList<Thing> result = new LinkedList<>(list);
        result.sort(new Comparator<Thing>() {
            @Override
            public int compare(Thing t1, Thing t2) {
                return t1.area - t2.area;
            }
        });
        return result;
    }

    /**
     * 从低到高。
     *
     * @param list
     * @param box
     * @return
     */
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

    public List<Texture> getQuadrants() {
        return this.quadrants;
    }

    public Texture getWhole() {
        return this.whole;
    }

    public Line getLine() {
        return this.line;
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

        list.addAll(this.otherSet.getAll());
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

    public boolean hasHouse() {
        return (null != this.houseList && !this.houseList.isEmpty());
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

    public boolean hasTree() {
        return (null != this.treeList && !this.treeList.isEmpty());
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

    public boolean hasPerson() {
        return (null != this.personList && !this.personList.isEmpty());
    }

    public OtherSet getOther() {
        return this.otherSet;
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
        json.put("type", this.type.name);
        json.put("elapsed", this.elapsed);
        json.put("size", this.canvasSize.toJSON());

        if (null != this.attribute) {
            json.put("attribute", this.attribute.toJSON());
        }

        if (null != this.whole) {
            JSONObject textureJson = new JSONObject();
            JSONArray array = new JSONArray();
            for (Texture texture : this.quadrants) {
                array.put(texture.toJSON());
            }
            textureJson.put("quadrants", array);
            textureJson.put("whole", this.whole.toJSON());
            json.put("texture", textureJson);
        }

        if (null != this.line) {
            json.put("line", this.line.toJSON());
        }

        if (null != this.materials) {
            json.put("materials", this.materials);
        }
        else {
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

            json.put("others", this.otherSet.toJSONArray());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("attribute")) {
            json.remove("attribute");
        }
        return json;
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
