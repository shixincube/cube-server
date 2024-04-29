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

    private Roof roof;

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

        if (json.has("roof")) {
            this.roof = new Roof(json.getJSONObject("roof"));
        }
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

    public void setRoof(Roof roof) {
        this.roof = roof;
    }

    public Roof getRoof() {
        return this.roof;
    }

    public boolean hasRoof() {
        return (null != this.roof);
    }

    /**
     * 获取房顶相对整个房子的面积比例。
     * 例如：房顶面积30，房整体面积100，则比例为0.3
     *
     * @return
     */
    public double getRoofAreaRatio() {
        if (null == this.roof) {
            return 0;
        }

        return ((double) this.roof.area)
                / ((double) this.area);
    }

    /**
     * 获取房顶相对整个房子的高度比例。
     * 例如：房顶高20，房子高100，则比例为0.2
     *
     * @return
     */
    public double getRoofHeightRatio() {
        if (null == this.roof) {
            return 0;
        }

        return ((double) this.roof.box.height)
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
                    / ((double) this.area);
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
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        if (null != this.roof) {
            json.put("roof", this.roof.toJSON());
        }
        return json;
    }
}
