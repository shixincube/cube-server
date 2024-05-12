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
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.json.JSONObject;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 量表。
 */
public class Scale extends Questionnaire {

    private File structureFile;

    public Scale(File structureFile) {
        this.structureFile = structureFile;
        this.build(this.readJsonFile(structureFile));
    }

    public JSONObject scoring() {
        Path scoringScriptFile = Paths.get(this.structureFile.getParent(), this.scoringScript);
        if (!Files.exists(scoringScriptFile)) {
            Logger.w(this.getClass(), "#scoring - Scoring script file error: "
                    + scoringScriptFile.toFile().getAbsolutePath());
            return null;
        }

        StringBuilder script = new StringBuilder();
        script.append("var ScaleScore = Java.type('cube.aigc.psychology.composition.ScaleScore');\n");
        try {
            script.append(new String(Files.readAllBytes(scoringScriptFile), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Logger.e(this.getClass(), "#scoring", e);
            return null;
        }

        ScriptObjectMirror returnVal = null;
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        try {
            engine.eval(script.toString());
            Invocable invocable = (Invocable) engine;
            returnVal = (ScriptObjectMirror) invocable.invokeFunction("scoring", this.getAllChosenAnswers());
        } catch (ScriptException | NoSuchMethodException e) {
            Logger.e(this.getClass(), "#scoring", e);
            return null;
        }

        if (null == returnVal) {
            Logger.w(this.getClass(), "#scoring - Return value is null: " + this.name);
            return null;
        }

        String result = "";
        ScaleScore scaleScore = new ScaleScore();
        if (returnVal.containsKey("result")) {
            result = returnVal.get("result").toString();
        }
        if (returnVal.containsKey("score")) {
            scaleScore = (ScaleScore) returnVal.get("score");
        }

        JSONObject json = new JSONObject();
        json.put("result", result);
        json.put("score", scaleScore.toJSON());
        return json;
    }

    /**
     * 读取指定 JSON 格式的文件。
     *
     * @param file
     * @return
     */
    private JSONObject readJsonFile(File file) {
        JSONObject json = null;
        try {
            byte[] data = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
            json = new JSONObject(new String(data, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Logger.w(this.getClass(), "#readJsonFile - Read file error", e);
        }
        return json;
    }
}
