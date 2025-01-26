/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.algorithm;

import cube.aigc.psychology.composition.BigFiveFactor;
import org.json.JSONArray;

public enum BigFivePanoramaQuadrant {

    /**
     * 宜人性/外向性。
     */
    ObligingnessExtraversion(-1, -1),

    /**
     * 外向性/进取性。
     */
    ExtraversionAchievement(-1, 1),

    /**
     * 进去性/尽责性。
     */
    AchievementConscientiousness(1, 1),

    /**
     * 尽责性/宜人性。
     */
    ConscientiousnessObligingness(1, -1)

    ;

    public final int xOri;

    public final int yOri;

    public final BigFiveFactor[] quadrant;

    BigFivePanoramaQuadrant(int xOri, int yOri) {
        this.xOri = xOri;
        this.yOri = yOri;
        if (xOri < 0 && yOri < 0) {
            this.quadrant = new BigFiveFactor[] { BigFiveFactor.Obligingness, BigFiveFactor.Extraversion };
        }
        else if (xOri < 0 && yOri > 0) {
            this.quadrant = new BigFiveFactor[] { BigFiveFactor.Extraversion, BigFiveFactor.Achievement };
        }
        else if (xOri > 0 && yOri > 0) {
            this.quadrant = new BigFiveFactor[] { BigFiveFactor.Achievement, BigFiveFactor.Conscientiousness };
        }
        else {
            this.quadrant = new BigFiveFactor[] { BigFiveFactor.Conscientiousness, BigFiveFactor.Obligingness };
        }
    }

    public JSONArray toJSONArray() {
        JSONArray quadrant = new JSONArray();
        quadrant.put(this.quadrant[0].toJSON());
        quadrant.put(this.quadrant[1].toJSON());
        return quadrant;
    }

    public static BigFivePanoramaQuadrant parse(double x, double y) {
        for (BigFivePanoramaQuadrant bfp : BigFivePanoramaQuadrant.values()) {
            if (bfp.xOri < 0.0d && x < 0.0d && bfp.yOri < 0.0d && y < 0.0d) {
                return bfp;
            }
            else if (bfp.xOri > 0.0d && x > 0.0d && bfp.yOri < 0.0d && y < 0.0d) {
                return bfp;
            }
            else if (bfp.xOri < 0.0d && x < 0.0d && bfp.yOri > 0.0d && y > 0.0d) {
                return bfp;
            }
            else if (bfp.xOri > 0.0d && x > 0.0d && bfp.yOri > 0.0d && y > 0.0d) {
                return bfp;
            }
        }

        return ObligingnessExtraversion;
    }
}
