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
import cube.aigc.psychology.material.*;
import cube.aigc.psychology.material.other.OtherSet;
import cube.common.entity.Material;
import cube.util.FloatUtils;
import cube.vision.Point;
import cube.vision.Size;

import java.util.ArrayList;
import java.util.List;

public class PaintingAccelerator {

    private final double scaleSize = 1280.0;

    private final double aligningAreaRatio = 100.0;

    private final double aligningSize = 1.0 / 960.0;

    private final double aligningPixel = 1.0 / 10.0;

    private final double aligningDensity = 1.0 / 2.0;

    private final double aligningHierarchy = 1.0 / 5.0;

    private final double aligningSd = 1.0 / 5.0;

    private Painting painting;

    private Parameter parameter;

    private House house1;
    private House house2;

    private Tree tree1;
    private Tree tree2;

    private Person person1;
    private Person person2;

    public PaintingAccelerator(Painting painting) {
        this.painting = painting;
        this.build();
    }

    public Parameter getParameter() {
        return this.parameter;
    }

    public boolean isValid() {
        return this.parameter.frameAreaRatio != 0;
    }

    public double[] parameters() {
        double[] data = new double[1754];
//        buf.append(this.parameter.frameAreaRatio);
//        buf.append(",").append(this.parameter.wholeTextureMax);
//        buf.append(",").append(this.parameter.wholeTextureAvg);
//        buf.append(",").append(this.parameter.wholeTextureDensity);
//        buf.append(",").append(this.parameter.wholeTextureHierarchy);
//        buf.append(",").append(this.parameter.wholeTextureStandardDeviation);
//
//        buf.append(",").append(this.parameter.quadrant1TextureMax);
//        buf.append(",").append(this.parameter.quadrant1TextureAvg);
//        buf.append(",").append(this.parameter.quadrant1TextureDensity);
//        buf.append(",").append(this.parameter.quadrant1TextureHierarchy);
//        buf.append(",").append(this.parameter.quadrant1TextureStandardDeviation);
//
//        buf.append(",").append(this.parameter.quadrant2TextureMax);
//        buf.append(",").append(this.parameter.quadrant2TextureAvg);
//        buf.append(",").append(this.parameter.quadrant2TextureDensity);
//        buf.append(",").append(this.parameter.quadrant2TextureHierarchy);
//        buf.append(",").append(this.parameter.quadrant2TextureStandardDeviation);
//
//        buf.append(",").append(this.parameter.quadrant3TextureMax);
//        buf.append(",").append(this.parameter.quadrant3TextureAvg);
//        buf.append(",").append(this.parameter.quadrant3TextureDensity);
//        buf.append(",").append(this.parameter.quadrant3TextureHierarchy);
//        buf.append(",").append(this.parameter.quadrant3TextureStandardDeviation);
//
//        buf.append(",").append(this.parameter.quadrant4TextureMax);
//        buf.append(",").append(this.parameter.quadrant4TextureAvg);
//        buf.append(",").append(this.parameter.quadrant4TextureDensity);
//        buf.append(",").append(this.parameter.quadrant4TextureHierarchy);
//        buf.append(",").append(this.parameter.quadrant4TextureStandardDeviation);
//
//        buf.append(",").append(this.parameter.house1.formatCSV());
//        buf.append(",").append(this.outputHouseParameterAsCSV(this.house1));
//        buf.append(",").append(this.parameter.house2.formatCSV());
//        buf.append(",").append(this.outputHouseParameterAsCSV(this.house2));
//
//        buf.append(",").append(this.parameter.tree1.formatCSV());
//        buf.append(",").append(this.outputTreeParameterAsCSV(this.tree1));
//        buf.append(",").append(this.parameter.tree2.formatCSV());
//        buf.append(",").append(this.outputTreeParameterAsCSV(this.tree2));
//
//        buf.append(",").append(this.parameter.person1.formatCSV());
//        buf.append(",").append(this.outputPersonParameterAsCSV(this.person1));
//        buf.append(",").append(this.parameter.person2.formatCSV());
//        buf.append(",").append(this.outputPersonParameterAsCSV(this.person2));
//
//        for (MaterialParameter mp : this.parameter.materials) {
//            buf.append(",").append(mp.formatCSV());
//        }
        return null;
    }

