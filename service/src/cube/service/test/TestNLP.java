package cube.service.test;

import cube.nlp.Processor;
import cube.nlp.seg.common.Term;
import cube.nlp.tokenizer.StandardTokenizer;

import java.util.List;
import java.util.Map;

public class TestNLP {

    public static void test() {
        Processor.setup("./assets");

        Processor processor = new Processor();

        String[] sentences = new String[] {
                "根据汤臣倍健2023年6月的舆情图表数据，撰写汤臣倍健6月份的舆情报告。",
                "自我存在是个体体验到自身是能思想、能感知、有情感、能行动的统一体，是扮演各种“角色”之总和，从中能看到并意识到自己的这些不同方面。"
        };

        StandardTokenizer tokenizer = processor.getTokenizer();

        for (String sentence : sentences) {
            // 分词
            List<Term> list = tokenizer.segment(sentence);
            System.out.println(list.toString());
            System.out.println("----------------------------------------");
            // 关键词
            Map<String, Float> words = processor.extractKeywordWithScore(list, 10);
            System.out.println(words.toString());

            System.out.println("----------------------------------------");
            // 摘要
            String summary = processor.getSummary(sentence, 100);
            System.out.println(summary);
            System.out.println();
        }

        Processor.teardown();
    }

    public static void testSummary() {
        Processor.setup("./assets");

        Processor processor = new Processor();

        String[] sentenceArray = new String[]{
                "每万人拥有5G基站数达49个，列全国第一；获准向公众开放的生成式人工智能大模型产品占全国近一半……数字经济关乎发展大局，正在举行的北京两会传来消息：2023年，北京持续精心打造全球数字经济标杆城市，数字经济增加值占地区生产总值比重达42.9%，有力支撑和推动首都高质量发展。",
                "统计显示，2023年北京数字经济实现增加值18766.7亿元，按现价计算同比增长8.5%，提高1.3个百分点。",
                "数字基础设施建设是数字经济发展的重要指标。2023年，北京新建5G基站3万个，每万人拥有5G基站数达49个，列全国第一；全球性能领先的区块链与隐私计算算力集群Hive“蜂巢”启用，工业互联网标识解析国家顶级节点（北京）累计标识注册量1262亿；编制算力基础设施实施方案，算力供给规模1.2万P以上。",
                "在数字化转型方面，北京数字产业化与产业数字化双向发力。“文心一言”“智谱清言”等24个获准向公众开放的生成式人工智能大模型产品占全国近一半；高级别自动驾驶示范区实现160平方公里连片运行，智能网联乘用车、无人配送车等八大场景775台自动驾驶车辆在区内测试使用；103家企业完成智能工厂和数字化车间建设。",
                "同时，北京智慧城市建设扎实推进。“京通”用户超过2200万、接入800余项市级服务事项及17个区级旗舰店，“京办”接入294个系统，“京智”接入22个决策专题。",
                "根据2024年北京市政府工作报告，北京今年还将推动算力中心、数据训练基地、国家区块链枢纽节点等一批重大项目落地，启动高级别自动驾驶示范区建设4.0阶段任务，加快培育标杆性“数字领航”企业，推动全市感知设备设施共建共享共用等。",
                "“加快建设全球数字经济标杆城市，积极布局数字经济关键赛道，以数字化驱动生产方式、生活方式和治理方式全面变革。”北京市市长殷勇表示。",
        };

        StringBuilder sentence = new StringBuilder();
        for (String line : sentenceArray) {
            sentence.append(line).append("\n");
        }

        String summary = processor.getSummary(sentence.toString(), 100);
        System.out.println(summary);

        System.out.println("----------------------------------------");

        List<String> summaryList = processor.extractSummary(sentence.toString(), 2, "\n");
        for (String s : summaryList) {
            System.out.println(s);
        }

        Processor.teardown();
    }

    public static void main(String[] args) {
        testSummary();
    }
}
