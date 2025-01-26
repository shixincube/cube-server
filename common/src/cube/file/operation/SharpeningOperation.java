/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.file.operation;

import cube.file.ImageOperation;
import org.json.JSONObject;

/**
 * 锐化操作。
 */
public class SharpeningOperation extends ImageOperation {

    public final static String Operation = "Sharpening";

    private double sigma;

    public SharpeningOperation(double sigma) {
        this.sigma = sigma;
    }

    public SharpeningOperation(JSONObject json) {
        super(json);
        this.sigma = json.getDouble("sigma");
    }

    public double getSigma() {
        return this.sigma;
    }

    public String formatSigma() {
        return String.format("%.1f", this.sigma);
    }

    @Override
    public String getOperation() {
        return Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("sigma", this.sigma);
        return json;
    }
}
