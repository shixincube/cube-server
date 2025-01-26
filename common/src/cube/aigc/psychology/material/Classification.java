/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material;

import cube.aigc.psychology.material.house.*;
import cube.aigc.psychology.material.other.*;
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
            case StickMan:
                return new StickMan(json);

            case Table:
                return new Table(json);
            case Bed:
                return new Bed(json);
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
            case Sea:
                return new Sea(json);
            case Pool:
                return new Pool(json);
            case Sunflower:
                return new Sunflower(json);
            case Mushroom:
                return new Mushroom(json);
            case Lotus:
                return new Lotus(json);
            case PlumFlower:
                return new PlumFlower(json);
            case Rose:
                return new Rose(json);
            case Cloud:
                return new Cloud(json);
            case Rain:
                return new Rain(json);
            case Rainbow:
                return new Rainbow(json);
            case Torch:
                return new Torch(json);
            case Bonfire:
                return new Bonfire(json);
            case Bird:
                return new Bird(json);
            case Cat:
                return new Cat(json);
            case Dog:
                return new Dog(json);
            case Cow:
                return new Cow(json);
            case Sheep:
                return new Sheep(json);
            case Pig:
                return new Pig(json);
            case Fish:
                return new Fish(json);
            case Rabbit:
                return new Rabbit(json);
            case Horse:
                return new Horse(json);
            case Hawk:
                return new Hawk(json);
            case Rat:
                return new Rat(json);
            case Butterfly:
                return new Butterfly(json);
            case Tiger:
                return new Tiger(json);
            case Hedgehog:
                return new Hedgehog(json);
            case Snake:
                return new Snake(json);
            case Dragon:
                return new Dragon(json);
            case Watch:
                return new Watch(json);
            case Clock:
                return new Clock(json);
            case MusicalNotation:
                return new MusicalNotation(json);
            case TV:
                return new TV(json);
            case Pole:
                return new Pole(json);
            case Tower:
                return new Tower(json);
            case Lighthouse:
                return new Lighthouse(json);
            case Gun:
                return new Gun(json);
            case Sword:
                return new Sword(json);
            case Knife:
                return new Knife(json);
            case Shield:
                return new Shield(json);
            case Sandglass:
                return new Sandglass(json);
            case Kite:
                return new Kite(json);
            case Umbrella:
                return new Umbrella(json);
            case Windmill:
                return new Windmill(json);
            case Flag:
                return new Flag(json);
            case Bridge:
                return new Bridge(json);
            case Crossroads:
                return new Crossroads(json);
            case Ladder:
                return new Ladder(json);
            case Stairs:
                return new Stairs(json);
            case Birdcage:
                return new Birdcage(json);
            case Car:
                return new Car(json);
            case Boat:
                return new Boat(json);
            case Airplane:
                return new Airplane(json);
            case Bike:
                return new Bike(json);
            case Skull:
                return new Skull(json);
            case Glasses:
                return new Glasses(json);
            case Swing:
                return new Swing(json);

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

            case PersonSideFace:
                return new Head(json);
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
                return new Eyeglass(json);

            default:
                break;
        }

        return null;
    }
}
