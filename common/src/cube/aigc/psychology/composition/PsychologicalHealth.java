/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

import cube.aigc.Prompt;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PsychologicalHealth {

    private final static String PATH = "assets/psychology/";

    private PsychologicalHealth() {
    }

    /**
     * 人际关系。
     *
     * @return
     */
    public static List<Prompt> makeInterpersonalRelationships() {
        return makePrompts("interpersonal_relationships.json");
    }

    /**
     * 抑郁。
     *
     * @return
     */
    public static List<Prompt> makeDepression() {
        return makePrompts("depression.json");
    }

    /**
     * 焦虑。
     *
     * @return
     */
    public static List<Prompt> makeAnxiety() {
        return makePrompts("anxiety.json");
    }

    /**
     * 恐惧。
     *
     * @return
     */
    public static List<Prompt> makePhobia() {
        return makePrompts("phobia.json");
    }

    /**
     * 偏执。
     *
     * @return
     */
    public static List<Prompt> makeParanoia() {
        return makePrompts("paranoia.json");
    }

    private static List<Prompt> makePrompts(String filename) {
        JSONArray array = readFile(filename);
        if (null == array) {
            return null;
        }
        List<Prompt> result = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            result.add(new Prompt(array.getJSONObject(i)));
        }
        return result;
    }

    private static JSONArray readFile(String filename) {
        JSONArray array = null;
        try {
            byte[] data = Files.readAllBytes(Paths.get(PATH, filename));
            String json = new String(data, StandardCharsets.UTF_8);
            array = new JSONArray(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return array;
    }
}
