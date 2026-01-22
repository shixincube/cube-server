/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material.other;

import cell.util.log.Logger;
import cube.aigc.psychology.material.Classification;
import cube.aigc.psychology.material.Label;
import cube.aigc.psychology.material.Thing;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * 其他绘画元素集合。
 */
public class DrawingSet {

    private List<Table> tableList;

    private List<Bed> bedList;

    private List<Sun> sunList;

    private List<Moon> moonList;

    private List<Star> starList;

    private List<Mountain> mountainList;

    private List<Flower> flowerList;

    private List<Grass> grassList;

    private List<Sea> seaList;

    private List<Pool> poolList;

    private List<Sunflower> sunflowerList;

    private List<Mushroom> mushroomList;

    private List<Lotus> lotusList;

    private List<PlumFlower> plumFlowerList;

    private List<Rose> roseList;

    private List<Cloud> cloudList;

    private List<Rain> rainList;

    private List<Rainbow> rainbowList;

    private List<Torch> torchList;

    private List<Bonfire> bonfireList;

    private List<Bird> birdList;

    private List<Cat> catList;

    private List<Dog> dogList;

    private List<Cow> cowList;

    private List<Sheep> sheepList;

    private List<Pig> pigList;

    private List<Fish> fishList;

    private List<Rabbit> rabbitList;

    private List<Horse> horseList;

    private List<Hawk> hawkList;

    private List<Rat> ratList;

    private List<Butterfly> butterflyList;

    private List<Tiger> tigerList;

    private List<Hedgehog> hedgehogList;

    private List<Snake> snakeList;

    private List<Dragon> dragonList;

    private List<Watch> watchList;

    private List<Clock> clockList;

    private List<MusicalNotation> musicalNotationList;

    private List<TV> tvList;

    private List<Pole> poleList;

    private List<Tower> towerList;

    private List<Lighthouse> lighthouseList;

    private List<Gun> gunList;

    private List<Sword> swordList;

    private List<Knife> knifeList;

    private List<Shield> shieldList;

    private List<Sandglass> sandglassList;

    private List<Kite> kiteList;

    private List<Umbrella> umbrellaList;

    private List<Windmill> windmillList;

    private List<Flag> flagList;

    private List<Bridge> bridgeList;

    private List<Crossroads> crossroadsList;

    private List<Ladder> ladderList;

    private List<Stairs> stairsList;

    private List<Birdcage> birdcageList;

    private List<Car> carList;

    private List<Boat> boatList;

    private List<Airplane> airplaneList;

    private List<Bike> bikeList;

    private List<Skull> skullList;

    private List<Glasses> glassesList;

    private List<Swing> swingList;

    public DrawingSet() {
    }

    public DrawingSet(JSONArray array) {
        Classification classification = new Classification();
        for (int i = 0; i < array.length(); ++i) {
            Thing thing = classification.recognize(array.getJSONObject(i));
            if (null == thing) {
                Logger.w(this.getClass(), "Unknown label: " + array.getJSONObject(i).toString(4));
                continue;
            }

            this.add(thing);
        }
    }

