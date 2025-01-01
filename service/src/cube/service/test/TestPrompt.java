package cube.service.test;

import cube.aigc.psychology.Resource;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.algorithm.KnowledgeStrategy;

import java.util.List;

public class TestPrompt {

    public static void testKnowledgeStrategy() {
        System.out.println("testKnowledgeStrategy");

        List<KnowledgeStrategy> list = Resource.getInstance().getTermInterpretations();
        System.out.println("Size: " + list.size());
    }

    public static void testThemeData() {
        System.out.println("testThemeData");

//        Resource.getInstance().getThemeTemplate(Theme.Generic);
    }

    public static void main(String[] args) {
        testKnowledgeStrategy();
    }
}
