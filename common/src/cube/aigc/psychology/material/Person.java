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

import cube.aigc.psychology.material.person.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 人。
 */
public class Person extends Thing {

    private boolean sideFace = false;

    private Head head;

    private List<Hair> hairList;

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

    private Mask mask;

    private List<HairAccessory> hairAccessoryList;

    private List<Item> itemList;

    private Glasses glasses;

    public Person(JSONObject json) {
        super(json);
    }

    public void setHead(Head head) {
        this.head = head;
    }

    public Head getHead() {
        return this.head;
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

    public void addEye(Eye eye) {
        if (null == this.eyeList) {
            this.eyeList = new ArrayList<>();
        }
        this.eyeList.add(eye);
    }

    public List<Eye> getEyes() {
        return this.eyeList;
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

    public void setNose(Nose nose) {
        this.nose = nose;
    }

    public Nose getNose() {
        return this.nose;
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

    public void setMouth(Mouth mouth) {
        this.mouth = mouth;
    }

    public Mouth getMouth() {
        return this.mouth;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public Body getBody() {
        return this.body;
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

    public void addFoot(Foot foot) {
        if (null == this.footList) {
            this.footList = new ArrayList<>();
        }
        this.footList.add(foot);
    }

    public List<Foot> getFoot() {
        return this.footList;
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

    public void addItem(Item item) {
        if (null == this.itemList) {
            this.itemList = new ArrayList<>();
        }
        this.itemList.add(item);
    }

    public List<Item> getItems() {
        return this.itemList;
    }

    public void setGlasses(Glasses glasses) {
        this.glasses = glasses;
    }

    public Glasses getGlasses() {
        return this.glasses;
    }
}
