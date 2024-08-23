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

package cube.aigc.psychology.algorithm;

import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.composition.ReportRelation;
import cube.vision.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * 大五人格分析。
 */
public class BigFiveRevolver {

    public BigFiveRevolver() {
    }

    public Panorama generatePanorama(List<ReportRelation> relations, List<PaintingReport> reports) {
        Panorama panorama = new Panorama();

        for (int i = 0; i < reports.size(); ++i) {
            PaintingReport report = reports.get(i);
            Point c = this.calcCoordinate(report.getEvaluationReport().getPersonalityAccelerator().getBigFiveFeature());

        }

        return panorama;
    }

    private Point calcCoordinate(BigFiveFeature feature) {
        Point pObligingness = new Point(0, -feature.getObligingness());
        Point pExtraversion = new Point(-feature.getExtraversion(), 0);
        Point pAchievement = new Point(0, feature.getAchievement());
        Point pConscientiousness = new Point(feature.getConscientiousness(), 0);
        return Point.getCentroid(new Point[] { pObligingness, pExtraversion, pAchievement, pConscientiousness });
    }


    public class Panorama {

        private List<Point> coordinates = new ArrayList<>();

//        private

        public Panorama() {
        }
    }
}
