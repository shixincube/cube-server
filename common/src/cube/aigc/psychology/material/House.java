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

import cube.aigc.psychology.material.house.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 房。
 */
public class House extends Thing {

    private Roof roof;

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

    public void setRoof(Roof roof) {
        this.roof = roof;
    }

    public Roof getRoof() {
        return this.roof;
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

    public void addDoor(Door door) {
        if (null == this.doorList) {
            this.doorList = new ArrayList<>();
        }
        this.doorList.add(door);
    }

    public List<Door> getDoors() {
        return this.doorList;
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

    public void addPath(Path path) {
        if (null == this.pathList) {
            this.pathList = new ArrayList<>();
        }
        this.pathList.add(path);
    }

    public List<Path> getPaths() {
        return this.pathList;
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
