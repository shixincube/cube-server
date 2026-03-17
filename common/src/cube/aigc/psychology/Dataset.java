/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

import cell.util.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Dataset {

    private Map<String, Content> contentMap;

    private Map<String, String[]> questionKeywordMap;

    private Map<String, String[]> questionKeywordSequenceMap;

    public Dataset(JSONArray array) {
        this.contentMap = new ConcurrentHashMap<>();
        this.questionKeywordMap = new ConcurrentHashMap<>();
        this.questionKeywordSequenceMap = new ConcurrentHashMap<>();
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
                    Content content = new Content();
                    for (int j = 0; j < answers.length(); ++j) {
                        content.addText(answers.getString(j));
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

    public void fillQuestionKeywords(String question, String[] keywords, String[] keywordSequence) {
        this.questionKeywordMap.put(question, keywords);
        this.questionKeywordSequenceMap.put(question, keywordSequence);
    }

    public String matchContent(String[] keywords, int length) {
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

    /**
     * 根据关键词检索内容。
     *
     * @param keywords 关键词列表。
     * @param matching 匹配数量。
     * @return
     */
    public List<String> searchContent(String[] keywords, int matching) {
        List<String> result = new ArrayList<>();

        Iterator<Map.Entry<String, String[]>> iter = this.questionKeywordMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String[]> entry = iter.next();
            String[] queryWords = entry.getValue();
            int count = 0;
            for (String keyword : keywords) {
                for (String qw : queryWords) {
                    if (keyword.equals(qw)) {
                        ++count;
                        break;
                    }
                }
            }
            if (count >= matching) {
                String content = this.getContent(entry.getKey());
                if (null != content) {
                    result.add(content);
                }
            }
        }

        return result;
    }

    /**
     * 根据关键词检索内容和顺序。
     *
     * @param keywords
     * @param matching
     * @return
     */
    public List<String> searchContentInOrder(String[] keywords, int matching) {
        List<String> result = new ArrayList<>();

        Iterator<Map.Entry<String, String[]>> iter = this.questionKeywordSequenceMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String[]> entry = iter.next();
            String[] queryWords = entry.getValue();

            List<String> candidate = new ArrayList<>();
            for (String qw : queryWords) {
                for (String keyword : keywords) {
                    if (qw.equals(keyword)) {
                        candidate.add(qw);
                        break;
                    }
                }
            }

            // 排除重复内容
            int count = 0;
            for (String keyword : keywords) {
                for (String cw : candidate) {
                    if (keyword.equalsIgnoreCase(cw)) {
                        ++count;
                        break;
                    }
                }
            }

            if (count < keywords.length) {
                continue;
            }

            boolean isOrder = true;
            // 判断顺序
            List<Integer> orderList = new ArrayList<>();
            List<String> processed = new ArrayList<>();
            if (candidate.size() >= matching) {
                for (String word : candidate) {
                    if (processed.contains(word)) {
                        continue;
                    }
                    processed.add(word);

                    for (int i = 0; i < keywords.length; ++i) {
                        if (word.equalsIgnoreCase(keywords[i])) {
                            orderList.add(i);
                            break;
                        }
                    }
                }

                for (int i = 0; i < orderList.size(); ++i) {
                    int current = orderList.get(i);
                    if (i + 1 < orderList.size()) {
                        int next = orderList.get(i + 1);
                        if (current > next) {
                            isOrder = false;
                            break;
                        }
                    }
                }
            }
            else {
                isOrder = false;
            }

            if (isOrder) {
                String content = this.getContent(entry.getKey());
                if (null != content) {
                    result.add(content);
                }
            }
        }

        return result;
    }

    public String getContent(String question) {
        Content content = this.contentMap.get(question);
        if (null == content) {
            return null;
        }

        return content.getText();
    }

    public class Content {

        private List<String> textList = new ArrayList<>();

        public Content() {
        }

        public String getText() {
            if (this.textList.size() == 1) {
                return this.textList.get(0);
            }
            else {
                return this.textList.get(Utils.randomInt(0, this.textList.size() - 1));
            }
        }

        public void addText(String text) {
            this.textList.add(text);
        }
    }
}
