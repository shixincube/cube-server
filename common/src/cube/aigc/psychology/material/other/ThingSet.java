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
import cube.aigc.psychology.material.Thing;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ThingSet {

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

    public ThingSet(JSONObject json) {
        if (json.has("tables")) {
            this.tableList = new ArrayList<>();
            this.parseList(json.getJSONArray("tables"), this.tableList);
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
    }

    public void add(Thing thing) {
        if (thing instanceof Table) {
            if (null == this.tableList) {
                this.tableList = new ArrayList<>();
            }
            this.tableList.add((Table) thing);
        }

    }

    public List<Thing> getAll() {
        return null;
    }

    private void parseList(JSONArray array, List targetList) {
        Classification classification = new Classification();

        for (int i = 0; i < array.length(); ++i) {
            Thing thing = classification.recognize(array.getJSONObject(i));
            if (null == thing) {
                Logger.w(this.getClass(), "#parseList - Unknown label: " + array.getJSONObject(i).toString(4));
                continue;
            }

            targetList.add(thing);
        }
    }
}
