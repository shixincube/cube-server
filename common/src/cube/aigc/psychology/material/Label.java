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

package cube.aigc.psychology.material;

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
            if (label.name.equals(name)) {
                return label;
            }
        }

        return Label.Unknown;
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
}
