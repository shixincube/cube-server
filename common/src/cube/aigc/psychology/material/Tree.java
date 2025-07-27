/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material;

import cube.aigc.psychology.material.tree.*;
import cube.vision.BoundingBox;
import cube.vision.Box;
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

    public Tree(BoundingBox boundingBox, Box box) {
        super(Label.Tree.name, boundingBox, box);
        this.backwardReasoning = true;
    }

    public void refreshBox(BoundingBox boundingBox, Box box) {
        this.boundingBox.x = Math.min(this.boundingBox.x, boundingBox.x);
        this.boundingBox.y = Math.min(this.boundingBox.y, boundingBox.y);
        int pX = Math.max(this.boundingBox.getX2(), boundingBox.getX2());
        int pY = Math.max(this.boundingBox.getY2(), boundingBox.getY2());

        this.boundingBox.width = pX - this.boundingBox.x;
        this.boundingBox.height = pY - this.boundingBox.y;

        this.box.refresh(box);
        this.area = Math.round(this.boundingBox.calculateArea() * 0.72f);
    }

    public void refreshArea() {
        this.area = Math.round(this.boundingBox.calculateArea() * 0.72f);
    }

    @Override
    public List<Thing> getSubThings(Label label) {
        List<Thing> things = null;
        switch (label) {
            case TreeTrunk:
                if (null != this.trunkList) {
                    things = new ArrayList<>(this.trunkList);
                }
                break;
            case TreeBranch:
                if (null != this.branchList) {
                    things = new ArrayList<>(this.branchList);
                }
                break;
            case TreeCanopy:
                if (null != this.canopyList) {
                    things = new ArrayList<>(this.canopyList);
                }
                break;
            case TreeRoot:
                if (null != this.rootList) {
                    things = new ArrayList<>(this.rootList);
                }
                break;
            case TreeFruit:
                if (null != this.fruitList) {
                    things = new ArrayList<>(this.fruitList);
                }
                break;
            case TreeHole:
                if (null != this.holeList) {
                    things = new ArrayList<>(this.holeList);
                }
                break;
            case TreeDrooping:
                if (null != this.droopingLeavesList) {
                    things = new ArrayList<>(this.droopingLeavesList);
                }
                break;
            default:
                break;
        }
        return things;
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

    /**
     * 树冠涂鸦
     *
     * @return
     */
    public boolean isDoodleCanopy() {
        if (null == this.canopyList) {
            return false;
        }

        Canopy canopy = this.getMaxAreaThing(this.canopyList);
        if (null == canopy) {
            return false;
        }

        if (canopy.texture.max > 4 && canopy.texture.avg > 2 && canopy.texture.hierarchy < 0.7) {
            return true;
        }
        else {
            return false;
        }
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

    public int numFruits() {
        return (null != this.fruitList) ? this.fruitList.size() : 0;
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

    @Override
    public boolean isDoodle() {
        return super.isDoodle();
    }

    @Override
    public int numComponents() {
        return ((null != this.trunkList) ? this.trunkList.size() : 0) +
                ((null != this.branchList) ? this.branchList.size() : 0) +
                ((null != this.canopyList) ? this.canopyList.size() : 0) +
                ((null != this.rootList) ? this.rootList.size() : 0) +
                ((null != this.fruitList) ? this.fruitList.size() : 0) +
                ((null != this.holeList) ? this.holeList.size() : 0) +
                ((null != this.droopingLeavesList) ? this.droopingLeavesList.size() : 0);
    }
}
