package cube.service.test;

import cube.aigc.Prompt;
import cube.aigc.PromptBuilder;
import cube.aigc.PromptChaining;
import cube.aigc.psychology.composition.PsychologicalHealth;

import java.io.File;
import java.util.List;

public class TestThing {

    public static void testPromptBuilder() {
        File file = new File("assets/prompt/psychology_01.json");
        PromptBuilder builder = new PromptBuilder();
        String prompt = builder.serializeFromFile(file);
        System.out.println(prompt);
    }

    public static void testPsychologicalHealth() {
//        List<Prompt> prompts = PsychologicalHealth.makeInterpersonalRelationships();
//        List<Prompt> prompts = PsychologicalHealth.makeDepression();
//        List<Prompt> prompts = PsychologicalHealth.makeAnxiety();
//        List<Prompt> prompts = PsychologicalHealth.makePhobia();
        List<Prompt> prompts = PsychologicalHealth.makeParanoia();
        System.out.println("num: " + prompts.size());

        PromptChaining chaining = new PromptChaining();
        chaining.addPrompts(prompts);
        System.out.println("words: " + chaining.getWordNum());

        PromptBuilder builder = new PromptBuilder();
        String prompt = builder.serializePromptChaining(chaining);
        System.out.println("prompt:\n" + prompt);
    }

    public static void main(String[] args) {
//        testPromptBuilder();
        testPsychologicalHealth();
    }
}
