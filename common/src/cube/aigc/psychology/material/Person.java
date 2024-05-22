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

import cube.aigc.psychology.material.person.*;
import cube.vision.BoundingBox;
import cube.vision.Box;
import cube.vision.Point;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 人。
 */
public class Person extends Thing {

    /**
     * 性别。
     */
    public enum Gender {
        Male,

        Female,

        Unknown
    }

    private boolean sideFace = false;

    protected Gender gender = Gender.Unknown;

    private Head head;

    private List<Braid> braidList;

    private List<Hair> hairList;

    private Cap cap;

    private List<Eye> eyeList;

    private List<Eyebrow> eyebrowList;

    private Nose nose;

    private List<Ear> earList;

    private Mouth mouth;

    private Body body;

    private List<Arm> armList;

    private List<Palm> palmList;

    private List<Leg> legList;

    private List<Foot> footList;

    private Skirt skirt;

    private Mask mask;

    private List<HairAccessory> hairAccessoryList;

    private List<Item> itemList;

    private Eyeglass eyeglass;

    public Person(JSONObject json) {
        super(json);
    }

    public Person(BoundingBox boundingBox, Box box) {
        super(Label.Person.name, boundingBox, box);
        this.backwardReasoning = true;
    }

    public Gender getGender() {
        return this.gender;
    }

    public void refreshBox(BoundingBox boundingBox, Box box) {
        this.boundingBox.x = Math.min(this.boundingBox.x, boundingBox.x);
        this.boundingBox.y = Math.min(this.boundingBox.y, boundingBox.y);
        int pX = Math.max(this.boundingBox.getX2(), boundingBox.getX2());
        int pY = Math.max(this.boundingBox.getY2(), boundingBox.getY2());

        this.boundingBox.width = pX - this.boundingBox.x;
        this.boundingBox.height = pY - this.boundingBox.y;

        this.box.refresh(box);
        this.area = Math.round(this.boundingBox.calculateArea() * 0.76f);
    }

    public void setHead(Head head) {
        this.head = head;
    }

    public Head getHead() {
        return this.head;
    }

    public boolean hasHead() {
        return (null != this.head);
    }

    public double getHeadHeightRatio() {
        if (null == this.head) {
            return 0;
        }

        return ((double) this.head.getHeight() / (double) this.getHeight());
    }

    public void setSideFace(Head head) {
        this.sideFace = true;
        this.head = head;
    }

    public void addBraid(Braid braid) {
        if (null == this.braidList) {
            this.braidList = new ArrayList<>();
        }
        this.braidList.add(braid);

        this.gender = Gender.Female;
    }

    public List<Braid> getBraids() {
        return this.braidList;
    }

    public void addHair(Hair hair) {
        if (null == this.hairList) {
            this.hairList = new ArrayList<>();
        }
        this.hairList.add(hair);
    }

    public List<Hair> getHairs() {
        return this.hairList;
    }

    public boolean hasHair() {
        return (null != this.hairList);
    }

    public boolean hasStraightHair() {
        if (null == this.hairList) {
            return false;
        }

        for (Hair hair : this.hairList) {
            if (hair instanceof StraightHair) {
                return true;
            }
        }

        return false;
    }

    public boolean hasShortHair() {
        if (null == this.hairList) {
            return false;
        }

        for (Hair hair : this.hairList) {
            if (hair instanceof ShortHair) {
                return true;
            }
        }

        return false;
    }

    public boolean hasCurlyHair() {
        if (null == this.hairList) {
            return false;
        }

        for (Hair hair : this.hairList) {
            if (hair instanceof CurlyHair) {
                return true;
            }
        }

        return false;
    }

    public boolean hasStandingHair() {
        if (null == this.hairList) {
            return false;
        }

        for (Hair hair : this.hairList) {
            if (hair instanceof StandingHair) {
                return true;
            }
        }

        return false;
    }

    public void setCap(Cap cap) {
        this.cap = cap;
    }

    public Cap getCap() {
        return this.cap;
    }

    public boolean hasCap() {
        return (null != this.cap);
    }

    public void addEye(Eye eye) {
        if (null == this.eyeList) {
            this.eyeList = new ArrayList<>();
        }
        this.eyeList.add(eye);
    }

    public List<Eye> getEyes() {
        return this.eyeList;
    }

    public boolean hasEye() {
        return (null != this.eyeList);
    }

    public boolean hasOpenEye() {
        if (null == this.eyeList) {
            return false;
        }

        for (Eye eye : this.eyeList) {
            if (eye.isOpen()) {
                return true;
            }
        }

        return false;
    }

    public double getMaxEyeAreaRatio() {
        if (null == this.eyeList || null == this.head) {
            return 0;
        }

        Eye eye = this.getMaxAreaThing(this.eyeList);
        return ((double) eye.area)
                / ((double) this.head.area);
    }

    public void addEyebrow(Eyebrow eyebrow) {
        if (null == this.eyebrowList) {
            this.eyebrowList = new ArrayList<>();
        }
        this.eyebrowList.add(eyebrow);
    }

    public List<Eyebrow> getEyebrows() {
        return this.eyebrowList;
    }

    public boolean hasEyebrow() {
        return (null != this.eyebrowList);
    }

