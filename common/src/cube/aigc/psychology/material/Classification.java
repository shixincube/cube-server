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

package cube.aigc.psychology.material;

import cube.aigc.psychology.material.house.*;
import cube.aigc.psychology.material.person.*;
import cube.aigc.psychology.material.tree.*;
import org.json.JSONObject;

/**
 * 分类器。
 */
public class Classification {

    public Classification() {
    }

    public Thing recognize(JSONObject json) {
        if (!json.has("label")) {
            return null;
        }

        Label label = Label.parse(json.getString("label"));

        switch (label) {
            case House:
                return new House(json);
            case Tree:
                return new Tree(json);
            case Person:
                return new Person(json);

            case Bungalow:
                return new Bungalow(json);
            case Villa:
                return new Villa(json);
            case Building:
                return new Building(json);
            case Fairyland:
                return new Fairyland(json);
            case Temple:
                return new Temple(json);
            case Grave:
                return new Grave(json);

            case DeciduousTree:
                return new DeciduousTree(json);
            case DeadTree:
                return new DeadTree(json);
            case PineTree:
                return new PineTree(json);
            case WillowTree:
                return new WillowTree(json);
            case CoconutTree:
                return new CoconutTree(json);
            case Bamboo:
                return new Bamboo(json);

            case Man:
                return new Man(json);
            case Woman:
                return new Woman(json);
            case Boy:
                return new Boy(json);
            case Girl:
                return new Girl(json);

            case Table:
                return new Table(json);

            case Sun:
                return new Sun(json);
            case Moon:
                return new Moon(json);
            case Star:
                return new Star(json);
            case Mountain:
                return new Mountain(json);
            case Flower:
                return new Flower(json);
            case Grass:
                return new Grass(json);
            case Cloud:
                return new Cloud(json);
            case Bird:
            case Cat:
            case Dog:
                return new Animal(json);

            case HouseSidewall:
                return new Sidewall(json);
            case HouseRoof:
                return new Roof(json);
            case HouseRoofSkylight:
                return new RoofSkylight(json);
            case HouseChimney:
                return new Chimney(json);
            case HouseWindow:
            case HouseWindowOpened:
                return new Window(json);
            case HouseDoor:
            case HouseDoorOpened:
            case HouseDoorLocked:
                return new Door(json);
            case HouseCurtain:
            case HouseCurtainOpened:
                return new Curtain(json);
            case HouseWindowRailing:
                return new WindowRailing(json);
            case HouseSmoke:
                return new Smoke(json);
            case HouseFence:
                return new Fence(json);
            case HousePath:
            case HouseCurvePath:
            case HouseCobbledPath:
                return new Path(json);

            case TreeTrunk:
                return new Trunk(json);
            case TreeBranch:
                return new Branch(json);
            case TreeCanopy:
                return new Canopy(json);
            case TreeRoot:
                return new Root(json);
            case TreeFruit:
                return new Fruit(json);
            case TreeHole:
                return new Hole(json);
            case TreeDrooping:
                return new DroopingLeaves(json);

            case PersonBraid:
                return new Braid(json);
            case PersonHead:
                return new Head(json);
            case PersonHair:
                return new Hair(json);
            case PersonStraightHair:
                return new StraightHair(json);
            case PersonShortHair:
                return new ShortHair(json);
            case PersonCurlyHair:
                return new CurlyHair(json);
            case PersonStandingHair:
                return new StandingHair(json);
            case PersonCap:
                return new Cap(json);
            case PersonEye:
                return new Eye(json);
            case PersonEyebrow:
                return new Eyebrow(json);
            case PersonNose:
                return new Nose(json);
            case PersonEar:
                return new Ear(json);
            case PersonMouth:
                return new Mouth(json);
            case PersonBody:
                return new Body(json);
            case PersonArm:
                return new Arm(json);
            case PersonPalm:
                return new Palm(json);
            case PersonLeg:
                return new Leg(json);
            case PersonFoot:
                return new Foot(json);
            case PersonSkirt:
                return new Skirt(json);
            case PersonMask:
                return new Mask(json);
            case PersonHairAccessories:
                return new HairAccessory(json);
            case PersonItem:
                return new Item(json);
            case PersonGlasses:
                return new Glasses(json);

            default:
                break;
        }

        return null;
    }
}
