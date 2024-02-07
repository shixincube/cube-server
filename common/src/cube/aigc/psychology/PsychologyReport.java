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

import cube.common.JSONable;
import cube.common.entity.AIGCChannel;
import cube.common.entity.FileLabel;
import org.json.JSONObject;

/**
 * 心理学报告。
 */
public class PsychologyReport implements JSONable {

    public final static String PHASE_PREDICT = "PREDICT";

    public final static String PHASE_PREDICT_FAILED = "PREDICT_FAILED";

    public final static String PHASE_INFER = "INFER";

    public final static String PHASE_INFER_FAILED = "INFER_FAILED";

    public final static String PHASE_FINISH = "FINISH";


    private String phase;

    private FileLabel fileLabel;

    private Theme theme;

    private AIGCChannel channel;

    private Workflow workflow;

    public PsychologyReport(FileLabel fileLabel, Theme theme, AIGCChannel channel) {
        this.phase = PHASE_PREDICT;
        this.fileLabel = fileLabel;
        this.theme = theme;
        this.channel = channel;
    }

    public PsychologyReport(JSONObject json) {
        this.phase = json.getString("phase");
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    public void resetPhase(String phase) {
        this.phase = phase;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = this.toCompactJSON();
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("phase", this.phase);
        json.put("fileLabel", this.fileLabel.toCompactJSON());
        json.put("theme", this.theme.name);
        return json;
    }
}