    public void add(Thing thing) {
        if (thing instanceof Table) {
            if (null == this.tableList) {
                this.tableList = new ArrayList<>();
            }
            this.tableList.add((Table) thing);
        }
        else if (thing instanceof Bed) {
            if (null == this.bedList) {
                this.bedList = new ArrayList<>();
            }
            this.bedList.add((Bed) thing);
        }
        else if (thing instanceof Sun) {
            if (null == this.sunList) {
                this.sunList = new ArrayList<>();
            }
            this.sunList.add((Sun) thing);
        }
        else if (thing instanceof Moon) {
            if (null == this.moonList) {
                this.moonList = new ArrayList<>();
            }
            this.moonList.add((Moon) thing);
        }
        else if (thing instanceof Star) {
            if (null == this.starList) {
                this.starList = new ArrayList<>();
            }
            this.starList.add((Star) thing);
        }
        else if (thing instanceof Mountain) {
            if (null == this.mountainList) {
                this.mountainList = new ArrayList<>();
            }
            this.mountainList.add((Mountain) thing);
        }
        else if (thing instanceof Flower) {
            if (null == this.flowerList) {
                this.flowerList = new ArrayList<>();
            }
            this.flowerList.add((Flower) thing);
        }
        else if (thing instanceof Grass) {
            if (null == this.grassList) {
                this.grassList = new ArrayList<>();
            }
            this.grassList.add((Grass) thing);
        }
        else if (thing instanceof Sea) {
            if (null == this.seaList) {
                this.seaList = new ArrayList<>();
            }
            this.seaList.add((Sea) thing);
        }
        else if (thing instanceof Pool) {
            if (null == this.poolList) {
                this.poolList = new ArrayList<>();
            }
            this.poolList.add((Pool) thing);
        }
        else if (thing instanceof Sunflower) {
            if (null == this.sunflowerList) {
                this.sunflowerList = new ArrayList<>();
            }
            this.sunflowerList.add((Sunflower) thing);
        }
        else if (thing instanceof Mushroom) {
            if (null == this.mushroomList) {
                this.mushroomList = new ArrayList<>();
            }
            this.mushroomList.add((Mushroom) thing);
        }
        else if (thing instanceof Lotus) {
            if (null == this.lotusList) {
                this.lotusList = new ArrayList<>();
            }
            this.lotusList.add((Lotus) thing);
        }
        else if (thing instanceof PlumFlower) {
            if (null == this.plumFlowerList) {
                this.plumFlowerList = new ArrayList<>();
            }
            this.plumFlowerList.add((PlumFlower) thing);
        }
        else if (thing instanceof Rose) {
            if (null == this.roseList) {
                this.roseList = new ArrayList<>();
            }
            this.roseList.add((Rose) thing);
        }
        else if (thing instanceof Cloud) {
            if (null == this.cloudList) {
                this.cloudList = new ArrayList<>();
            }
            this.cloudList.add((Cloud) thing);
        }
        else if (thing instanceof Rain) {
            if (null == this.rainList) {
                this.rainList = new ArrayList<>();
            }
            this.rainList.add((Rain) thing);
        }
        else if (thing instanceof Rainbow) {
            if (null == this.rainbowList) {
                this.rainbowList = new ArrayList<>();
            }
            this.rainbowList.add((Rainbow) thing);
        }
        else if (thing instanceof Torch) {
            if (null == this.torchList) {
                this.torchList = new ArrayList<>();
            }
            this.torchList.add((Torch) thing);
        }
        else if (thing instanceof Bonfire) {
            if (null == this.bonfireList) {
                this.bonfireList = new ArrayList<>();
            }
            this.bonfireList.add((Bonfire) thing);
        }
        else if (thing instanceof Bird) {
            if (null == this.birdList) {
                this.birdList = new ArrayList<>();
            }
            this.birdList.add((Bird) thing);
        }
        else if (thing instanceof Cat) {
            if (null == this.catList) {
                this.catList = new ArrayList<>();
            }
            this.catList.add((Cat) thing);
        }
        else if (thing instanceof Dog) {
            if (null == this.dogList) {
                this.dogList = new ArrayList<>();
            }
            this.dogList.add((Dog) thing);
        }
        else if (thing instanceof Cow) {
            if (null == this.cowList) {
                this.cowList = new ArrayList<>();
            }
            this.cowList.add((Cow) thing);
        }
        else if (thing instanceof Sheep) {
            if (null == this.sheepList) {
                this.sheepList = new ArrayList<>();
            }
            this.sheepList.add((Sheep) thing);
        }
        else if (thing instanceof Pig) {
            if (null == this.pigList) {
                this.pigList = new ArrayList<>();
            }
            this.pigList.add((Pig) thing);
        }
        else if (thing instanceof Fish) {
            if (null == this.fishList) {
                this.fishList = new ArrayList<>();
            }
            this.fishList.add((Fish) thing);
        }
        else if (thing instanceof Rabbit) {
            if (null == this.rabbitList) {
                this.rabbitList = new ArrayList<>();
            }
            this.rabbitList.add((Rabbit) thing);
        }
        else if (thing instanceof Horse) {
            if (null == this.horseList) {
                this.horseList = new ArrayList<>();
            }
            this.horseList.add((Horse) thing);
        }
        else if (thing instanceof Hawk) {
            if (null == this.hawkList) {
                this.hawkList = new ArrayList<>();
            }
            this.hawkList.add((Hawk) thing);
        }
        else if (thing instanceof Rat) {
            if (null == this.ratList) {
                this.ratList = new ArrayList<>();
            }
            this.ratList.add((Rat) thing);
        }
        else if (thing instanceof Butterfly) {
            if (null == this.butterflyList) {
                this.butterflyList = new ArrayList<>();
            }
            this.butterflyList.add((Butterfly) thing);
        }
        else if (thing instanceof Tiger) {
            if (null == this.tigerList) {
                this.tigerList = new ArrayList<>();
            }
            this.tigerList.add((Tiger) thing);
        }
        else if (thing instanceof Hedgehog) {
            if (null == this.hedgehogList) {
                this.hedgehogList = new ArrayList<>();
            }
            this.hedgehogList.add((Hedgehog) thing);
        }
        else if (thing instanceof Snake) {
            if (null == this.snakeList) {
                this.snakeList = new ArrayList<>();
            }
            this.snakeList.add((Snake) thing);
        }
        else if (thing instanceof Dragon) {
            if (null == this.dragonList) {
                this.dragonList = new ArrayList<>();
            }
            this.dragonList.add((Dragon) thing);
        }
        else if (thing instanceof Watch) {
            if (null == this.watchList) {
                this.watchList = new ArrayList<>();
            }
            this.watchList.add((Watch) thing);
        }
        else if (thing instanceof Clock) {
            if (null == this.clockList) {
                this.clockList = new ArrayList<>();
            }
            this.clockList.add((Clock) thing);
        }
        else if (thing instanceof MusicalNotation) {
            if (null == this.musicalNotationList) {
                this.musicalNotationList = new ArrayList<>();
            }
            this.musicalNotationList.add((MusicalNotation) thing);
        }
        else if (thing instanceof TV) {
            if (null == this.tvList) {
                this.tvList = new ArrayList<>();
            }
            this.tvList.add((TV) thing);
        }
        else if (thing instanceof Pole) {
            if (null == this.poleList) {
                this.poleList = new ArrayList<>();
            }
            this.poleList.add((Pole) thing);
        }
        else if (thing instanceof Tower) {
            if (null == this.towerList) {
                this.towerList = new ArrayList<>();
            }
            this.towerList.add((Tower) thing);
        }
        else if (thing instanceof Lighthouse) {
            if (null == this.lighthouseList) {
                this.lighthouseList = new ArrayList<>();
            }
            this.lighthouseList.add((Lighthouse) thing);
        }
        else if (thing instanceof Gun) {
            if (null == this.gunList) {
                this.gunList = new ArrayList<>();
            }
            this.gunList.add((Gun) thing);
        }
        else if (thing instanceof Sword) {
            if (null == this.swordList) {
                this.swordList = new ArrayList<>();
            }
            this.swordList.add((Sword) thing);
        }
        else if (thing instanceof Knife) {
            if (null == this.knifeList) {
                this.knifeList = new ArrayList<>();
            }
            this.knifeList.add((Knife) thing);
        }
        else if (thing instanceof Shield) {
            if (null == this.shieldList) {
                this.shieldList = new ArrayList<>();
            }
            this.shieldList.add((Shield) thing);
        }
        else if (thing instanceof Sandglass) {
            if (null == this.sandglassList) {
                this.sandglassList = new ArrayList<>();
            }
            this.sandglassList.add((Sandglass) thing);
        }
        else if (thing instanceof Kite) {
            if (null == this.kiteList) {
                this.kiteList = new ArrayList<>();
            }
            this.kiteList.add((Kite) thing);
        }
        else if (thing instanceof Umbrella) {
            if (null == this.umbrellaList) {
                this.umbrellaList = new ArrayList<>();
            }
            this.umbrellaList.add((Umbrella) thing);
        }
        else if (thing instanceof Windmill) {
            if (null == this.windmillList) {
                this.windmillList = new ArrayList<>();
            }
            this.windmillList.add((Windmill) thing);
        }
        else if (thing instanceof Flag) {
            if (null == this.flagList) {
                this.flagList = new ArrayList<>();
            }
            this.flagList.add((Flag) thing);
        }
        else if (thing instanceof Bridge) {
            if (null == this.bridgeList) {
                this.bridgeList = new ArrayList<>();
            }
            this.bridgeList.add((Bridge) thing);
        }
        else if (thing instanceof Crossroads) {
            if (null == this.crossroadsList) {
                this.crossroadsList = new ArrayList<>();
            }
            this.crossroadsList.add((Crossroads) thing);
        }
        else if (thing instanceof Ladder) {
            if (null == this.ladderList) {
                this.ladderList = new ArrayList<>();
            }
            this.ladderList.add((Ladder) thing);
        }
        else if (thing instanceof Stairs) {
            if (null == this.stairsList) {
                this.stairsList = new ArrayList<>();
            }
            this.stairsList.add((Stairs) thing);
        }
        else if (thing instanceof Birdcage) {
            if (null == this.birdcageList) {
                this.birdcageList = new ArrayList<>();
            }
            this.birdcageList.add((Birdcage) thing);
        }
        else if (thing instanceof Car) {
            if (null == this.carList) {
                this.carList = new ArrayList<>();
            }
            this.carList.add((Car) thing);
        }
        else if (thing instanceof Boat) {
            if (null == this.boatList) {
                this.boatList = new ArrayList<>();
            }
            this.boatList.add((Boat) thing);
        }
        else if (thing instanceof Airplane) {
            if (null == this.airplaneList) {
                this.airplaneList = new ArrayList<>();
            }
            this.airplaneList.add((Airplane) thing);
        }
        else if (thing instanceof Bike) {
            if (null == this.bikeList) {
                this.bikeList = new ArrayList<>();
            }
            this.bikeList.add((Bike) thing);
        }
        else if (thing instanceof Skull) {
            if (null == this.skullList) {
                this.skullList = new ArrayList<>();
            }
            this.skullList.add((Skull) thing);
        }
        else if (thing instanceof Glasses) {
            if (null == this.glassesList) {
                this.glassesList = new ArrayList<>();
            }
            this.glassesList.add((Glasses) thing);
        }
        else if (thing instanceof Swing) {
            if (null == this.swingList) {
                this.swingList = new ArrayList<>();
            }
            this.swingList.add((Swing) thing);
        }
    }