    public void setNose(Nose nose) {
        this.nose = nose;
    }

    public Nose getNose() {
        return this.nose;
    }

    public boolean hasNose() {
        return (null != this.nose);
    }

    public void addEar(Ear ear) {
        if (null == this.earList) {
            this.earList = new ArrayList<>();
        }
        this.earList.add(ear);
    }

    public List<Ear> getEars() {
        return this.earList;
    }

    public boolean hasEar() {
        return (null != this.earList);
    }

    public void setMouth(Mouth mouth) {
        this.mouth = mouth;
    }

    public Mouth getMouth() {
        return this.mouth;
    }

    public boolean hasMouth() {
        return (null != this.mouth);
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public Body getBody() {
        return this.body;
    }

    public boolean hasBody() {
        return (null != this.body);
    }

    public void addArm(Arm arm) {
        if (null == this.armList) {
            this.armList = new ArrayList<>();
        }
        this.armList.add(arm);
    }

    public List<Arm> getArms() {
        return this.armList;
    }

    public boolean hasTwoArms() {
        return (null != this.armList && this.armList.size() >= 2);
    }

    public double calcArmsDistance() {
        if (!this.hasTwoArms()) {
            return 0;
        }

        Point c1 = this.armList.get(0).box.getCenterPoint();
        Point c2 = this.armList.get(1).box.getCenterPoint();
        return Math.abs(c1.x - c2.x);
    }

    public void addPalm(Palm palm) {
        if (null == this.palmList) {
            this.palmList = new ArrayList<>();
        }
        this.palmList.add(palm);
    }

    public List<Palm> getPalms() {
        return this.palmList;
    }

    public void addLeg(Leg leg) {
        if (null == this.legList) {
            this.legList = new ArrayList<>();
        }
        this.legList.add(leg);
    }

    public List<Leg> getLegs() {
        return this.legList;
    }

    public boolean hasTwoLegs() {
        return (null != this.legList && this.legList.size() >= 2);
    }

    public double calcLegsDistance() {
        if (!this.hasTwoLegs()) {
            return 0;
        }

        Point c1 = this.legList.get(0).box.getCenterPoint();
        Point c2 = this.legList.get(1).box.getCenterPoint();
        return Math.abs(c1.x - c2.x);
    }

    public Leg getThinnestLeg() {
        if (null == this.legList) {
            return null;
        }

        Leg leg = null;
        int w = Integer.MAX_VALUE;
        for (Leg cur : this.legList) {
            if ((int) cur.getWidth() < w) {
                w = (int) cur.getWidth();
                leg = cur;
            }
        }
        return leg;
    }

    public void addFoot(Foot foot) {
        if (null == this.footList) {
            this.footList = new ArrayList<>();
        }
        this.footList.add(foot);
    }

    public List<Foot> getFoot() {
        return this.footList;
    }

    public void setSkirt(Skirt skirt) {
        this.skirt = skirt;
        if (null != this.skirt) {
            this.gender = Gender.Female;
        }
    }

    public Skirt getSkirt() {
        return this.skirt;
    }

    public void setMask(Mask mask) {
        this.mask = mask;
    }

    public Mask getMask() {
        return this.mask;
    }

    public void addHairAccessory(HairAccessory hairAccessory) {
        if (null == this.hairAccessoryList) {
            this.hairAccessoryList = new ArrayList<>();
        }
        this.hairAccessoryList.add(hairAccessory);
    }

    public List<HairAccessory> getHairAccessories() {
        return this.hairAccessoryList;
    }

    public boolean hasHairAccessory() {
        return (null != this.hairAccessoryList);
    }

    public void addItem(Item item) {
        if (null == this.itemList) {
            this.itemList = new ArrayList<>();
        }
        this.itemList.add(item);
    }

    public List<Item> getItems() {
        return this.itemList;
    }

    public void setGlasses(Eyeglass eyeglass) {
        this.eyeglass = eyeglass;
    }

    public Eyeglass getGlasses() {
        return this.eyeglass;
    }

    @Override
    public int numComponents() {
        int num = 0;
        if (null != this.head) {
            num += 1;
        }
        if (null != braidList) {
            num += braidList.size();
        }
        if (null != hairList) {
            num += 1;
        }
        if (null != cap) {
            num += 1;
        }
        if (null != eyeList) {
            num += eyeList.size();
        }
        if (null != eyebrowList) {
            num += eyebrowList.size();
        }
        if (null != nose) {
            num += 1;
        }
        if (null != earList) {
            num += earList.size();
        }
        if (null != mouth) {
            num += 1;
        }
        if (null != body) {
            num += 1;
        }
        if (null != armList) {
            num += armList.size();
        }
        if (null != palmList) {
            num += palmList.size();
        }
        if (null != legList) {
            num += legList.size();
        }
        if (null != footList) {
            num += footList.size();
        }
        if (null != skirt) {
            num += 1;
        }
        if (null != mask) {
            num += 1;
        }
        if (null != hairAccessoryList) {
            num += 1;
        }
        if (null != itemList) {
            num += 1;
        }
        if (null != eyeglass) {
            num += 1;
        }
        return num;
    }
}
