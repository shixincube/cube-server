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
import cube.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 量表配置信息。
 */
public class ScalesConfiguration {

    public final static String CATEGORY_PERSONALITY = "Personality";
    public final static String CATEGORY_EMOTION = "Emotion";
    public final static String CATEGORY_COGNITION = "Cognition";
    public final static String CATEGORY_BEHAVIOR = "Behavior";
    public final static String CATEGORY_INTERPERSONAL_RELATIONSHIP = "InterpersonalRelationship";
    public final static String CATEGORY_SELF_ASSESSMENT = "SelfAssessment";
    public final static String CATEGORY_MENTAL_HEALTH = "MentalHealth";
    public final static String CATEGORY_CAPACITY = "Capacity";

    private File configFile =  new File("./assets/psychology/scale.json");

    private Category personality;
    private Category emotion;
    private Category cognition;
    private Category behavior;
    private Category interpersonalRelationship;
    private Category selfAssessment;
    private Category mentalHealth;
    private Category capacity;

    public ScalesConfiguration() {
        this.load();
    }

    public Category getCategory(String category) {
        switch (category) {
            case CATEGORY_PERSONALITY:
                return this.personality;
            case CATEGORY_EMOTION:
                return this.emotion;
            case CATEGORY_COGNITION:
                return this.cognition;
            case CATEGORY_BEHAVIOR:
                return this.behavior;
            case CATEGORY_INTERPERSONAL_RELATIONSHIP:
                return this.interpersonalRelationship;
            case CATEGORY_SELF_ASSESSMENT:
                return this.selfAssessment;
            case CATEGORY_MENTAL_HEALTH:
                return this.mentalHealth;
            case CATEGORY_CAPACITY:
                return this.capacity;
            default:
                return null;
        }
    }

    private void load() {
        JSONObject data = null;
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(this.configFile.getAbsolutePath()));
            data = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (null == data) {
            Logger.w(this.getClass(), "#load - Read configuration file failed: " + this.configFile.getAbsolutePath());
            return;
        }

        JSONObject categoryJSON = data.getJSONObject("category");
        this.personality = new Category(categoryJSON.getJSONObject(CATEGORY_PERSONALITY));
        this.emotion = new Category(categoryJSON.getJSONObject(CATEGORY_EMOTION));
        this.cognition = new Category(categoryJSON.getJSONObject(CATEGORY_COGNITION));
        this.behavior = new Category(categoryJSON.getJSONObject(CATEGORY_BEHAVIOR));
        this.interpersonalRelationship = new Category(categoryJSON.getJSONObject(CATEGORY_INTERPERSONAL_RELATIONSHIP));
        this.selfAssessment = new Category(categoryJSON.getJSONObject(CATEGORY_SELF_ASSESSMENT));
        this.mentalHealth = new Category(categoryJSON.getJSONObject(CATEGORY_MENTAL_HEALTH));
        this.capacity = new Category(categoryJSON.getJSONObject(CATEGORY_CAPACITY));
    }


    public class Category {

        public final String name;

        public final List<Configuration> scales = new ArrayList<>();

        public Category(JSONObject json) {
            this.name = json.getString("name");

            JSONArray array = json.getJSONArray("scales");
            for (int i = 0; i < array.length(); ++i) {
                Configuration configuration = new Configuration(array.getJSONObject(i));
                this.scales.add(configuration);
            }
        }

        public Configuration find(String classification) {
            for (Configuration configuration : this.scales) {
                if (configuration.classification.contains(classification)) {
                    return configuration;
                }
            }

            return null;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("name", this.name);

            JSONArray array = new JSONArray();
            for (Configuration configuration : this.scales) {
                array.put(configuration.toJSON());
            }
            json.put("scales", array);

            return json;
        }
    }


    public class Configuration {

        public String classification;

        public List<String> factors;

        public String title;

        public String name;

        public Configuration(JSONObject json) {
            this.classification = json.getString("classification");
            this.factors = JSONUtils.toStringList(json.getJSONArray("factors"));
            this.title = json.getString("title");
            this.name = json.getString("name");
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("classification", this.classification);
            json.put("title", this.title);
            json.put("name", this.name);
            json.put("factors", JSONUtils.toStringArray(this.factors));
            return json;
        }
    }
}
