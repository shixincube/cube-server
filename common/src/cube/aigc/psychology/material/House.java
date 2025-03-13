/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.material;

import cube.aigc.psychology.material.house.*;
import cube.vision.BoundingBox;
import cube.vision.Box;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 房。
 */
public class House extends Thing {

    private List<Sidewall> sidewallList;

    private List<Roof> roofList;

    private List<RoofSkylight> roofSkylightList;

    private List<Chimney> chimneyList;

    private List<Window> windowList;

    private List<Door> doorList;

    private List<Curtain> curtainList;

    private List<WindowRailing> windowRailingList;

    private List<Smoke> smokeList;

    private List<Fence> fenceList;

    private List<Path> pathList;

    public House(JSONObject json) {
        super(json);
    }

    public House(BoundingBox boundingBox, Box box) {
        super(Label.House.name, boundingBox, box);
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
        this.area = Math.round(this.boundingBox.calculateArea() * 0.86f);
    }

    public void refreshArea() {
        this.area = Math.round(this.boundingBox.calculateArea() * 0.86f);
    }

    @Override
    public List<Thing> getSubThings(Label label) {
        List<Thing> things = null;
        switch (label) {
            case HouseSidewall:
                if (null != this.sidewallList) {
                    things = new ArrayList<>(this.sidewallList);
                }
                break;
            case HouseRoof:
                if (null != this.roofList) {
                    things = new ArrayList<>(this.roofList);
                }
                break;
            case HouseRoofSkylight:
                if (null != this.roofSkylightList) {
                    things = new ArrayList<>(this.roofSkylightList);
                }
                break;
            case HouseChimney:
                if (null != this.chimneyList) {
                    things = new ArrayList<>(this.chimneyList);
                }
                break;
            case HouseWindow:
                if (null != this.windowList) {
                    things = new ArrayList<>(this.windowList);
                }
                break;
            case HouseDoor:
                if (null != this.doorList) {
                    things = new ArrayList<>(this.doorList);
                }
                break;
            case HouseCurtain:
                if (null != this.curtainList) {
                    things = new ArrayList<>(this.curtainList);
                }
                break;
            case HouseWindowRailing:
                if (null != this.windowRailingList) {
                    things = new ArrayList<>(this.windowRailingList);
                }
                break;
            case HouseSmoke:
                if (null != this.smokeList) {
                    things = new ArrayList<>(this.smokeList);
                }
                break;
            case HouseFence:
                if (null != this.fenceList) {
                    things = new ArrayList<>(this.fenceList);
                }
                break;
            case HousePath:
                if (null != this.pathList) {
                    things = new ArrayList<>(this.pathList);
                }
                break;
            default:
                break;
        }
        return things;
    }

    @Override
    public boolean isDoodle() {
        // 涂鸦特征：轮廓密度高，层密度低
        if (this.texture.max > 0 && this.texture.hierarchy > 0) {
            // 判断最大值
            if (this.texture.max >= 1.0 && this.texture.density > 0.2) {
                // 判断标准差和层密度
                if (this.texture.standardDeviation >= 0.4 && this.texture.hierarchy <= 0.03) {
                    return true;
                }
            }
        }

        return false;
    }

    public void addSidewall(Sidewall sidewall) {
        if (null == this.sidewallList) {
            this.sidewallList = new ArrayList<>();
        }
        this.sidewallList.add(sidewall);
    }

    public List<Sidewall> getSidewalls() {
        return this.sidewallList;
    }

    public boolean hasSidewall() {
        return (null != this.sidewallList);
    }

    public void addRoof(Roof roof) {
        if (null == this.roofList) {
            this.roofList = new ArrayList<>();
        }
        this.roofList.add(roof);
    }

    public Roof getRoof() {
        if (null == this.roofList) {
            return null;
        }

        return this.roofList.get(0);
    }

    public boolean hasRoof() {
        return (null != this.roofList);
    }

    /**
     * 获取房顶相对整个房子的面积比例。
     * 例如：房顶面积30，房整体面积100，则比例为0.3
     *
     * @return
     */
    public double getRoofAreaRatio() {
        if (null == this.roofList) {
            return 0;
        }

        return ((double) this.roofList.get(0).area)
                / ((double) this.area);
    }

    /**
     * 获取房顶相对整个房子的高度比例。
     * 例如：房顶高20，房子高100，则比例为0.2
     *
     * @return
     */
    public double getRoofHeightRatio() {
        if (null == this.roofList) {
            return 0;
        }

        return ((double) this.roofList.get(0).box.height)
                / ((double) this.box.height);
    }

    public void addRoofSkylight(RoofSkylight roofSkylight) {
        if (null == this.roofSkylightList) {
            this.roofSkylightList = new ArrayList<>();
        }
        this.roofSkylightList.add(roofSkylight);
    }

    public List<RoofSkylight> getRoofSkylights() {
        return this.roofSkylightList;
    }