    public Thing get(Label label) {
        switch (label) {
            case Table:
                return (null != this.tableList) ? this.tableList.get(0) : null;
            case Bed:
                return (null != this.bedList) ? this.bedList.get(0) : null;
            case Sun:
                return (null != this.sunList) ? this.sunList.get(0) : null;
            case Moon:
                return (null != this.moonList) ? this.moonList.get(0) : null;
            case Star:
                return (null != this.starList) ? this.starList.get(0) : null;
            case Mountain:
                return (null != this.mountainList) ? this.mountainList.get(0) : null;
            case Flower:
                return (null != this.flowerList) ? this.flowerList.get(0) : null;
            case Grass:
                return (null != this.grassList) ? this.grassList.get(0) : null;
            case Sea:
                return (null != this.seaList) ? this.seaList.get(0) : null;
            case Pool:
                return (null != this.poolList) ? this.poolList.get(0) : null;
            case Sunflower:
                return (null != this.sunflowerList) ? this.sunflowerList.get(0) : null;
            case Mushroom:
                return (null != this.mushroomList) ? this.mushroomList.get(0) : null;
            case Lotus:
                return (null != this.lotusList) ? this.lotusList.get(0) : null;
            case PlumFlower:
                return (null != this.plumFlowerList) ? this.plumFlowerList.get(0) : null;
            case Rose:
                return (null != this.roseList) ? this.roseList.get(0) : null;
            case Cloud:
                return (null != this.cloudList) ? this.cloudList.get(0) : null;
            case Rain:
                return (null != this.rainList) ? this.rainList.get(0) : null;
            case Rainbow:
                return (null != this.rainbowList) ? this.rainbowList.get(0) : null;
            case Torch:
                return (null != this.torchList) ? this.torchList.get(0) : null;
            case Bonfire:
                return (null != this.bonfireList) ? this.bonfireList.get(0) : null;
            case Bird:
                return (null != this.birdList) ? this.birdList.get(0) : null;
            case Cat:
                return (null != this.catList) ? this.catList.get(0) : null;
            case Dog:
                return (null != this.dogList) ? this.dogList.get(0) : null;
            case Cow:
                return (null != this.cowList) ? this.cowList.get(0) : null;
            case Sheep:
                return (null != this.sheepList) ? this.sheepList.get(0) : null;
            case Pig:
                return (null != this.pigList) ? this.pigList.get(0) : null;
            case Fish:
                return (null != this.fishList) ? this.fishList.get(0) : null;
            case Rabbit:
                return (null != this.rabbitList) ? this.rabbitList.get(0) : null;
            case Horse:
                return (null != this.horseList) ? this.horseList.get(0) : null;
            case Hawk:
                return (null != this.hawkList) ? this.hawkList.get(0) : null;
            case Rat:
                return (null != this.ratList) ? this.ratList.get(0) : null;
            case Butterfly:
                return (null != this.butterflyList) ? this.butterflyList.get(0) : null;
            case Tiger:
                return (null != this.tigerList) ? this.tigerList.get(0) : null;
            case Hedgehog:
                return (null != this.hedgehogList) ? this.hedgehogList.get(0) : null;
            case Snake:
                return (null != this.snakeList) ? this.snakeList.get(0) : null;
            case Dragon:
                return (null != this.dragonList) ? this.dragonList.get(0) : null;
            case Watch:
                return (null != this.watchList) ? this.watchList.get(0) : null;
            case Clock:
                return (null != this.clockList) ? this.clockList.get(0) : null;
            case MusicalNotation:
                return (null != this.musicalNotationList) ? this.musicalNotationList.get(0) : null;
            case TV:
                return (null != this.tvList) ? this.tvList.get(0) : null;
            case Pole:
                return (null != this.poleList) ? this.poleList.get(0) : null;
            case Tower:
                return (null != this.towerList) ? this.towerList.get(0) : null;
            case Lighthouse:
                return (null != this.lighthouseList) ? this.lighthouseList.get(0) : null;
            case Gun:
                return (null != this.gunList) ? this.gunList.get(0) : null;
            case Sword:
                return (null != this.swordList) ? this.swordList.get(0) : null;
            case Knife:
                return (null != this.knifeList) ? this.knifeList.get(0) : null;
            case Shield:
                return (null != this.shieldList) ? this.shieldList.get(0) : null;
            case Sandglass:
                return (null != this.sandglassList) ? this.sandglassList.get(0) : null;
            case Kite:
                return (null != this.kiteList) ? this.kiteList.get(0) : null;
            case Umbrella:
                return (null != this.umbrellaList) ? this.umbrellaList.get(0) : null;
            case Windmill:
                return (null != this.windmillList) ? this.windmillList.get(0) : null;
            case Flag:
                return (null != this.flagList) ? this.flagList.get(0) : null;
            case Bridge:
                return (null != this.bridgeList) ? this.bridgeList.get(0) : null;
            case Crossroads:
                return (null != this.crossroadsList) ? this.crossroadsList.get(0) : null;
            case Ladder:
                return (null != this.ladderList) ? this.ladderList.get(0) : null;
            case Stairs:
                return (null != this.stairsList) ? this.stairsList.get(0) : null;
            case Birdcage:
                return (null != this.birdcageList) ? this.birdcageList.get(0) : null;
            case Car:
                return (null != this.carList) ? this.carList.get(0) : null;
            case Boat:
                return (null != this.boatList) ? this.boatList.get(0) : null;
            case Airplane:
                return (null != this.airplaneList) ? this.airplaneList.get(0) : null;
            case Bike:
                return (null != this.bikeList) ? this.bikeList.get(0) : null;
            case Skull:
                return (null != this.skullList) ? this.skullList.get(0) : null;
            case Glasses:
                return (null != this.glassesList) ? this.glassesList.get(0) : null;
            case Swing:
                return (null != this.swingList) ? this.swingList.get(0) : null;
            default:
                break;
        }
        return null;
    }

