/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签。
 */
public enum Label {

    House("p_house"),

    Tree("p_tree"),

    Person("p_person"),

    // 房 - 类别

    Bungalow("p_bungalow"),

    Villa("p_villa"),

    Building("p_building"),

    Fairyland("p_fairyland"),

    Temple("p_temple"),

    Grave("p_grave"),

    // 树 - 类别

    DeciduousTree("p_deciduous_tree"),

    DeadTree("p_dead_tree"),

    PineTree("p_pine_tree"),

    WillowTree("p_willow_tree"),

    CoconutTree("p_coconut_tree"),

    Bamboo("p_bamboo"),

    // 人 - 类别

    Man("p_man"),

    Woman("p_woman"),

    Boy("p_boy"),

    Girl("p_girl"),

    StickMan("p_stick_man"),

    // 其他

    Table("p_table"),

    Bed("p_bed"),

    Sun("p_sun"),

    Moon("p_moon"),

    Star("p_star"),

    Mountain("p_mountain"),

    Flower("p_flower"),

    Grass("p_grass"),

    Sea("p_sea"),

    Pool("p_pool"),

    Sunflower("p_sunflower"),

    Mushroom("p_mushroom"),

    Lotus("p_lotus"),

    PlumFlower("p_plum_flower"),

    Rose("p_rose"),

    Cloud("p_cloud"),

    Rain("p_rain"),

    Rainbow("p_rainbow"),

    Torch("p_torch"),

    Bonfire("p_bonfire"),

    Bird("p_bird"),

    Cat("p_cat"),

    Dog("p_dog"),

    Cow("p_cow"),

    Sheep("p_sheep"),

    Pig("p_pig"),

    Fish("p_fish"),

    Rabbit("p_rabbit"),

    Horse("p_horse"),

    Hawk("p_hawk"),

    Rat("p_rat"),

    Butterfly("p_butterfly"),

    Tiger("p_tiger"),

    Hedgehog("p_hedgehog"),

    Snake("p_snake"),

    Dragon("p_dragon"),

    Watch("p_watch"),

    Clock("p_clock"),

    MusicalNotation("p_musical_notation"),

    TV("p_tv"),

    Pole("p_pole"),

    Tower("p_tower"),

    Lighthouse("p_lighthouse"),

    Gun("p_gun"),

    Sword("p_sword"),

    Knife("p_knife"),

    Shield("p_shield"),

    Sandglass("p_sandglass"),

    Kite("p_kite"),

    Umbrella("p_umbrella"),

    Windmill("p_windmill"),

    Flag("p_flag"),

    Bridge("p_bridge"),

    Crossroads("p_crossroads"),

    Ladder("p_ladder"),

    Stairs("p_stairs"),

    Birdcage("p_birdcage"),

    Car("p_car"),

    Boat("p_boat"),

    Airplane("p_airplane"),

    Bike("p_bike"),

    Skull("p_skull"),

    Glasses("p_glasses"),

    Swing("p_swing"),

    // 房 - 细节

    HouseSidewall("p_house_sidewall"),

    HouseRoof("p_house_roof"),

    HouseRoofTextured("p_house_roof_textured"),

    HouseRoofSkylight("p_house_roof_skylight"),

    HouseChimney("p_house_chimney"),

    HouseDoor("p_house_door"),

    HouseDoorOpened("p_house_door_opened"),

    HouseDoorLocked("p_house_door_locked"),

    HouseWindow("p_house_window"),

    HouseWindowOpened("p_house_window_opened"),

    HouseCurtain("p_house_curtain"),

    HouseCurtainOpened("p_house_curtain_opened"),

    HouseWindowRailing("p_house_window_railing"),

    HouseSmoke("p_house_smoke"),

    HouseFence("p_house_fence"),

    HousePath("p_house_path"),

    HouseCurvePath("p_house_curve_path"),

    HouseCobbledPath("p_house_cobbled_path"),

    // 树 - 细节

    TreeTrunk("p_tree_trunk"),

    TreeBranch("p_tree_branch"),

    TreeCanopy("p_tree_canopy"),

    TreeRoot("p_tree_root"),

    TreeFruit("p_tree_fruit"),

    TreeHole("p_tree_hole"),

    TreeDrooping("p_tree_drooping"),

    // 人 - 细节

    PersonSideFace("p_person_side_face"),

    PersonBraid("p_person_braid"),

