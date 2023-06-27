package cube.service.tokenizer.keyword;

import cube.service.tokenizer.Tokenizer;

import java.io.*;
import java.util.*;

/**
 * tfidf算法原理参考：http://www.cnblogs.com/ywl925/p/3275878.html
 */
public class TFIDFAnalyzer {

    private static HashMap<String, Double> idfMap;
    private static HashSet<String> stopWordsSet;
    private static double idfMedian;

    private Tokenizer tokenizer;

    public TFIDFAnalyzer() {
        this.tokenizer = new Tokenizer();
    }

    public TFIDFAnalyzer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    /**
     * tfidf分析方法
     *
     * @param content 需要分析的文本/文档内容
     * @param topN    需要返回的tfidf值最高的N个关键词，若超过content本身含有的词语上限数目，则默认返回全部
     * @return
     */
    public List<Keyword> analyze(String content, int topN) {
        List<Keyword> keywordList = new ArrayList<>();

        if (stopWordsSet == null) {
            stopWordsSet = new HashSet<>();
            try {
                loadStopWords(stopWordsSet, new FileInputStream(new File("assets/tokenizer/stop_words.txt")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (idfMap == null) {
            idfMap = new HashMap<>();
            try {
                loadIDFMap(idfMap, new FileInputStream("assets/tokenizer/idf_dict.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Map<String, Double> tfMap = getTF(content);
        for (String word : tfMap.keySet()) {
            // 若该词不在idf文档中，则使用平均的idf值(可能定期需要对新出现的网络词语进行纳入)
            if (idfMap.containsKey(word)) {
                keywordList.add(new Keyword(word, idfMap.get(word) * tfMap.get(word)));
            } else {
                keywordList.add(new Keyword(word, idfMedian * tfMap.get(word)));
            }
        }

        Collections.sort(keywordList);

        if (keywordList.size() > topN) {
            int num = keywordList.size() - topN;
            for (int i = 0; i < num; i++) {
                keywordList.remove(topN);
            }
        }
        return keywordList;
    }

    /**
     * tf值计算公式
     * tf=N(i,j)/(sum(N(k,j) for all k))
     * N(i,j)表示词语Ni在该文档d（content）中出现的频率，sum(N(k,j))代表所有词语在文档d中出现的频率之和
     *
     * @param content
     * @return
     */
    private Map<String, Double> getTF(String content) {
        Map<String, Double> tfMap = new HashMap<>();
        if (content == null || content.equals(""))
            return tfMap;

        List<String> segments = this.tokenizer.sentenceProcess(content);
        Map<String, Integer> freqMap = new HashMap<>();

        int wordSum = 0;
        for (String segment : segments) {
            //停用词不予考虑，单字词不予考虑
            if (!stopWordsSet.contains(segment) && segment.length() > 1) {
                wordSum++;
                if (freqMap.containsKey(segment)) {
                    freqMap.put(segment, freqMap.get(segment) + 1);
                } else {
                    freqMap.put(segment, 1);
                }
            }
        }

        // 计算double型的tf值
        for (String word : freqMap.keySet()) {
            tfMap.put(word, freqMap.get(word) * 0.1 / wordSum);
        }

        return tfMap;
    }

    /**
     * 默认jieba分词的停词表
     * url:https://github.com/yanyiwu/nodejieba/blob/master/dict/stop_words.utf8
     *
     * @param set
     * @param in
     */
    private void loadStopWords(Set<String> set, InputStream in) {
        BufferedReader bufr;
        try {
            bufr = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = bufr.readLine()) != null) {
                set.add(line.trim());
            }
            try {
                bufr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * idf值本来需要语料库来自己按照公式进行计算，不过jieba分词已经提供了一份很好的idf字典，所以默认直接使用jieba分词的idf字典
     * url:https://raw.githubusercontent.com/yanyiwu/nodejieba/master/dict/idf.utf8
     *
     * @param map
     * @param in
     */
    private void loadIDFMap(Map<String, Double> map, InputStream in) {
        BufferedReader bufr;
        try {
            bufr = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = bufr.readLine()) != null) {
                String[] kv = line.trim().split(" ");
                map.put(kv[0], Double.parseDouble(kv[1]));
            }
            try {
                bufr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 计算idf值的中位数
            List<Double> idfList = new ArrayList<>(map.values());
            Collections.sort(idfList);
            idfMedian = idfList.get(idfList.size() / 2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

