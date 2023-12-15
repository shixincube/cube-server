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

package cube.dispatcher.test;

import cube.aigc.publicopinion.Article;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.WWWAuthenticationProtocolHandler;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * AIGC 测试。
 */
public class AIGCTest {

    private HttpClient client;

    private String host = "127.0.0.1";
    private int port = 7010;
    private String code = "XMQJiNEyZKMFomvGmymFuZJaoGYGRDas";

    protected AIGCTest() {
        this.client = new HttpClient();
    }

    public void setup() {
        try {
            this.client.start();
            this.client.getProtocolHandlers().remove(WWWAuthenticationProtocolHandler.NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void teardown() {
        try {
            this.client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getBaseUrl() {
        return "http://" + this.host + ":" + this.port;
    }

    private String getUrl(String contextPath) {
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        if (!contextPath.endsWith("/")) {
            contextPath = contextPath + "/";
        }
        return this.getBaseUrl() + contextPath + this.code;
    }

    public void testRequestChannel() {
        String url = this.getUrl("/aigc/channel/");

        System.out.println("POST - " + url);

        try {
            JSONObject request = new JSONObject();
            request.put("participant", "来自火星的星星");
            StringContentProvider provider = new StringContentProvider(request.toString());
            ContentResponse response = this.client.POST(url).content(provider).send();
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject data = new JSONObject(response.getContentAsString());
                System.out.println(data.toString(4));
            }
            else {
                System.out.println("Error: " + response.getStatus());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        System.out.println("******** END ********");
    }

    public void testChat(String code) {
        String url = this.getUrl("/aigc/chat/");

        System.out.println("POST - " + url);

        try {
            JSONObject request = new JSONObject();
            request.put("code", code);
            request.put("content", "大理旅游体验好吗");
            StringContentProvider provider = new StringContentProvider(request.toString());
            ContentResponse response = this.client.POST(url).content(provider).timeout(3, TimeUnit.MINUTES).send();
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject data = new JSONObject(response.getContentAsString());
                System.out.println(data.toString(4));
            }
            else {
                System.out.println("Error: " + response.getStatus());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        System.out.println("******** END ********");
    }

    public void testSentiment() {
        String url = this.getUrl("/aigc/nlp/sentiment/");

        System.out.println("POST - " + url);

        try {
            JSONObject request = new JSONObject();
            request.put("text", "天气不好呀");
            StringContentProvider provider = new StringContentProvider(request.toString());
            ContentResponse response = this.client.POST(url).content(provider).timeout(3, TimeUnit.MINUTES).send();
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject data = new JSONObject(response.getContentAsString());
                System.out.println(data.toString(4));
            }
            else {
                System.out.println("Error: " + response.getStatus());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        System.out.println("******** END ********");
    }

    public void testAddArticle() {
        String url = this.getUrl("/aigc/po/data/");

        System.out.println("POST - " + url);

        Article article = new Article("“荧光剂”争议中的舒肤佳",
                "舒肤佳再次因争议出现在大众视野。某博主测评舒肤佳一款香皂含有荧光剂，引发消费者对健康的担忧，对此，舒肤佳官方回应“添加剂量合规合法，正常使用不影响健康”。然而网友并不买账，更是罗列出荧光剂的危害，这样的争议中让舒肤佳站上了舆论的风口浪尖，甚至在6月6日登上热搜。\n" +
                        "从事件本身来说，在我国现行的化妆品添加物质法规中，荧光剂并非禁用物质，只要经过相关部门审批，正规生产，可以放心使用。但从事件外来看，当接二连三的负面舆论缠身，本就辉煌不再的舒肤佳又有了新的烦恼。\n" +
                        "争议焦点“荧光剂”为何物？\n" +
                        "因添加荧光剂，舒肤佳再次被质疑。6月6日，“舒肤佳回应香皂含荧光剂”的词条在微博平台不断发酵，引发争议。争议背后源于某测评博主发布的一段测评视频。在该博主发布的测评视频中，舒肤佳纯白清香型香皂被检测出含有荧光剂。在该视频中，舒肤佳官方回应：“舒肤佳纯白香皂的配方中为了调节香皂的色泽，添加了微量的调色成分，正常使用不会对皮肤产生影响。”\n" +
                        "这样的回应似乎并没有让消费者安心。有消费者担心经常使用，就算微量也会影响人体健康。也有不少网友罗列出荧光剂的危害，譬如皮肤过敏，影响血液系统、肝肾功能等。\n" +
                        "对此，舒肤佳相关负责人对北京商报记者表示，关于舒肤佳产品在添加剂的安全把控上，舒肤佳品牌所有产品上市前均经过大量严格测试，以确保符合所在国家、地区相关法律法规，安全与质量有充分保证。“关于测评博主视频，讲到“舒肤佳香皂中含荧光剂，可造成消费者皮肤红肿、内分泌紊乱，对免疫系统造成影响，甚至致癌。该说法无视科学事实，属不实传言。”该负责人补充道。\n" +
                        "官方回应微量添加，正常使用不会影响身体健康，消费者不放心拒绝接受官方回应。孰对孰错一时间难有定论。\n" +
                        "荧光剂为何物？又是否能够被允许用在化妆品中成为此次争议的焦点。\n" +
                        "根据中国洗涤用品工业协会发布的科普信息，荧光剂是荧光增白剂的简称，是一类复杂的有机化合物。该化合物在阳光或者紫外线的照射下会发出蓝色或者紫色的荧光，能增加视觉上的白亮度。荧光剂依据其化学结构不同，可以分为二苯乙烯型、香豆素型、吡唑啉型、苯并氧氮型和苯二甲酰亚胺型。其中，目前常用的荧光剂如硅酸锌、硫化锌镉、荧光黄、桑色素等，主要成分是二苯乙烯类衍生物。\n" +
                        "在《化妆品安全技术规范》禁止使用的物质中，荧光剂不在其中。根据广东省药品监督管理局科普信息，该物质的使用必须进行原材料申报，且需要经过有关部门的审批。因此，正规厂家生产的产品，其荧光剂的添加剂量和种类，都符合国家质量标准，控制在安全范围内，可以放心使用。\n" +
                        "“沉寂”的皂王频登舆论场\n" +
                        "这不是舒肤佳第一次出现在舆论场中。此前，舒肤佳因对其产品“ 去除99%”细菌的宣传，被相关部门罚款20万元，引发消费者不满。\n" +
                        "值得注意的是，相较于近年来不止一次陷入舆论风波，曾经的舒肤佳可谓是香皂界不折不扣的第一品牌，国人选购香皂的首选品牌。曾经的舒肤佳，可以毫不夸张地说是扛起了宝洁进驻中国市场的重担，帮其打开了中国市场。\n" +
                        "1988年，宝洁在广州设立了第一家属于自己的合资企业——广州宝洁有限公司，跨出布局中国市场的第一步。当时，一块香皂就可以解决所有清洁问题的中国市场，宝洁将布局的希望寄托在了舒肤佳的身上。\n" +
                        "1992年，宝洁推出舒肤佳，为了快速占领市场，宝洁一口气在中国投资了超过50条生产线。2003年，宝洁斥资2.2亿元的天价，在央视的黄金时段连续买下了3个月的广告播出权。\n" +
                        "随着“舒肤佳，爱心妈妈，呵护全家”广告火遍大江南北，舒肤佳成功地站在了香皂市场的顶端。公开数据显示，1998年，国产香皂市场占比不足10%；2005年，舒肤佳市占率达到41.95%，占据国内香皂市场第一的位置。\n" +
                        "然而，这样的情况没有持续太久，随着香皂替代品的逐渐增多，沐浴、洗衣领域各品牌推出针对专门场景使用的产品，沐浴露、洗面奶、洗手液等，香皂的市场逐渐萎缩。公开数据显示，肥皂产量2017年同比下滑2.2%至89万吨；2018年同比增长1.12%至90万吨，2019年肥皂产量与2018年持平。2016-2020年中国肥皂需求量呈现下降趋势。与此同时，随着舒肤佳受欢迎，市场上出现大量制假售假的情况，也在一定程度上影响了舒肤佳品牌。\n" +
                        "如今，虽然舒肤佳在香皂界依然稳居前排，但随着整个市场的萎缩，以及上海家化六神、力士等品牌的竞争，舒肤佳市占率出现下滑，目前占比为35.7%。基于此，舒肤佳也不断扩充沐浴露、洗手液等清洁产品以巩固自身地位。资料显示，近年来，舒肤佳希望通过花式营销重回大众视野。譬如推出香皂味香水，打出怀旧感情牌，进行捆绑营销；合作TFboys、吴尊、陈立农等明星代言营销等。\n" +
                        "“随着市场替代品出现，消费者有了更高的消费需求，下沉市场消费者需求也改变升级，洗手液、洗发水、沐浴露都取代了当年香皂的作用，香皂市场萎缩。此外，舒肤佳香皂在中国市场份额大到一度引发大量造假产品在农村市场销售，影响其品牌份额和销量。”深圳市思其晟公司CEO伍岱麒分析认为。\n" +
                        "在快消行业新零售专家鲍跃忠看来，面对不太景气的行业环境，品牌需要通过一些新的营销手段来提升逐渐沉寂的品牌活跃度。舒肤佳推出香水等营销活动，正是提升品牌活跃度、引流的一种营销尝试。类似这种怀旧营销推出跨品类的尝试，对于舒肤佳品牌的活跃度有着很大的帮助。",
                "北京商报",
                2023, 6, 7);

        try {
            JSONObject request = new JSONObject();
            request.put("action", "addArticle");
            request.put("category", "舒肤佳");
            request.put("article", article.toJSON());

            StringContentProvider provider = new StringContentProvider(request.toString());
            ContentResponse response = this.client.POST(url).content(provider).timeout(3, TimeUnit.MINUTES).send();
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject data = new JSONObject(response.getContentAsString());
                System.out.println(data.toString(4));
            }
            else {
                System.out.println("Error: " + response.getStatus());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public void testGetArticles() {
        String url = this.getUrl("/aigc/po/data/");

        System.out.println("POST - " + url);

        try {
            JSONObject request = new JSONObject();
            request.put("action", "getArticles");
            request.put("category", "汤臣倍健");

            StringContentProvider provider = new StringContentProvider(request.toString());
            ContentResponse response = this.client.POST(url).content(provider).timeout(3, TimeUnit.MINUTES).send();
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject data = new JSONObject(response.getContentAsString());
                System.out.println(data.toString(4));
            }
            else {
                System.out.println("Error: " + response.getStatus());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        AIGCTest test = new AIGCTest();
        test.setup();

//        test.testRequestChannel();
//        test.testChat("NldHebfkVCmljDnX");

//        test.testSentiment();

//        test.testGetArticles();
        test.testAddArticle();

        test.teardown();
    }
}