    public List<Thing> getAll() {
        List<Thing> result = new ArrayList<>();

        if (null != this.tableList) {
            result.addAll(this.tableList);
        }
        if (null != this.bedList) {
            result.addAll(this.bedList);
        }
        if (null != this.sunList) {
            result.addAll(this.sunList);
        }
        if (null != this.moonList) {
            result.addAll(this.moonList);
        }
        if (null != this.starList) {
            result.addAll(this.starList);
        }
        if (null != this.mountainList) {
            result.addAll(this.mountainList);
        }
        if (null != this.flowerList) {
            result.addAll(this.flowerList);
        }
        if (null != this.grassList) {
            result.addAll(this.grassList);
        }
        if (null != this.seaList) {
            result.addAll(this.seaList);
        }
        if (null != this.poolList) {
            result.addAll(this.poolList);
        }
        if (null != this.sunflowerList) {
            result.addAll(this.sunflowerList);
        }
        if (null != this.mushroomList) {
            result.addAll(this.mushroomList);
        }
        if (null != this.lotusList) {
            result.addAll(this.lotusList);
        }
        if (null != this.plumFlowerList) {
            result.addAll(this.plumFlowerList);
        }
        if (null != this.roseList) {
            result.addAll(this.roseList);
        }
        if (null != this.cloudList) {
            result.addAll(this.cloudList);
        }
        if (null != this.rainList) {
            result.addAll(this.rainList);
        }
        if (null != this.rainbowList) {
            result.addAll(this.rainbowList);
        }
        if (null != this.torchList) {
            result.addAll(this.torchList);
        }
        if (null != this.bonfireList) {
            result.addAll(this.bonfireList);
        }
        if (null != this.birdList) {
            result.addAll(this.birdList);
        }
        if (null != this.catList) {
            result.addAll(this.catList);
        }
        if (null != this.dogList) {
            result.addAll(this.dogList);
        }
        if (null != this.cowList) {
            result.addAll(this.cowList);
        }
        if (null != this.sheepList) {
            result.addAll(this.sheepList);
        }
        if (null != this.pigList) {
            result.addAll(this.pigList);
        }
        if (null != this.fishList) {
            result.addAll(this.fishList);
        }
        if (null != this.rabbitList) {
            result.addAll(this.rabbitList);
        }
        if (null != this.horseList) {
            result.addAll(this.horseList);
        }
        if (null != this.hawkList) {
            result.addAll(this.hawkList);
        }
        if (null != this.ratList) {
            result.addAll(this.ratList);
        }
        if (null != this.butterflyList) {
            result.addAll(this.butterflyList);
        }
        if (null != this.tigerList) {
            result.addAll(this.tigerList);
        }
        if (null != this.hedgehogList) {
            result.addAll(this.hedgehogList);
        }
        if (null != this.snakeList) {
            result.addAll(this.snakeList);
        }
        if (null != this.dragonList) {
            result.addAll(this.dragonList);
        }
        if (null != this.watchList) {
            result.addAll(this.watchList);
        }
        if (null != this.clockList) {
            result.addAll(this.clockList);
        }
        if (null != this.musicalNotationList) {
            result.addAll(this.musicalNotationList);
        }
        if (null != this.tvList) {
            result.addAll(this.tvList);
        }
        if (null != this.poleList) {
            result.addAll(this.poleList);
        }
        if (null != this.towerList) {
            result.addAll(this.towerList);
        }
        if (null != this.lighthouseList) {
            result.addAll(this.lighthouseList);
        }
        if (null != this.gunList) {
            result.addAll(this.gunList);
        }
        if (null != this.swordList) {
            result.addAll(this.swordList);
        }
        if (null != this.knifeList) {
            result.addAll(this.knifeList);
        }
        if (null != this.shieldList) {
            result.addAll(this.shieldList);
        }
        if (null != this.sandglassList) {
            result.addAll(this.sandglassList);
        }
        if (null != this.kiteList) {
            result.addAll(this.kiteList);
        }
        if (null != this.umbrellaList) {
            result.addAll(this.umbrellaList);
        }
        if (null != this.windmillList) {
            result.addAll(this.windmillList);
        }
        if (null != this.flagList) {
            result.addAll(this.flagList);
        }
        if (null != this.bridgeList) {
            result.addAll(this.bridgeList);
        }
        if (null != this.crossroadsList) {
            result.addAll(this.crossroadsList);
        }
        if (null != this.ladderList) {
            result.addAll(this.ladderList);
        }
        if (null != this.stairsList) {
            result.addAll(this.stairsList);
        }
        if (null != this.birdcageList) {
            result.addAll(this.birdcageList);
        }
        if (null != this.carList) {
            result.addAll(this.carList);
        }
        if (null != this.boatList) {
            result.addAll(this.boatList);
        }
        if (null != this.airplaneList) {
            result.addAll(this.airplaneList);
        }
        if (null != this.bikeList) {
            result.addAll(this.bikeList);
        }
        if (null != this.skullList) {
            result.addAll(this.skullList);
        }
        if (null != this.glassesList) {
            result.addAll(this.glassesList);
        }
        if (null != this.swingList) {
            result.addAll(this.swingList);
        }

        return result;
    }

