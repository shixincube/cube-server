package cube.service.test;

import cube.service.tokenizer.Tokenizer;

import java.util.List;

public class TestFastPrompt {

    public void testPersonalityPrompt() {
        Tokenizer tokenizer = new Tokenizer();

        String[] sentences = new String[] {
                "如何进行绘画评测",
                "高分宜人性表现",
                "低分宜人性表现",
                "宜人性一般的表现",
                "高分尽责性表现",
                "低分尽责性表现",
                "尽责性一般的表现",
                "高分外向性表现",
                "低分外向性表现",
                "外向性一般的表现",
                "高分进取性表现",
                "低分进取性表现",
                "进取性一般的表现",
                "高分情绪性表现",
                "低分情绪性表现",
                "情绪性一般的表现"
        };

        for (String sentence : sentences) {
            List<String> result = tokenizer.sentenceProcess(sentence);
            System.out.println(result.toString());
        }
    }

    public static void main(String[] args) {
        TestFastPrompt test = new TestFastPrompt();
        test.testPersonalityPrompt();
    }
}
