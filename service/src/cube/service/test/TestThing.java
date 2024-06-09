package cube.service.test;

import cube.aigc.PromptBuilder;

import java.io.File;

public class TestThing {

    public static void testPromptBuilder() {
        File file = new File("assets/prompt/psychology_01.json");
        PromptBuilder builder = new PromptBuilder();
        String prompt = builder.serializeFromFile(file);
        System.out.println(prompt);
    }

    public static void main(String[] args) {
//        testPromptBuilder();
    }
}
