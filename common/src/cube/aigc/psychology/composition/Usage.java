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

package cube.aigc.psychology.composition;

import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.ScaleReport;
import cube.auth.AuthToken;
import cube.common.JSONable;
import org.json.JSONObject;

public class Usage implements JSONable {

    public final static String QUERY_TYPE_PAINTING = "painting";

    public final static String QUERY_TYPE_SCALE = "scale";

    public long cid;

    public String token;

    public long timestamp;

    public String remoteHost;

    public String query;

    public String queryType;

    public long queryTokens;

    public long completionTokens;

    public long completionSN;

    public Usage(AuthToken authToken, String remoteHost, PaintingReport report) {
        this.cid = authToken.getContactId();
        this.token = authToken.getCode();
        this.timestamp = report.timestamp;
        this.remoteHost = remoteHost;
        this.query = report.getFileCode();
        this.queryType = QUERY_TYPE_PAINTING;
        this.queryTokens = report.painting.getAllThings().size();
        this.completionTokens = report.getSummary().length();
        this.completionSN = report.sn;
    }

    public Usage(AuthToken authToken, String remoteHost, ScaleReport report) {
        this.cid = authToken.getContactId();
        this.token = authToken.getCode();
        this.timestamp = report.timestamp;
        this.remoteHost = remoteHost;
        this.query = "";
        this.queryType = QUERY_TYPE_SCALE;
        this.queryTokens = report.getFactors().size();
        this.completionTokens = report.getSummary().length();
        this.completionSN = report.sn;
    }

    public Usage() {
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("cid", this.cid);
        json.put("token", this.token);
        json.put("timestamp", this.timestamp);
        json.put("remoteHost", this.remoteHost);
        json.put("query", this.query);
        json.put("queryType", this.queryType);
        json.put("queryTokens", this.queryTokens);
        json.put("completionTokens", this.completionTokens);
        json.put("completionSN", this.completionSN);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
