package cube.service.test;

import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.BigFiveFactor;

public class TestResource {

    public static void main(String[] args) {
        String result = Resource.getInstance().loadDataset().getContent(BigFiveFactor.Obligingness.name);
        System.out.println(result);

        result = Resource.getInstance().loadDataset().getContent(BigFiveFactor.Conscientiousness.name);
        System.out.println(result);

        result = Resource.getInstance().loadDataset().getContent(BigFiveFactor.Extraversion.name);
        System.out.println(result);

        result = Resource.getInstance().loadDataset().getContent(BigFiveFactor.Achievement.name);
        System.out.println(result);

        result = Resource.getInstance().loadDataset().getContent(BigFiveFactor.Neuroticism.name);
        System.out.println(result);
    }
}
