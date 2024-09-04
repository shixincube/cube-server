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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Dataset {

    private Map<String, Content> contentMap;

    private Map<String, String[]> questionKeywordMap;

    public Dataset(JSONArray array) {
        this.contentMap = new ConcurrentHashMap<>();
        this.questionKeywordMap = new ConcurrentHashMap<>();
        this.load(array);
    }

    private void load(JSONArray array) {
        for (int i = 0; i < array.length(); ++i) {
            JSONObject item = array.getJSONObject(i);

            if (item.has("questions") && item.has("answers")) {
                JSONArray answers = item.getJSONArray("answers");
                JSONArray questions = item.getJSONArray("questions");
                if (answers.length() > 0 && questions.length() > 0) {
                    // 内容
                    Content content = new Content(answers.getString(0));
                    if (answers.length() > 1) {
                        content.subContent = answers.getString(1);
                    }

                    // 问题
                    for (int j = 0; j < questions.length(); ++j) {
                        this.contentMap.put(questions.getString(j), content);
                    }
                }
            }
        }
    }

    public boolean hasAnalyzed() {
        return !this.questionKeywordMap.isEmpty();
    }

    public Collection<String> getQuestions() {
        return this.contentMap.keySet();
    }

    public void fillQuestionKeywords(String question, String[] keywords) {
        this.questionKeywordMap.put(question, keywords);
    }

    public String getContentByKeywords(String[] keywords, int length) {
        String question = null;
        Iterator<Map.Entry<String, String[]>> iter = this.questionKeywordMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String[]> entry = iter.next();
            String[] value = entry.getValue();

            if (Math.abs(value.length - keywords.length) > 1) {
                continue;
            }

            int hit = 0;
            for (int i = 0; i < value.length && i < keywords.length && i < length; ++i) {
                String cur = value[i];
                String word = keywords[i];
                if (cur.equalsIgnoreCase(word)) {
                    ++hit;
                }
            }

            if (hit == 1 && value.length == 1) {
                question = entry.getKey();
                break;
            }
            else if (hit > 0 && hit >= Math.min(value.length, keywords.length)) {
                question = entry.getKey();
                break;
            }
        }

        if (null == question) {
            return null;
        }

        return this.getContent(question);
    }

    public String getContent(String question) {
        Content content = this.contentMap.get(question);
        if (null == content) {
            return null;
        }

        return content.mainContent;
    }

    public class Content {

        public String mainContent;

        public String subContent;

        public Content(String mainContent) {
            this.mainContent = mainContent;
        }
    }
}