    public String formatCSVHead() {
        StringBuilder buf = new StringBuilder();
        buf.append("frame_area_ratio");
        buf.append(",").append("whole_t_max");
        buf.append(",").append("whole_t_avg");
        buf.append(",").append("whole_t_density");
        buf.append(",").append("whole_t_hierarchy");
        buf.append(",").append("whole_t_sd");
        buf.append(",").append("quadrant1_t_max");
        buf.append(",").append("quadrant1_t_avg");
        buf.append(",").append("quadrant1_t_density");
        buf.append(",").append("quadrant1_t_hierarchy");
        buf.append(",").append("quadrant1_t_sd");
        buf.append(",").append("quadrant2_t_max");
        buf.append(",").append("quadrant2_t_avg");
        buf.append(",").append("quadrant2_t_density");
        buf.append(",").append("quadrant2_t_hierarchy");
        buf.append(",").append("quadrant2_t_sd");
        buf.append(",").append("quadrant3_t_max");
        buf.append(",").append("quadrant3_t_avg");
        buf.append(",").append("quadrant3_t_density");
        buf.append(",").append("quadrant3_t_hierarchy");
        buf.append(",").append("quadrant3_t_sd");
        buf.append(",").append("quadrant4_t_max");
        buf.append(",").append("quadrant4_t_avg");
        buf.append(",").append("quadrant4_t_density");
        buf.append(",").append("quadrant4_t_hierarchy");
        buf.append(",").append("quadrant4_t_sd");

        for (int i = 1; i <= 2; ++i) {
            String prefix = "house" + i;
            buf.append(",").append(formatMaterialParameterCSVHead(prefix));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_sidewall"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_roof"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_roof_skylight"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_chimney"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_window"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_door"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_curtain"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_window_railing"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_smoke"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_fence"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_path"));
        }

        for (int i = 1; i <= 2; ++i) {
            String prefix = "tree" + i;
            buf.append(",").append(formatMaterialParameterCSVHead(prefix));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_trunk"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_branch"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_canopy"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_root"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_fruit"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_hole"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_drooping"));
        }

        for (int i = 1; i <= 2; ++i) {
            String prefix = "person" + i;
            buf.append(",").append(formatMaterialParameterCSVHead(prefix));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_braid"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_head"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_hair"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_cap"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_eye"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_eyebrow"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_nose"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_ear"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_mouth"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_body"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_arm"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_palm"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_leg"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_foot"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_mask"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_hair_accessory"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_skirt"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_item"));
            buf.append(",").append(formatMaterialParameterCSVHead(prefix + "_eyeglass"));
        }

        for (MaterialParameter mp : this.parameter.materials) {
            buf.append(",").append(formatMaterialParameterCSVHead(mp.label.name.substring(2)));
        }

        return buf.toString();
    }

    public String formatParameterAsCSV(boolean normalization) {
        StringBuilder buf = new StringBuilder();

        buf.append(this.parameter.frameAreaRatio);
        buf.append(",").append(normalization ? this.parameter.wholeTextureMax * aligningPixel : this.parameter.wholeTextureMax);
        buf.append(",").append(normalization ? this.parameter.wholeTextureAvg * aligningPixel : this.parameter.wholeTextureAvg);
        buf.append(",").append(normalization ? this.parameter.wholeTextureDensity * aligningDensity : this.parameter.wholeTextureDensity);
        buf.append(",").append(normalization ? this.parameter.wholeTextureHierarchy * aligningHierarchy : this.parameter.wholeTextureHierarchy);
        buf.append(",").append(normalization ? this.parameter.wholeTextureStandardDeviation * aligningSd : this.parameter.wholeTextureStandardDeviation);

        buf.append(",").append(normalization ? this.parameter.quadrant1TextureMax * aligningPixel : this.parameter.quadrant1TextureMax);
        buf.append(",").append(normalization ? this.parameter.quadrant1TextureAvg * aligningPixel : this.parameter.quadrant1TextureAvg);
        buf.append(",").append(normalization ? this.parameter.quadrant1TextureDensity * aligningDensity : this.parameter.quadrant1TextureDensity);
        buf.append(",").append(normalization ? this.parameter.quadrant1TextureHierarchy * aligningHierarchy : this.parameter.quadrant1TextureHierarchy);
        buf.append(",").append(normalization ? this.parameter.quadrant1TextureStandardDeviation * aligningSd : this.parameter.quadrant1TextureStandardDeviation);

        buf.append(",").append(normalization ? this.parameter.quadrant2TextureMax * aligningPixel : this.parameter.quadrant2TextureMax);
        buf.append(",").append(normalization ? this.parameter.quadrant2TextureAvg * aligningPixel : this.parameter.quadrant2TextureAvg);
        buf.append(",").append(normalization ? this.parameter.quadrant2TextureDensity * aligningDensity : this.parameter.quadrant2TextureDensity);
        buf.append(",").append(normalization ? this.parameter.quadrant2TextureHierarchy * aligningHierarchy : this.parameter.quadrant2TextureHierarchy);
        buf.append(",").append(normalization ? this.parameter.quadrant2TextureStandardDeviation * aligningSd: this.parameter.quadrant2TextureStandardDeviation);

        buf.append(",").append(normalization ? this.parameter.quadrant3TextureMax * aligningPixel : this.parameter.quadrant3TextureMax);
        buf.append(",").append(normalization ? this.parameter.quadrant3TextureAvg * aligningPixel : this.parameter.quadrant3TextureAvg);
        buf.append(",").append(normalization ? this.parameter.quadrant3TextureDensity * aligningDensity : this.parameter.quadrant3TextureDensity);
        buf.append(",").append(normalization ? this.parameter.quadrant3TextureHierarchy * aligningHierarchy : this.parameter.quadrant3TextureHierarchy);
        buf.append(",").append(normalization ? this.parameter.quadrant3TextureStandardDeviation * aligningSd : this.parameter.quadrant3TextureStandardDeviation);

        buf.append(",").append(normalization ? this.parameter.quadrant4TextureMax * aligningPixel : this.parameter.quadrant4TextureMax);
        buf.append(",").append(normalization ? this.parameter.quadrant4TextureAvg * aligningPixel : this.parameter.quadrant4TextureAvg);
        buf.append(",").append(normalization ? this.parameter.quadrant4TextureDensity * aligningDensity : this.parameter.quadrant4TextureDensity);
        buf.append(",").append(normalization ? this.parameter.quadrant4TextureHierarchy * aligningHierarchy : this.parameter.quadrant4TextureHierarchy);
        buf.append(",").append(normalization ? this.parameter.quadrant4TextureStandardDeviation * aligningSd : this.parameter.quadrant4TextureStandardDeviation);

        buf.append(",").append(this.parameter.house1.formatCSV(normalization));
        buf.append(",").append(this.formatHouseParameterAsCSV(this.house1, normalization));
        buf.append(",").append(this.parameter.house2.formatCSV(normalization));
        buf.append(",").append(this.formatHouseParameterAsCSV(this.house2, normalization));

        buf.append(",").append(this.parameter.tree1.formatCSV(normalization));
        buf.append(",").append(this.formatTreeParameterAsCSV(this.tree1, normalization));
        buf.append(",").append(this.parameter.tree2.formatCSV(normalization));
        buf.append(",").append(this.formatTreeParameterAsCSV(this.tree2, normalization));

        buf.append(",").append(this.parameter.person1.formatCSV(normalization));
        buf.append(",").append(this.formatPersonParameterAsCSV(this.person1, normalization));
        buf.append(",").append(this.parameter.person2.formatCSV(normalization));
        buf.append(",").append(this.formatPersonParameterAsCSV(this.person2, normalization));

        for (MaterialParameter mp : this.parameter.materials) {
            buf.append(",").append(mp.formatCSV(normalization));
        }

        if (normalization) {
            String[] seg = buf.toString().split(",");
            double[] data = new double[seg.length];
            for (int i = 0; i < data.length; ++i) {
                data[i] = Double.parseDouble(seg[i]);
            }
            double[] normalizationValues = FloatUtils.normalization(data, 0.0, 1.0);
            buf = new StringBuilder();
            for (int i = 0; i < normalizationValues.length; ++i) {
                buf.append(normalizationValues[i]);
                if (i < normalizationValues.length - 1) {
                    buf.append(",");
                }
            }
        }

        return buf.toString();
    }

    private double[] houseParameters(House house, boolean normalization) {
        double[] result = new double[11 * 12];
        MaterialParameter blank = new MaterialParameter(Label.Unknown);

        if (null != house) {
            if (null != house.getSubThings(Label.HouseSidewall)) {
                double[] data = new MaterialParameter(house.getSubThings(Label.HouseSidewall).get(0)).parameters(normalization);
                System.arraycopy(data, 0, result, 0, 12);
            } else {
                System.arraycopy(blank.parameters(normalization), 0, result, 0, 12);
            }

//            if (null != house.getSubThings(Label.HouseRoof)) {
//                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseRoof).get(0)).formatCSV());
//            } else {
//                buf.append(",").append(blank.formatCSV());
//            }
//
//            if (null != house.getSubThings(Label.HouseRoofSkylight)) {
//                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseRoofSkylight).get(0)).formatCSV());
//            } else {
//                buf.append(",").append(blank.formatCSV());
//            }
//
//            if (null != house.getSubThings(Label.HouseChimney)) {
//                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseChimney).get(0)).formatCSV());
//            } else {
//                buf.append(",").append(blank.formatCSV());
//            }
//
//            if (null != house.getSubThings(Label.HouseWindow)) {
//                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseWindow).get(0)).formatCSV());
//            } else {
//                buf.append(",").append(blank.formatCSV());
//            }
//
//            if (null != house.getSubThings(Label.HouseDoor)) {
//                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseDoor).get(0)).formatCSV());
//            } else {
//                buf.append(",").append(blank.formatCSV());
//            }
//
//            if (null != house.getSubThings(Label.HouseCurtain)) {
//                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseCurtain).get(0)).formatCSV());
//            } else {
//                buf.append(",").append(blank.formatCSV());
//            }
//
//            if (null != house.getSubThings(Label.HouseWindowRailing)) {
//                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseWindowRailing).get(0)).formatCSV());
//            } else {
//                buf.append(",").append(blank.formatCSV());
//            }
//
//            if (null != house.getSubThings(Label.HouseSmoke)) {
//                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseSmoke).get(0)).formatCSV());
//            } else {
//                buf.append(",").append(blank.formatCSV());
//            }
//
//            if (null != house.getSubThings(Label.HouseFence)) {
//                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseFence).get(0)).formatCSV());
//            } else {
//                buf.append(",").append(blank.formatCSV());
//            }
//
//            if (null != house.getSubThings(Label.HousePath)) {
//                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HousePath).get(0)).formatCSV());
//            } else {
//                buf.append(",").append(blank.formatCSV());
//            }
        }
        else {
//            buf.append(blank.formatCSV());
//            buf.append(",").append(blank.formatCSV());
//            buf.append(",").append(blank.formatCSV());
//            buf.append(",").append(blank.formatCSV());
//            buf.append(",").append(blank.formatCSV());
//            buf.append(",").append(blank.formatCSV());
//            buf.append(",").append(blank.formatCSV());
//            buf.append(",").append(blank.formatCSV());
//            buf.append(",").append(blank.formatCSV());
//            buf.append(",").append(blank.formatCSV());
//            buf.append(",").append(blank.formatCSV());
        }

        return result;
    }

    private String formatHouseParameterAsCSV(House house, boolean normalization) {
        MaterialParameter blank = new MaterialParameter(Label.Unknown);

        StringBuilder buf = new StringBuilder();
        if (null != house) {
            if (null != house.getSubThings(Label.HouseSidewall)) {
                buf.append(new MaterialParameter(house.getSubThings(Label.HouseSidewall).get(0)).formatCSV(normalization));
            } else {
                buf.append(blank.formatCSV(normalization));
            }

            if (null != house.getSubThings(Label.HouseRoof)) {
                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseRoof).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != house.getSubThings(Label.HouseRoofSkylight)) {
                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseRoofSkylight).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != house.getSubThings(Label.HouseChimney)) {
                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseChimney).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != house.getSubThings(Label.HouseWindow)) {
                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseWindow).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != house.getSubThings(Label.HouseDoor)) {
                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseDoor).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != house.getSubThings(Label.HouseCurtain)) {
                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseCurtain).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != house.getSubThings(Label.HouseWindowRailing)) {
                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseWindowRailing).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != house.getSubThings(Label.HouseSmoke)) {
                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseSmoke).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != house.getSubThings(Label.HouseFence)) {
                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HouseFence).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != house.getSubThings(Label.HousePath)) {
                buf.append(",").append(new MaterialParameter(house.getSubThings(Label.HousePath).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }
        }
        else {
            buf.append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
        }
        return buf.toString();
    }

    private String formatTreeParameterAsCSV(Tree tree, boolean normalization) {
        MaterialParameter blank = new MaterialParameter(Label.Unknown);

        StringBuilder buf = new StringBuilder();
        if (null != tree) {
            if (null != tree.getSubThings(Label.TreeTrunk)) {
                buf.append(new MaterialParameter(tree.getSubThings(Label.TreeTrunk).get(0)).formatCSV(normalization));
            } else {
                buf.append(blank.formatCSV(normalization));
            }

            if (null != tree.getSubThings(Label.TreeBranch)) {
                buf.append(",").append(new MaterialParameter(tree.getSubThings(Label.TreeBranch).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != tree.getSubThings(Label.TreeCanopy)) {
                buf.append(",").append(new MaterialParameter(tree.getSubThings(Label.TreeCanopy).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != tree.getSubThings(Label.TreeRoot)) {
                buf.append(",").append(new MaterialParameter(tree.getSubThings(Label.TreeRoot).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != tree.getSubThings(Label.TreeFruit)) {
                buf.append(",").append(new MaterialParameter(tree.getSubThings(Label.TreeFruit).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != tree.getSubThings(Label.TreeHole)) {
                buf.append(",").append(new MaterialParameter(tree.getSubThings(Label.TreeHole).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != tree.getSubThings(Label.TreeDrooping)) {
                buf.append(",").append(new MaterialParameter(tree.getSubThings(Label.TreeDrooping).get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }
        }
        else {
            buf.append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
        }
        return buf.toString();
    }

    private String formatPersonParameterAsCSV(Person person, boolean normalization) {
        MaterialParameter blank = new MaterialParameter(Label.Unknown);

        StringBuilder buf = new StringBuilder();
        if (null != person) {
            if (null != person.getBraids()) {
                buf.append(new MaterialParameter(person.getBraids().get(0)).formatCSV(normalization));
            } else {
                buf.append(blank.formatCSV(normalization));
            }

            if (null != person.getHead()) {
                buf.append(",").append(new MaterialParameter(person.getHead()).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (person.hasHair()) {
                buf.append(",").append(new MaterialParameter(person.getHairs().get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (person.hasCap()) {
                buf.append(",").append(new MaterialParameter(person.getCap()).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (person.hasEye()) {
                buf.append(",").append(new MaterialParameter(person.getEyes().get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (person.hasEyebrow()) {
                buf.append(",").append(new MaterialParameter(person.getEyebrows().get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (person.hasNose()) {
                buf.append(",").append(new MaterialParameter(person.getNose()).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (person.hasEar()) {
                buf.append(",").append(new MaterialParameter(person.getEars().get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (person.hasMouth()) {
                buf.append(",").append(new MaterialParameter(person.getMouth()).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (person.hasBody()) {
                buf.append(",").append(new MaterialParameter(person.getBody()).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (person.hasArm()) {
                buf.append(",").append(new MaterialParameter(person.getArms().get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (person.hasPalm()) {
                buf.append(",").append(new MaterialParameter(person.getPalms().get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (person.hasLeg()) {
                buf.append(",").append(new MaterialParameter(person.getLegs().get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (person.hasFoot()) {
                buf.append(",").append(new MaterialParameter(person.getFoot().get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != person.getMask()) {
                buf.append(",").append(new MaterialParameter(person.getMask()).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (person.hasHairAccessory()) {
                buf.append(",").append(new MaterialParameter(person.getHairAccessories().get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != person.getSkirt()) {
                buf.append(",").append(new MaterialParameter(person.getSkirt()).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != person.getItems()) {
                buf.append(",").append(new MaterialParameter(person.getItems().get(0)).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }

            if (null != person.getGlasses()) {
                buf.append(",").append(new MaterialParameter(person.getGlasses()).formatCSV(normalization));
            } else {
                buf.append(",").append(blank.formatCSV(normalization));
            }
        }
        else {
            buf.append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
            buf.append(",").append(blank.formatCSV(normalization));
        }
        return buf.toString();
    }

    private void build() {
        this.parameter = new Parameter();
        SpaceLayout spaceLayout = new SpaceLayout(this.painting);

        this.parameter.frameAreaRatio = spaceLayout.getAreaRatio();

        this.parameter.wholeTextureMax = this.painting.getWhole().max;
        this.parameter.wholeTextureAvg = this.painting.getWhole().avg;
        this.parameter.wholeTextureDensity = this.painting.getWhole().density;
        this.parameter.wholeTextureHierarchy = this.painting.getWhole().hierarchy;
        this.parameter.wholeTextureStandardDeviation = this.painting.getWhole().standardDeviation;

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
            this.house1 = this.painting.getHouse();
            this.parameter.house1 = new MaterialParameter(this.painting.getHouse());
            if (this.painting.getHouses().size() > 1) {
                this.house2 = this.painting.getHouses().get(0);
                // 默认列表是面积正序，所以这里获取列表里第一个，面积最小的一个
                this.parameter.house2 = new MaterialParameter(this.painting.getHouses().get(0));
            }
            else {
                this.parameter.house2 = new MaterialParameter(Label.House);
            }
        }
        else {
            this.parameter.house1 = new MaterialParameter(Label.House);
            this.parameter.house2 = new MaterialParameter(Label.House);
        }

        if (this.painting.hasTree()) {
            this.parameter.tree1 = new MaterialParameter(this.painting.getTree());
            if (this.painting.getTrees().size() > 1) {
                // 默认列表是面积正序，所以这里获取列表里第一个，面积最小的一个
                this.parameter.tree2 = new MaterialParameter(this.painting.getTrees().get(0));
            }
            else {
                this.parameter.tree2 = new MaterialParameter(Label.Tree);
            }
        }
        else {
            this.parameter.tree1 = new MaterialParameter(Label.Tree);
            this.parameter.tree2 = new MaterialParameter(Label.Tree);
        }

        if (this.painting.hasPerson()) {
            this.parameter.person1 = new MaterialParameter(this.painting.getPerson());
            if (this.painting.getPersons().size() > 1) {
                // 默认列表是面积正序，所以这里获取列表里第一个，面积最小的一个
                this.parameter.person2 = new MaterialParameter(this.painting.getPersons().get(0));
            }
            else {
                this.parameter.person2 = new MaterialParameter(Label.Person);
            }
        }
        else {
            this.parameter.person1 = new MaterialParameter(Label.Person);
            this.parameter.person2 = new MaterialParameter(Label.Person);
        }

        OtherSet otherSet = this.painting.getOther();
        // 将所有标签都加入
        for (Label label : Label.values()) {
            if (Label.isOther(label)) {
                Thing thing = otherSet.get(label);
                if (null != thing) {
                    this.parameter.materials.add(new MaterialParameter(thing));
                }
                else {
                    // 不存在的标签
                    this.parameter.materials.add(new MaterialParameter(label));
                }
            }
        }
    }

    private String formatMaterialParameterCSVHead(String prefix) {
        StringBuilder buf = new StringBuilder();
        buf.append(prefix).append("_").append("center_x");
        buf.append(",").append(prefix).append("_").append("center_y");
        buf.append(",").append(prefix).append("_").append("location_x");
        buf.append(",").append(prefix).append("_").append("location_y");
        buf.append(",").append(prefix).append("_").append("size_width");
        buf.append(",").append(prefix).append("_").append("size_height");
        buf.append(",").append(prefix).append("_").append("area_ratio");
        buf.append(",").append(prefix).append("_").append("t_max");
        buf.append(",").append(prefix).append("_").append("t_avg");
        buf.append(",").append(prefix).append("_").append("t_density");
        buf.append(",").append(prefix).append("_").append("t_hierarchy");
        buf.append(",").append(prefix).append("_").append("t_sd");
        return buf.toString();
    }

    public class Parameter {

        public double frameAreaRatio = 0;

        public double wholeTextureMax = 0;
        public double wholeTextureAvg = 0;
        public double wholeTextureDensity = 0;
        public double wholeTextureHierarchy = 0;
        public double wholeTextureStandardDeviation = 0;

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

        public Point location;

        public Size size;

        public double areaRatio = 0;

        public double textureMax = 0;

        public double textureAvg = 0;

        public double textureHierarchy = 0;

        public double textureDensity = 0;

        public double textureStandardDeviation = 0;

        public MaterialParameter(Label label) {
            this.label = label;
            this.center = new Point(0, 0);
            this.location = new Point(0, 0);
            this.size = new Size(0, 0);
        }

        public MaterialParameter(Material material) {
            this.label = Label.parse(material.label);

            Size size = painting.getCanvasSize();
            double r = scaleSize / size.width;

            double cpX = material.box.getCenterPoint().x * r;
            double cpY = material.box.getCenterPoint().y * r;
            this.center = new Point(cpX, cpY);

            this.location = new Point(material.box.x0 * r, material.box.y0 * r);
            this.size = new Size((int)(r * (double) material.box.width), (int)(r * (double) material.box.height));

            this.areaRatio = ((double) material.area) / (double) painting.getCanvasSize().calculateArea();

            this.textureMax = material.texture.max;
            this.textureAvg = material.texture.avg;
            this.textureDensity = material.texture.density;
            this.textureHierarchy = material.texture.hierarchy;
            this.textureStandardDeviation = material.texture.standardDeviation;
        }

        public double[] parameters(boolean normalization) {
            double[] data = new double[12];
            data[0] = normalization ? this.center.x * aligningSize : this.center.x;
            data[1] = normalization ? this.center.y * aligningSize : this.center.y;
            data[2] = normalization ? this.location.x * aligningSize : this.location.x;
            data[3] = normalization ? this.location.y * aligningSize : this.location.y;
            data[4] = normalization ? this.size.width * aligningSize : this.size.width;
            data[5] = normalization ? this.size.height * aligningSize : this.size.height;
            data[6] = normalization ? this.areaRatio * aligningAreaRatio : this.areaRatio;
            data[7] = normalization ? this.textureMax * aligningPixel : this.textureMax;
            data[8] = normalization ? this.textureAvg * aligningPixel : this.textureAvg;
            data[9] = normalization ? this.textureDensity * aligningDensity : this.textureDensity;
            data[10] = normalization ? this.textureHierarchy * aligningHierarchy : this.textureHierarchy;
            data[11] = normalization ? this.textureStandardDeviation * aligningSd : this.textureStandardDeviation;
            return data;
        }

        public String formatCSV(boolean normalization) {
            StringBuilder buf = new StringBuilder();
            buf.append(normalization ? this.center.x * aligningSize : this.center.x);
            buf.append(",").append(normalization ? this.center.y * aligningSize : this.center.y);
            buf.append(",").append(normalization ? this.location.x * aligningSize : this.location.x);
            buf.append(",").append(normalization ? this.location.y * aligningSize : this.location.y);
            buf.append(",").append(normalization ? this.size.width * aligningSize : this.size.width);
            buf.append(",").append(normalization ? this.size.height * aligningSize : this.size.height);
            buf.append(",").append(normalization ? this.areaRatio * aligningAreaRatio : this.areaRatio);
            buf.append(",").append(normalization ? this.textureMax * aligningPixel : this.textureMax);
            buf.append(",").append(normalization ? this.textureAvg * aligningPixel : this.textureAvg);
            buf.append(",").append(normalization ? this.textureDensity * aligningDensity : this.textureDensity);
            buf.append(",").append(normalization ? this.textureHierarchy * aligningHierarchy : this.textureHierarchy);
            buf.append(",").append(normalization ? this.textureStandardDeviation * aligningSd : this.textureStandardDeviation);
            return buf.toString();
        }
    }
}
