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

package cube.aigc.psychology;

import cube.aigc.psychology.composition.SpaceLayout;
import cube.aigc.psychology.material.Label;
import cube.common.entity.Material;
import cube.vision.Point;
import cube.vision.Size;

import java.util.ArrayList;
import java.util.List;

public class PaintingAccelerator {

    private Painting painting;

    private Parameter parameter;

    public PaintingAccelerator(Painting painting) {
        this.painting = painting;
        this.build();
    }

    public Parameter getParameter() {
        return this.parameter;
    }

    private void build() {
        this.parameter = new Parameter();
        SpaceLayout spaceLayout = new SpaceLayout(this.painting);

        this.parameter.frameAreaRatio = spaceLayout.getAreaRatio();
        this.parameter.wholeTextureMax = this.painting.getWhole().max;
        this.parameter.wholeTextureDensity = this.painting.getWhole().density;

        this.parameter.quadrant1TextureMax = this.painting.getQuadrants().get(0).max;
        this.parameter.quadrant1TextureAvg = this.painting.getQuadrants().get(0).avg;
        this.parameter.quadrant1TextureDensity = this.painting.getQuadrants().get(0).density;
        this.parameter.quadrant1TextureHierarchy = this.painting.getQuadrants().get(0).hierarchy;
        this.parameter.quadrant1TextureStandardDeviation = this.painting.getQuadrants().get(0).standardDeviation;

        this.parameter.quadrant2TextureMax = this.painting.getQuadrants().get(1).max;
        this.parameter.quadrant2TextureAvg = this.painting.getQuadrants().get(1).avg;
        this.parameter.quadrant2TextureDensity = this.painting.getQuadrants().get(1).density;
        this.parameter.quadrant2TextureHierarchy = this.painting.getQuadrants().get(1).hierarchy;
        this.parameter.quadrant2TextureStandardDeviation = this.painting.getQuadrants().get(1).standardDeviation;

        this.parameter.quadrant3TextureMax = this.painting.getQuadrants().get(2).max;
        this.parameter.quadrant3TextureAvg = this.painting.getQuadrants().get(2).avg;
        this.parameter.quadrant3TextureDensity = this.painting.getQuadrants().get(2).density;
        this.parameter.quadrant3TextureHierarchy = this.painting.getQuadrants().get(2).hierarchy;
        this.parameter.quadrant3TextureStandardDeviation = this.painting.getQuadrants().get(2).standardDeviation;

        this.parameter.quadrant4TextureMax = this.painting.getQuadrants().get(3).max;
        this.parameter.quadrant4TextureAvg = this.painting.getQuadrants().get(3).avg;
        this.parameter.quadrant4TextureDensity = this.painting.getQuadrants().get(3).density;
        this.parameter.quadrant4TextureHierarchy = this.painting.getQuadrants().get(3).hierarchy;
        this.parameter.quadrant4TextureStandardDeviation = this.painting.getQuadrants().get(3).standardDeviation;

        if (this.painting.hasHouse()) {
            this.parameter.house1 = new MaterialParameter(this.painting.getHouse());
            if (this.painting.getHouses().size() > 1) {
                // 默认列表是面积正序，所以这里获取列表里第一个，面积最小的一个
                this.parameter.house2 = new MaterialParameter(this.painting.getHouses().get(0));
            }
        }

        if (this.painting.hasTree()) {
            this.parameter.tree1 = new MaterialParameter(this.painting.getTree());
            if (this.painting.getTrees().size() > 1) {
                // 默认列表是面积正序，所以这里获取列表里第一个，面积最小的一个
                this.parameter.tree2 = new MaterialParameter(this.painting.getTrees().get(0));
            }
        }

        if (this.painting.hasPerson()) {
            this.parameter.person1 = new MaterialParameter(this.painting.getPerson());
            if (this.painting.getPersons().size() > 1) {
                // 默认列表是面积正序，所以这里获取列表里第一个，面积最小的一个
                this.parameter.person2 = new MaterialParameter(this.painting.getPersons().get(0));
            }
        }
    }

    public class Parameter {

        public double frameAreaRatio = 0;

        public double wholeTextureMax = 0;

        public double wholeTextureDensity = 0;

        public double quadrant1TextureMax = 0;
        public double quadrant1TextureAvg = 0;
        public double quadrant1TextureDensity = 0;
        public double quadrant1TextureHierarchy = 0;
        public double quadrant1TextureStandardDeviation = 0;

        public double quadrant2TextureMax = 0;
        public double quadrant2TextureAvg = 0;
        public double quadrant2TextureDensity = 0;
        public double quadrant2TextureHierarchy = 0;
        public double quadrant2TextureStandardDeviation = 0;

        public double quadrant3TextureMax = 0;
        public double quadrant3TextureAvg = 0;
        public double quadrant3TextureDensity = 0;
        public double quadrant3TextureHierarchy = 0;
        public double quadrant3TextureStandardDeviation = 0;

        public double quadrant4TextureMax = 0;
        public double quadrant4TextureAvg = 0;
        public double quadrant4TextureDensity = 0;
        public double quadrant4TextureHierarchy = 0;
        public double quadrant4TextureStandardDeviation = 0;

        public MaterialParameter house1;

        public MaterialParameter house2;

        public MaterialParameter tree1;

        public MaterialParameter tree2;

        public MaterialParameter person1;

        public MaterialParameter person2;

        public List<MaterialParameter> materials;

        public Parameter() {
            this.materials = new ArrayList<>();
        }
    }

    public class MaterialParameter {

        public Label label;

        public Point center;

        public double areaRatio = 0;

        public double textureMax = 0;

        public double textureHierarchy = 0;

        public double textureDensity = 0;

        public double textureStandardDeviation = 0;

        public MaterialParameter(Material material) {
            this.label = Label.parse(material.label);
            Size size = painting.getCanvasSize();

        }
    }
}