    public List<Thing> getList(Label label) {
        List<Thing> result = null;
        switch (label) {
            case Table:
                result = toThingList(this.tableList);
                break;
            case Bed:
                result = toThingList(this.bedList);
                break;
            case Sun:
                result = toThingList(this.sunList);
                break;
            case Moon:
                result = toThingList(this.moonList);
                break;
            case Star:
                result = toThingList(this.starList);
                break;
            case Mountain:
                result = toThingList(this.mountainList);
                break;
            case Flower:
                result = toThingList(this.flowerList);
                break;
            case Grass:
                result = toThingList(this.grassList);
                break;
            case Sea:
                result = toThingList(this.seaList);
                break;
            case Pool:
                result = toThingList(this.poolList);
                break;
            case Sunflower:
                result = toThingList(this.sunflowerList);
                break;
            case Mushroom:
                result = toThingList(this.mushroomList);
                break;
            case Lotus:
                result = toThingList(this.lotusList);
                break;
            case PlumFlower:
                result = toThingList(this.plumFlowerList);
                break;
            case Rose:
                result = toThingList(this.roseList);
                break;
            case Cloud:
                result = toThingList(this.cloudList);
                break;
            case Rain:
                result = toThingList(this.rainList);
                break;
            case Rainbow:
                result = toThingList(this.rainbowList);
                break;
            case Torch:
                result = toThingList(this.torchList);
                break;
            case Bonfire:
                result = toThingList(this.bonfireList);
                break;
            case Bird:
                result = toThingList(this.birdList);
                break;
            case Cat:
                result = toThingList(this.catList);
                break;
            case Dog:
                result = toThingList(this.dogList);
                break;
            case Cow:
                result = toThingList(this.cowList);
                break;
            case Sheep:
                result = toThingList(this.sheepList);
                break;
            case Pig:
                result = toThingList(this.pigList);
                break;
            case Fish:
                result = toThingList(this.fishList);
                break;
            case Rabbit:
                result = toThingList(this.rabbitList);
                break;
            case Horse:
                result = toThingList(this.horseList);
                break;
            case Hawk:
                result = toThingList(this.hawkList);
                break;
            case Rat:
                result = toThingList(this.ratList);
                break;
            case Butterfly:
                result = toThingList(this.butterflyList);
                break;
            case Tiger:
                result = toThingList(this.tigerList);
                break;
            case Hedgehog:
                result = toThingList(this.hedgehogList);
                break;
            case Snake:
                result = toThingList(this.snakeList);
                break;
            case Dragon:
                result = toThingList(this.dragonList);
                break;
            case Watch:
                result = toThingList(this.watchList);
                break;
            case Clock:
                result = toThingList(this.clockList);
                break;
            case MusicalNotation:
                result = toThingList(this.musicalNotationList);
                break;
            case TV:
                result = toThingList(this.tvList);
                break;
            case Pole:
                result = toThingList(this.poleList);
                break;
            case Tower:
                result = toThingList(this.towerList);
                break;
            case Lighthouse:
                result = toThingList(this.lighthouseList);
                break;
            case Gun:
                result = toThingList(this.gunList);
                break;
            case Sword:
                result = toThingList(this.swordList);
                break;
            case Knife:
                result = toThingList(this.knifeList);
                break;
            case Shield:
                result = toThingList(this.shieldList);
                break;
            case Sandglass:
                result = toThingList(this.sandglassList);
                break;
            case Kite:
                result = toThingList(this.kiteList);
                break;
            case Umbrella:
                result = toThingList(this.umbrellaList);
                break;
            case Windmill:
                result = toThingList(this.windmillList);
                break;
            case Flag:
                result = toThingList(this.flagList);
                break;
            case Bridge:
                result = toThingList(this.bridgeList);
                break;
            case Crossroads:
                result = toThingList(this.crossroadsList);
                break;
            case Ladder:
                result = toThingList(this.ladderList);
                break;
            case Stairs:
                result = toThingList(this.stairsList);
                break;
            case Birdcage:
                result = toThingList(this.birdcageList);
                break;
            case Car:
                result = toThingList(this.carList);
                break;
            case Boat:
                result = toThingList(this.boatList);
                break;
            case Airplane:
                result = toThingList(this.airplaneList);
                break;
            case Bike:
                result = toThingList(this.bikeList);
                break;
            case Skull:
                result = toThingList(this.skullList);
                break;
            case Glasses:
                result = toThingList(this.glassesList);
                break;
            case Swing:
                result = toThingList(this.swingList);
                break;
            default:
                break;
        }
        return result;
    }

