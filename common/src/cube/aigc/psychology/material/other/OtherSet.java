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

package cube.aigc.psychology.material.other;

import cell.util.log.Logger;
import cube.aigc.psychology.material.Classification;
import cube.aigc.psychology.material.Label;
import cube.aigc.psychology.material.Thing;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 其他绘画元素集合。
 */
public class OtherSet {

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

    public OtherSet() {
    }

    public OtherSet(JSONObject json) {
        if (json.has("others")) {
            JSONArray array = json.getJSONArray("others");
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

    public boolean has(Label label) {
        return false;
    }

    public JSONArray toJSONArray() {
        JSONArray array = new JSONArray();

        return array;
    }
}
