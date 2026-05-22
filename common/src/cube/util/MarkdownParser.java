/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.util;

import java.util.ArrayList;
import java.util.List;

public class MarkdownParser {

    private String other;

    private List<Paragraph> paragraphs = new ArrayList<>();

    public MarkdownParser(String text) {
        this.parse(text);
    }

    private void parse(String text) {
        StringBuilder other = new StringBuilder();

        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; ++i) {
            String line = lines[i];
            if (line.length() <= 2) {
                other.append(line);
                continue;
            }

            if (line.startsWith("#") || line.startsWith("【")) {
                line = line.trim();

                if (line.startsWith("【") && line.contains("】")) {
                    line = line.replace("【", "");
                    line = line.replace("】", "");
                    line = "# " + line.trim();
                }

                String title = line;
                StringBuilder buf = new StringBuilder();
                for (int n = i + 1; n < lines.length; ++n, ++i) {
                    String lineText = lines[n].trim();
                    if (lineText.length() < 2) {
                        buf.append("\n");
                        continue;
                    }
                    if (lineText.startsWith("#") || lineText.startsWith("【")) {
                        i -= 1;
                        break;
                    }
                    buf.append(lineText).append("\n");
                }

                Paragraph paragraph = new Paragraph(title.trim(), buf.toString().trim());
                this.paragraphs.add(paragraph);
            }
            else {
                other.append(line);
            }
        }

        this.other = other.toString().trim();
    }

    public String getOther() {
        return this.other;
    }

    public List<Paragraph> getParagraphs() {
        return this.paragraphs;
    }

    public class Paragraph {

        public String title;

        public String content;

        public Paragraph(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }

    public static void main(String[] args) {
//        String text = "您的内心焦虑画像是： **迷雾萦绕的晃动吊桥**\n\n## 状态解析\n\n你心里的这座桥是一座吊桥，虽然没有断，但走在上面摇摇晃晃。更让你难受的是，桥的对岸被一层迷雾遮住了。你对“未知”的容忍度正在降低，脑海里总是不断地冒出“万一出错了怎么办”、“万一搞砸了怎么收场”的念头。你试图通过过度准备和反复确认来获得控制感，但这反而让你感到身心俱疲。\n\n从你的绘画中可以看出，画面的画幅相对画布面积非常小，这说明你可能在内心深处缺乏自信，认为自己的存在感较弱。画面中主要元素少于指导数量，显示出你倾向于逃避现实，可能在面对问题时选择性地忽略或回避。整个画幅结构偏向画布顶部，这表明你对未来抱有理想化的期待，但这种期待可能与现实脱节。同时，整个画幅结构偏于画布角落，说明你缺乏足够的自信，内心存在一定的焦虑倾向。画面中人物的比例非常大，暗示你可能有自我膨胀的倾向，同时也可能隐藏着一定的攻击性。人物没有细节、五官和耳朵，这些细节的缺失表明你缺乏创造力，同时也反映出你内心的冲突和固执。\n\n## 具体表现与底层逻辑\n\n- **具体表现**：经常感到肩颈僵硬、眉头紧锁；做事容易追求完美，反复检查细节；在安静的时候（尤其是睡前），大脑像跑马灯一样疯狂播放各种灾难化的“如果...就...”场景。\n\n- **底层逻辑**：你的大脑对“不确定性”产生了过敏反应。为了防御潜在的危险，你的潜意识开启了“灾难化思维（Catastrophizing）”模式，把只有1%可能发生的坏事，在脑海里放大成了100%会发生的灾难。这种思维模式不仅加剧了你的焦虑感，还让你陷入了一种“越想越害怕”的恶性循环。\n\n## 专属破局建议\n\n- **切断“万一”的死循环**：当脑子里再次冒出“万一搞砸了怎么办”时，强制自己回答下一个问题：“如果真的搞砸了，我现在的能力能怎么补救？”把虚无的恐慌落地为具体的对策。例如，你可以问自己：“如果我犯了错误，我能不能从中吸取教训？我能不能通过努力弥补损失？”这种思维方式可以帮助你将注意力从“灾难化”的想象转移到实际可行的解决方案上。\n\n- **云宝的安抚**：迷雾只是遮住了视线，并没有切断前方的路。你不需要看清对岸的每一根草，你只需要看清脚下这半米长的木板。把注意力从“不可控的未来”拉回到“可控的当下”。你可以尝试一些放松的技巧，比如深呼吸、正念冥想，或者写下你担心的事情，然后分析这些担忧的合理性。记住，焦虑的情绪本身并不可怕，可怕的是你如何应对它。试着用更理性和客观的态度看待自己的情绪，你会发现，很多你担心的事情其实并没有那么可怕。";
        String text = "【你的心理画像】  \n你是一个谨慎而传统的年轻人，性格温和，行事保守。你对人友善，愿意支持那些行为端正、有成果的人，但对怪异行为和原则性问题难以容忍。你心思细腻，注重隐私，喜欢与人保持一定的距离。面对新事物和挑战，你倾向于用保守的方法应对，不愿冒险。你对世界有自己的看法，有时会沉浸在自己的理想中，但对现实的感知力稍显不足。\n\n【只有你懂的生活瞬间】  \n1. 你总是能在新环境中迅速找到自己的节奏，但每当需要做出决定时，内心总会有些犹豫，仿佛在等待一个“正确”的答案。这种谨慎让你在适应新事物时显得格外稳重，但也偶尔会让你错过一些即兴的快乐。  \n2. 你擅长倾听，总能用温和的语气让对方感到被理解。然而，当别人表达与你不同的意见时，你可能会不自觉地想要说服对方，而不是单纯地接纳不同的观点。这种习惯让你在人际关系中既温暖又有些疲惫。\n\n【懂点心理学：潜意识在保护你什么？】  \n你的心理机制其实是在用一种“安全第一”的方式保护你。你的情绪脑（边缘系统）可能过于敏感，总是倾向于优先考虑安全和稳定，而不是冒险和尝试。这种机制让你在面对变化时显得格外谨慎，甚至有些退缩。但你知道吗？这种谨慎的背后，是你对责任的强烈担当和对秩序的深刻尊重。你是一个极其有责任心的人，总是希望自己的行为是正确且符合原则的。这种特质虽然让你在某些时候显得有些保守，但也让你在团队中成为一个值得信赖的伙伴。\n\n【微光行动锦囊】  \n1. **每天记录一件小事**：从今天开始，每天记录一件你独立完成的小事，比如自己解决了一个问题，或者尝试了一个新的小任务。记录时，允许自己感受其中的成就感，哪怕只是一点点。  \n2. **睡前感恩练习**：每晚睡觉前，花3分钟写下3件你当天感激的事情。这些事情可以是别人对你的一个小善意，也可以是你自己完成的一件小事。这种练习能帮助你更关注当下的美好，而不是过度理想化未来。\n\n【云宝悄悄话】  \n我在，你并不孤单。你的谨慎和责任感让你在人群中闪闪发光，虽然你可能还没发现自己的光芒，但我知道，你的温柔和坚持都是最珍贵的礼物。辛苦了，抱抱你。";
        MarkdownParser markdownParser = new MarkdownParser(text);

        System.out.println("****************************************");
        System.out.println(markdownParser.getOther());
        System.out.println("****************************************");

        for (Paragraph paragraph : markdownParser.getParagraphs()) {
            System.out.println("****************************************");
            System.out.println("Paragraph:");
            System.out.println(paragraph.title);
            System.out.println("----------------------------------------");
            System.out.println(paragraph.content);
        }
    }
}