    public boolean has(Label label) {
        switch (label) {
            case Table:
                return (null != this.tableList);
            case Bed:
                return (null != this.bedList);
            case Sun:
                return (null != this.sunList);
            case Moon:
                return (null != this.moonList);
            case Star:
                return (null != this.starList);
            case Mountain:
                return (null != this.mountainList);
            case Flower:
                return (null != this.flowerList);
            case Grass:
                return (null != this.grassList);
            case Sea:
                return (null != this.seaList);
            case Pool:
                return (null != this.poolList);
            case Sunflower:
                return (null != this.sunflowerList);
            case Mushroom:
                return (null != this.mushroomList);
            case Lotus:
                return (null != this.lotusList);
            case PlumFlower:
                return (null != this.plumFlowerList);
            case Rose:
                return (null != this.roseList);
            case Cloud:
                return (null != this.cloudList);
            case Rain:
                return (null != this.rainList);
            case Rainbow:
                return (null != this.rainbowList);
            case Torch:
                return (null != this.torchList);
            case Bonfire:
                return (null != this.bonfireList);
            case Bird:
                return (null != this.birdList);
            case Cat:
                return (null != this.catList);
            case Dog:
                return (null != this.dogList);
            case Cow:
                return (null != this.cowList);
            case Sheep:
                return (null != this.sheepList);
            case Pig:
                return (null != this.pigList);
            case Fish:
                return (null != this.fishList);
            case Rabbit:
                return (null != this.rabbitList);
            case Horse:
                return (null != this.horseList);
            case Hawk:
                return (null != this.hawkList);
            case Rat:
                return (null != this.ratList);
            case Butterfly:
                return (null != this.butterflyList);
            case Tiger:
                return (null != this.tigerList);
            case Hedgehog:
                return (null != this.hedgehogList);
            case Snake:
                return (null != this.snakeList);
            case Dragon:
                return (null != this.dragonList);
            case Watch:
                return (null != this.watchList);
            case Clock:
                return (null != this.clockList);
            case MusicalNotation:
                return (null != this.musicalNotationList);
            case TV:
                return (null != this.tvList);
            case Pole:
                return (null != this.poleList);
            case Tower:
                return (null != this.towerList);
            case Lighthouse:
                return (null != this.lighthouseList);
            case Gun:
                return (null != this.gunList);
            case Sword:
                return (null != this.swordList);
            case Knife:
                return (null != this.knifeList);
            case Shield:
                return (null != this.shieldList);
            case Sandglass:
                return (null != this.sandglassList);
            case Kite:
                return (null != this.kiteList);
            case Umbrella:
                return (null != this.umbrellaList);
            case Windmill:
                return (null != this.windmillList);
            case Flag:
                return (null != this.flagList);
            case Bridge:
                return (null != this.bridgeList);
            case Crossroads:
                return (null != this.crossroadsList);
            case Ladder:
                return (null != this.ladderList);
            case Stairs:
                return (null != this.stairsList);
            case Birdcage:
                return (null != this.birdcageList);
            case Car:
                return (null != this.carList);
            case Boat:
                return (null != this.boatList);
            case Airplane:
                return (null != this.airplaneList);
            case Bike:
                return (null != this.bikeList);
            case Skull:
                return (null != this.skullList);
            case Glasses:
                return (null != this.glassesList);
            case Swing:
                return (null != this.swingList);
            default:
                break;
        }
        return false;
    }

    public JSONArray toJSONArray() {
        JSONArray array = new JSONArray();
        List<Thing> all = this.getAll();
        for (Thing thing : all) {
            array.put(thing.toJSON());
        }
        return array;
    }

    private List<Thing> toThingList(List<? extends Thing> list) {
        List<Thing> result = new ArrayList<>();
        if (null != list) {
            result.addAll(list);
        }
        return result;
    }
}
