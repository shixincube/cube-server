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

package cube.common.entity;

import cell.util.Utils;
import org.json.JSONObject;

/**
 * 知识释义。
 */
public class KnowledgeParaphrase extends Entity {

    private long parentId;

    private String category;

    private String word;

    private String paraphrase;

    public KnowledgeParaphrase(String category, String word, String paraphrase) {
        super(Utils.generateSerialNumber());
        this.category = category;
        this.word = word;
        this.paraphrase = paraphrase;
    }

    public KnowledgeParaphrase(JSONObject json) {
        super(json);
        this.category = json.getString("category");
        this.word = json.getString("word");
        this.paraphrase = json.getString("paraphrase");
    }

    public String getCategory() {
        return this.category;
    }

    public String getWord() {
        return this.word;
    }

    public String getParaphrase() {
        return this.paraphrase;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("category", this.category);
        json.put("word", this.word);
        json.put("paraphrase", this.paraphrase);
        return json;
    }
}
