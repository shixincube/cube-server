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
        Size canvasSize = this.painting.getCanvasSize();
        SpaceLayout spaceLayout = new SpaceLayout(this.painting);

        this.parameter.frameAreaRatio = spaceLayout.getAreaRatio();
        this.parameter.wholeTextureMax = this.painting.getWhole().max;
        this.parameter.wholeTextureDensity = this.painting.getWhole().density;

        this.parameter.quadrant1TextureMax = this.painting.getQuadrants().get(0).max;
        this.parameter.quadrant1TextureDensity = this.painting.getQuadrants().get(0).density;


        this.parameter.house1 = new MaterialParameter();

    }

    public class Parameter {

        public double frameAreaRatio = 0;

        public double wholeTextureMax = 0;

        public double wholeTextureDensity = 0;

        public double quadrant1TextureMax = 0;

        public double quadrant1TextureDensity = 0;

        public double quadrant2TextureMax = 0;

        public double quadrant2TextureDensity = 0;

        public double quadrant3TextureMax = 0;

        public double quadrant3TextureDensity = 0;

        public double quadrant4TextureMax = 0;

        public double quadrant4TextureDensity = 0;

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

        public double textureStandardDeviation = 0;

        public double textureHierarchy = 0;
    }
}
