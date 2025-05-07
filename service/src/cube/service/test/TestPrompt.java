package cube.service.test;

import cube.aigc.Consts;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.algorithm.KnowledgeStrategy;
import cube.service.tokenizer.Tokenizer;
import cube.util.TextUtils;

import java.util.List;

public class TestPrompt {

    public static void testKnowledgeStrategy() {
        System.out.println("testKnowledgeStrategy");

        List<KnowledgeStrategy> list = Resource.getInstance().loadTermInterpretations();
        System.out.println("Size: " + list.size());
    }

    public static void testThemeData() {
        System.out.println("testThemeData");

//        Resource.getInstance().getThemeTemplate(Theme.Generic);
    }

    public static Attribute extractAttribute(String query) {
        Tokenizer tokenizer = new Tokenizer();

        int age = 0;
        String gender = "";

        List<String> words = tokenizer.sentenceProcess(query);
        int ageIndex = -1;
        int genderIndex = -1;
        for (int i = 0; i < words.size(); ++i) {
            String word = words.get(i);
            if (Consts.contains(word, Consts.AGE_SYNONYMS)) {
                ageIndex = i;
            }
            else if (Consts.contains(word, Consts.GENDER_SYNONYMS)) {
                genderIndex = i;
            }
        }

        if (ageIndex >= 0) {
            int start = Math.max(0, ageIndex - 2);
            for (int i = start; i < words.size(); ++i) {
                String word = words.get(i);
                word = word.trim();
                if (TextUtils.isNumeric(word)) {
                    try {
                        age = Integer.parseInt(word);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        else {
            for (int i = words.size() - 1; i >= 0; --i) {
                String word = words.get(i);
                word = word.trim();
                if (TextUtils.isNumeric(word)) {
                    try {
                        int value = Integer.parseInt(word);
                        if (value >= Attribute.MIN_AGE && value <= Attribute.MAX_AGE) {
                            age = value;
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (genderIndex >= 0) {
            int start = Math.max(0, genderIndex - 2);
            for (int i = start; i < words.size(); ++i) {
                String word = words.get(i);
                word = word.trim();
                if (word.equals("男") || word.equals("男性") || word.equalsIgnoreCase("male")) {
                    gender = "male";
                    break;
                }
                else if (word.equals("女") || word.equals("女性") || word.equalsIgnoreCase("female")) {
                    gender = "female";
                    break;
                }
            }
        }
        else {
            for (String word : words) {
                if (word.equals("男") || word.equals("男性") || word.equalsIgnoreCase("male")) {
                    gender = "male";
                    break;
                }
                else if (word.equals("女") || word.equals("女性") || word.equalsIgnoreCase("female")) {
                    gender = "female";
                    break;
                }
            }
        }

        return new Attribute(gender, age, false);
    }

    public static void main(String[] args) {
//        testKnowledgeStrategy();

        Attribute attribute = extractAttribute("男39");
        System.out.println("Attribute: \n" + attribute.toJSON().toString(4));
    }
}