    public boolean hasRoofSkylight() {
        return (null != this.roofSkylightList);
    }

    public void addChimney(Chimney chimney) {
        if (null == this.chimneyList) {
            this.chimneyList = new ArrayList<>();
        }
        this.chimneyList.add(chimney);
    }

    public List<Chimney> getChimneys() {
        return this.chimneyList;
    }

    public boolean hasChimney() {
        return (null != this.chimneyList);
    }

    public void addWindow(Window window) {
        if (null == this.windowList) {
            this.windowList = new ArrayList<>();
        }
        this.windowList.add(window);
    }

    public List<Window> getWindows() {
        return this.windowList;
    }

    public boolean hasWindow() {
        return (null != this.windowList);
    }

    public double getMaxWindowAreaRatio() {
        double maxRatio = 0;
        for (Window window : this.windowList) {
            double ratio = ((double) window.area)
                    / ((double) this.area);
            if (ratio > maxRatio) {
                maxRatio = ratio;
            }
        }
        return maxRatio;
    }

    public void addDoor(Door door) {
        if (null == this.doorList) {
            this.doorList = new ArrayList<>();
        }
        this.doorList.add(door);
    }

    public List<Door> getDoors() {
        return this.doorList;
    }

    public boolean hasDoor() {
        return (null != this.doorList);
    }

    public boolean hasOpenDoor() {
        if (null == this.doorList) {
            return false;
        }

        for (Door door : this.doorList) {
            if (door.isOpen()) {
                return true;
            }
        }
        return false;
    }

    public double getMaxDoorAreaRatio() {
        double maxRatio = 0;
        for (Door door : this.doorList) {
            double ratio = ((double) door.area)
                    / ((double) this.area * 0.95);
            if (ratio > maxRatio) {
                maxRatio = ratio;
            }
        }
        return maxRatio;
    }

    /**
     * 所有门和窗总面积比例。
     *
     * @return
     */
    public double getAllDoorsAndWindowsAreaRatio() {
        double totalArea = 0;
        if (null != this.windowList) {
            for (Window window : this.windowList) {
                totalArea += window.area;
            }
        }
        if (null != this.doorList) {
            for (Door door : this.doorList) {
                totalArea += door.area;
            }
        }
        return totalArea / this.area;
    }

    public void addCurtain(Curtain curtain) {
        if (null == this.curtainList) {
            this.curtainList = new ArrayList<>();
        }
        this.curtainList.add(curtain);
    }

    public List<Curtain> getCurtains() {
        return this.curtainList;
    }

    public boolean hasCurtain() {
        return (null != this.curtainList);
    }

    public boolean hasOpenCurtain() {
        if (null == this.curtainList) {
            return false;
        }

        for (Curtain curtain : this.curtainList) {
            if (curtain.isOpen()) {
                return true;
            }
        }

        return false;
    }

    public void addWindowRailing(WindowRailing windowRailing) {
        if (null == this.windowRailingList) {
            this.windowRailingList = new ArrayList<>();
        }
        this.windowRailingList.add(windowRailing);
    }

    public List<WindowRailing> getWindowRailings() {
        return this.windowRailingList;
    }

    public void addSmoke(Smoke smoke) {
        if (null == this.smokeList) {
            this.smokeList = new ArrayList<>();
        }
        this.smokeList.add(smoke);
    }

    public List<Smoke> getSmoke() {
        return this.smokeList;
    }

    public void addFence(Fence fence) {
        if (null == this.fenceList) {
            this.fenceList = new ArrayList<>();
        }
        this.fenceList.add(fence);
    }

    public List<Fence> getFences() {
        return this.fenceList;
    }

    public boolean hasFence() {
        return (null != this.fenceList);
    }

    public void addPath(Path path) {
        if (null == this.pathList) {
            this.pathList = new ArrayList<>();
        }
        this.pathList.add(path);
    }

    public List<Path> getPaths() {
        return this.pathList;
    }

    public boolean hasPath() {
        return (null != this.pathList);
    }

    public boolean hasCurvePath() {
        if (null == this.pathList) {
            return false;
        }

        for (Path path : this.pathList) {
            if (path.isCurve()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasCobbledPath() {
        if (null == this.pathList) {
            return false;
        }

        for (Path path : this.pathList) {
            if (path.isCobbled()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int numComponents() {
        int num = 0;
        if (null != roofList) {
            num += roofList.size();
        }
        if (null != roofSkylightList) {
            num += roofSkylightList.size();
        }
        if (null != chimneyList) {
            num += chimneyList.size();
        }
        if (null != windowList) {
            num += windowList.size();
        }
        if (null != doorList) {
            num += doorList.size();
        }
        if (null != curtainList) {
            num += curtainList.size();
        }
        if (null != windowRailingList) {
            num += windowRailingList.size();
        }
        if (null != smokeList) {
            num += smokeList.size();
        }
        return num;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        return json;
    }
}