    PersonHead("p_person_head"),

    PersonHair("p_person_hair"),

    PersonStraightHair("p_person_straight_hair"),

    PersonShortHair("p_person_short_hair"),

    PersonCurlyHair("p_person_curly_hair"),

    PersonStandingHair("p_person_standing_hair"),

    PersonCap("p_person_cap"),

    PersonEye("p_person_eye"),

    PersonEyebrow("p_person_eyebrow"),

    PersonNose("p_person_nose"),

    PersonEar("p_person_ear"),

    PersonSkirt("p_person_skirt"),

    PersonMouth("p_person_mouth"),

    PersonBody("p_person_body"),

    PersonArm("p_person_arm"),

    PersonPalm("p_person_palm"),

    PersonLeg("p_person_leg"),

    PersonFoot("p_person_foot"),

    PersonMask("p_person_mask"),

    PersonHairAccessories("p_person_hair_accessories"),

    PersonItem("p_person_item"),

    PersonGlasses("p_person_glasses"),

    Unknown("p_unknown");

    public final String name;

    Label(String name) {
        this.name = name;
    }

    public static Label parse(String name) {
        for (Label label : Label.values()) {
            if (label.name.equalsIgnoreCase(name)) {
                return label;
            }
        }

        return Label.Unknown;
    }

    public static boolean isHouse(Label label) {
        switch (label) {
            case House:
            case Bungalow:
            case Villa:
            case Building:
            case Fairyland:
            case Temple:
            case Grave:
                return true;
            default:
                return false;
        }
    }

    public static boolean isHouseComponent(Label label) {
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
                return true;
            default:
                return false;
        }
    }

    public static boolean isTree(Label label) {
        switch (label) {
            case Tree:
            case DeciduousTree:
            case DeadTree:
            case PineTree:
            case WillowTree:
            case CoconutTree:
            case Bamboo:
                return true;
            default:
                return false;
        }
    }

    public static boolean isTreeComponent(Label label) {
        switch (label) {
            case TreeTrunk:
            case TreeBranch:
            case TreeCanopy:
            case TreeRoot:
            case TreeFruit:
            case TreeHole:
            case TreeDrooping:
                return true;
            default:
                return false;
        }
    }

    public static boolean isPerson(Label label) {
        switch (label) {
            case Person:
            case Man:
            case Woman:
            case Boy:
            case Girl:
            case StickMan:
                return true;
            default:
                return false;
        }
    }

    public static boolean isPersonComponent(Label label) {
        switch (label) {
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
            case PersonSkirt:
            case PersonMouth:
            case PersonBody:
            case PersonArm:
            case PersonPalm:
            case PersonLeg:
            case PersonFoot:
            case PersonMask:
            case PersonHairAccessories:
            case PersonItem:
            case PersonGlasses:
                return true;
            default:
                return false;
        }
    }

    public static boolean isOther(Label label) {
        switch (label) {
            case Table:
            case Bed:
            case Sun:
            case Moon:
            case Star:
            case Mountain:
            case Flower:
            case Grass:
            case Sea:
            case Pool:
            case Sunflower:
            case Mushroom:
            case Lotus:
            case PlumFlower:
            case Rose:
            case Cloud:
            case Rain:
            case Rainbow:
            case Torch:
            case Bonfire:
            case Bird:
            case Cat:
            case Dog:
            case Cow:
            case Sheep:
            case Pig:
            case Fish:
            case Rabbit:
            case Horse:
            case Hawk:
            case Rat:
            case Butterfly:
            case Tiger:
            case Hedgehog:
            case Snake:
            case Dragon:
            case Watch:
            case Clock:
            case MusicalNotation:
            case TV:
            case Pole:
            case Tower:
            case Lighthouse:
            case Gun:
            case Sword:
            case Knife:
            case Shield:
            case Sandglass:
            case Kite:
            case Umbrella:
            case Windmill:
            case Flag:
            case Bridge:
            case Crossroads:
            case Ladder:
            case Stairs:
            case Birdcage:
            case Car:
            case Boat:
            case Airplane:
            case Bike:
            case Skull:
            case Glasses:
            case Swing:
                return true;
            default:
                return false;
        }
    }

    public static List<Label> getOthers() {
        List<Label> result = new ArrayList<>();
        for (Label label : Label.values()) {
            if (isOther(label)) {
                result.add(label);
            }
        }
        return result;
    }
}
