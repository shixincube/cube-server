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

/**
 * 标签。
 */
public enum Label {

    House("p_house"),

    Tree("p_tree"),

    Person("p_person"),

    Man("p_man"),

    Woman("p_woman"),

    Boy("p_boy"),

    Girl("p_girl"),

    Table("p_table"),

    Sun("p_sun"),

    Moon("p_moon"),

    Star("p_star"),

    Mountain("p_mountain"),

    Flower("p_flower"),

    Grass("p_grass"),

    Cloud("p_cloud"),

    Bird("p_bird"),

    Cat("p_cat"),

    Dog("p_dog"),

    Temple("p_temple"),

    Grave("p_grave"),

    HouseRoof("p_house_roof"),

    HouseRoofTextured("p_house_roof_textured"),

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

    TreeTrunk("p_tree_trunk"),

    TreeBranch("p_tree_branch"),

    TreeCanopy("p_tree_canopy"),

    TreeRoot("p_tree_root"),

    TreeFruit("p_tree_fruit"),

    TreeHole("p_tree_hole"),

    TreeDrooping("p_tree_drooping"),

    PersonSideFace("p_person_side_face"),

    PersonBraid("p_person_braid"),

    PersonHead("p_person_head"),

    PersonHair("p_person_hair"),

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
}
