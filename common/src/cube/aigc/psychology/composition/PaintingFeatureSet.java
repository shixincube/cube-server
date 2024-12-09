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

import cell.util.log.Logger;
import cube.aigc.psychology.EvaluationFeature;
import cube.aigc.psychology.Term;
import cube.aigc.psychology.algorithm.Representation;
import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * 特征集。
 */
public class PaintingFeatureSet implements JSONable {

    private long sn;

    private long timestamp = System.currentTimeMillis();

    /**
     * Key: 描述
     */
    private Map<String, List<Representation>> dataMap = new LinkedHashMap<>();

    public PaintingFeatureSet(List<EvaluationFeature> features, List<Representation> representations) {
        for (EvaluationFeature feature : features) {
            this.add(feature, representations);
        }
    }

    public PaintingFeatureSet(JSONObject json) {
        if (json.has("sn")) {
            this.sn = json.getLong("sn");
        }

        if (json.has("timestamp")) {
            this.timestamp = json.getLong("timestamp");
        }

        JSONArray data = json.getJSONArray("data");
        for (int i = 0; i < data.length(); ++i) {
            JSONObject item = data.getJSONObject(i);

            try {
                String desc = item.getString("desc");

                JSONArray list = item.getJSONArray("list");
                List<Representation> representations = new ArrayList<>();
                for (int n = 0; n < list.length(); ++n) {
                    JSONObject representationJson = list.getJSONObject(n);
                    Representation representation = new Representation(representationJson);
                    representations.add(representation);
                }

                this.dataMap.put(desc, representations);
            } catch (Exception e) {
                Logger.e(this.getClass(), "", e);
            }
        }
    }

    private void add(EvaluationFeature evaluationFeature, List<Representation> representations) {
        for (EvaluationFeature.Feature feature : evaluationFeature.getFeatures()) {
            if (null == feature.description || feature.description.length() == 0) {
                continue;
            }

            List<Representation> list = this.dataMap.get(feature.description);
            if (null == list) {
                list = new ArrayList<>();
                this.dataMap.put(feature.description, list);
            }

            for (Representation representation : representations) {
                if (representation.knowledgeStrategy.getTerm() == feature.term) {
                    list.add(representation);
                    break;
                }
            }
        }
    }

    public void setSN(long sn) {
        this.sn = sn;
    }

    public long getSN() {
        return this.sn;
    }

    public String makePrompt(boolean knowledgeStrategy) {
        StringBuilder buf = new StringBuilder();

        buf.append("绘画画面的心理特征如下：\n");
        Iterator<Map.Entry<String, List<Representation>>> iter = this.dataMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, List<Representation>> e = iter.next();
            buf.append("* ").append(e.getKey()).append("。");
            buf.append("该特征说明受测人有以下心理特征：");
            List<Representation> list = e.getValue();
            for (Representation representation : list) {
                buf.append(representation.description).append("，");
            }
            buf.delete(buf.length() - 1, buf.length());
            buf.append("。\n");
        }

        if (knowledgeStrategy) {
            buf.append("\n");
            buf.append("相关心理特征释义如下：\n\n");
            iter = this.dataMap.entrySet().iterator();
            List<Term> terms = new ArrayList<>();
            while (iter.hasNext()) {
                List<Representation> list = iter.next().getValue();
                for (Representation representation : list) {
                    if (terms.contains(representation.knowledgeStrategy.getTerm())) {
                        continue;
                    }
                    terms.add(representation.knowledgeStrategy.getTerm());

                    buf.append("* **").append(representation.knowledgeStrategy.getTerm().word).append("**");
                    buf.append("的心理学释义是");
                    buf.append(representation.knowledgeStrategy.getInterpretation());
                    buf.append("。\n");
                }
            }
        }

        buf.append("\n");

        Logger.d(this.getClass(), "#makePrompt - Words: " + buf.length());

        return buf.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn);
        json.put("timestamp", this.timestamp);

        JSONArray array = new JSONArray();

        Iterator<Map.Entry<String, List<Representation>>> iter = this.dataMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, List<Representation>> e = iter.next();

            JSONObject item = new JSONObject();
            item.put("desc", e.getKey());

            JSONArray list = new JSONArray();
            for (Representation representation : e.getValue()) {
                list.put(representation.toJSON());
            }
            item.put("list", list);

            array.put(item);
        }

        json.put("data", array);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
