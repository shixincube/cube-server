/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

import cube.aigc.psychology.PaintingReport;
import cube.vision.Point;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 大五人格分析。
 */
public class BigFiveRevolver {

    public BigFiveRevolver() {
    }

    public Panorama generatePanorama(List<PaintingReport> reports) {
        Panorama panorama = new Panorama();

        for (int i = 0; i < reports.size(); ++i) {
            PaintingReport report = reports.get(i);
            // 计算全景坐标
            Point coordinate = this.calcPanoramaCoordinate(
                    report.getEvaluationReport().getPersonalityAccelerator().getBigFivePersonality());
            // 计算象限
            BigFivePanoramaQuadrant quadrant = BigFivePanoramaQuadrant.parse(coordinate.x, coordinate.y);
            // 计算工作环
            BigFiveWorkingLoop loop = BigFiveWorkingLoop.parse(coordinate);
            // 计算团队合作风格
            BigFiveTeamworkStyle style = BigFiveTeamworkStyle.parse(coordinate);
            panorama.participants.add(new Participant(report, coordinate, quadrant, loop, style));
        }

        return panorama;
    }

    private Point calcPanoramaCoordinate(BigFivePersonality feature) {
        Point pObligingness = new Point(0, -feature.getObligingness());
        Point pExtraversion = new Point(-feature.getExtraversion(), 0);
        Point pAchievement = new Point(0, feature.getAchievement());
        Point pConscientiousness = new Point(feature.getConscientiousness(), 0);
        Point centroid = Point.getCentroid(new Point[] { pObligingness, pExtraversion, pAchievement, pConscientiousness });
        // 将坐标缩放到 10x10 范围
        centroid = centroid.scale(3.9);
        return centroid;
    }


    public class Panorama {

        private List<Participant> participants = new ArrayList<>();

        public Panorama() {
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            JSONArray participants = new JSONArray();
            for (int i = 0; i < this.participants.size(); ++i) {
                participants.put(this.participants.get(i).toJSON());
            }
            json.put("participants", participants);
            return json;
        }
    }

    public class Participant {

        public PaintingReport report;

        public Point coordinate;

        public BigFivePanoramaQuadrant quadrant;

        public BigFiveWorkingLoop workingLoop;

        public BigFiveTeamworkStyle teamworkStyle;

        public Participant(PaintingReport report, Point coordinate, BigFivePanoramaQuadrant quadrant,
                       BigFiveWorkingLoop workingLoop, BigFiveTeamworkStyle teamworkStyle) {
            this.report = report;
            this.coordinate = coordinate;
            this.quadrant = quadrant;
            this.workingLoop = workingLoop;
            this.teamworkStyle = teamworkStyle;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("sn", this.report.sn);
            json.put("coordinate", this.coordinate.toJSON());
            json.put("quadrant", this.quadrant.toJSONArray());
            json.put("workingLoop", this.workingLoop.name);
            json.put("teamworkStyle", this.teamworkStyle.name);
            return json;
        }
    }
}
