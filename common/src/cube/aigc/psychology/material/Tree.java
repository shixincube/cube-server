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

import cube.aigc.psychology.material.tree.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 树。
 */
public class Tree extends Thing {

    private List<Trunk> trunkList;

    private List<Branch> branchList;

    private List<Canopy> canopyList;

    private List<Root> rootList;

    private List<Fruit> fruitList;

    private List<Hole> holeList;

    private List<DroopingLeaves> droopingLeavesList;

    public Tree(JSONObject json) {
        super(json);
    }

    public void addTrunk(Trunk trunk) {
        if (null == this.trunkList) {
            this.trunkList = new ArrayList<>();
        }
        this.trunkList.add(trunk);
    }

    public List<Trunk> getTrunks() {
        return this.trunkList;
    }

    public boolean hasTrunk() {
        return (null != this.trunkList);
    }

    /**
     * 获取树干相较于树的宽度比例。
     * 例如：树干宽30，树整体宽度100，则返回0.3
     *
     * @return
     */
    public double getTrunkWidthRatio() {
        if (null == this.trunkList) {
            return 0;
        }

        return ((double) this.trunkList.get(0).box.width * 0.7f)
                / ((double) this.box.width);
    }

    public void addBranch(Branch branch) {
        if (null == this.branchList) {
            this.branchList = new ArrayList<>();
        }
        this.branchList.add(branch);
    }

    public List<Branch> getBranches() {
        return this.branchList;
    }

    public void addCanopy(Canopy canopy) {
        if (null == this.canopyList) {
            this.canopyList = new ArrayList<>();
        }
        this.canopyList.add(canopy);
    }

    public List<Canopy> getCanopies() {
        return this.canopyList;
    }

    public boolean hasCanopy() {
        return (null != this.canopyList);
    }

    /**
     * 计算树冠面积比例。
     *
     * @return
     */
    public double getCanopyAreaRatio() {
        if (null == this.canopyList) {
            return 0;
        }

        Canopy canopy = this.getMaxAreaThing(this.canopyList);
        if (null == canopy) {
            return 0;
        }

        return ((double) canopy.area) / ((double) this.area);
    }

    /**
     * 计算树冠高度比例。
     *
     * @return
     */
    public double getCanopyHeightRatio() {
        if (null == this.canopyList) {
            return 0;
        }

        double height = 0;
        for (Canopy canopy : this.canopyList) {
            if (canopy.getHeight() > height) {
                height = canopy.getHeight();
            }
        }

        return height / this.getHeight();
    }

    public void addRoot(Root root) {
        if (null == this.rootList) {
            this.rootList = new ArrayList<>();
        }
        this.rootList.add(root);
    }

    public List<Root> getRoots() {
        return this.rootList;
    }

    public boolean hasRoot() {
        return (null != this.rootList);
    }

    public void addFruit(Fruit fruit) {
        if (null == this.fruitList) {
            this.fruitList = new ArrayList<>();
        }
        this.fruitList.add(fruit);
    }

    public List<Fruit> getFruits() {
        return this.fruitList;
    }

    public boolean hasFruit() {
        return (null != this.fruitList);
    }

    public double[] getFruitAreaRatios() {
        if (null == this.fruitList || null == this.canopyList) {
            return null;
        }

        Canopy canopy = this.getMaxAreaThing(this.canopyList);
        double totalArea = canopy.area;

        double[] result = new double[this.fruitList.size()];
        int index = 0;
        for (Fruit fruit : this.fruitList) {
            double area = fruit.area;
            double ratio = area / totalArea;
            result[index] = ratio;
            ++index;
        }
        return result;
    }

    public void addHole(Hole hole) {
        if (null == this.holeList) {
            this.holeList = new ArrayList<>();
        }
        this.holeList.add(hole);
    }

    public List<Hole> getHoles() {
        return this.holeList;
    }

    public boolean hasHole() {
        return (null != this.holeList);
    }

    public void addDrooping(DroopingLeaves droopingLeaves) {
        if (null == this.droopingLeavesList) {
            this.droopingLeavesList = new ArrayList<>();
        }
        this.droopingLeavesList.add(droopingLeaves);
    }

    public List<DroopingLeaves> getDroopingLeaves() {
        return this.droopingLeavesList;
    }
}
