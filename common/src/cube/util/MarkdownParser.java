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

            if (line.startsWith("#")) {
                String title = line;
                StringBuilder buf = new StringBuilder();
                for (int n = i + 1; n < lines.length; ++n, ++i) {
                    String lineText = lines[n];
                    if (lineText.length() < 2) {
                        buf.append("\n");
                        continue;
                    }
                    if (lineText.startsWith("#")) {
                        i -= 1;
                        break;
                    }
                    buf.append(lineText);
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
        String text = "您的内心焦虑画像是： **迷雾萦绕的晃动吊桥**\n\n## 状态解析\n\n你心里的这座桥是一座吊桥，虽然没有断，但走在上面摇摇晃晃。更让你难受的是，桥的对岸被一层迷雾遮住了。你对“未知”的容忍度正在降低，脑海里总是不断地冒出“万一出错了怎么办”、“万一搞砸了怎么收场”的念头。你试图通过过度准备和反复确认来获得控制感，但这反而让你感到身心俱疲。\n\n从你的绘画中可以看出，画面的画幅相对画布面积非常小，这说明你可能在内心深处缺乏自信，认为自己的存在感较弱。画面中主要元素少于指导数量，显示出你倾向于逃避现实，可能在面对问题时选择性地忽略或回避。整个画幅结构偏向画布顶部，这表明你对未来抱有理想化的期待，但这种期待可能与现实脱节。同时，整个画幅结构偏于画布角落，说明你缺乏足够的自信，内心存在一定的焦虑倾向。画面中人物的比例非常大，暗示你可能有自我膨胀的倾向，同时也可能隐藏着一定的攻击性。人物没有细节、五官和耳朵，这些细节的缺失表明你缺乏创造力，同时也反映出你内心的冲突和固执。\n\n## 具体表现与底层逻辑\n\n- **具体表现**：经常感到肩颈僵硬、眉头紧锁；做事容易追求完美，反复检查细节；在安静的时候（尤其是睡前），大脑像跑马灯一样疯狂播放各种灾难化的“如果...就...”场景。\n\n- **底层逻辑**：你的大脑对“不确定性”产生了过敏反应。为了防御潜在的危险，你的潜意识开启了“灾难化思维（Catastrophizing）”模式，把只有1%可能发生的坏事，在脑海里放大成了100%会发生的灾难。这种思维模式不仅加剧了你的焦虑感，还让你陷入了一种“越想越害怕”的恶性循环。\n\n## 专属破局建议\n\n- **切断“万一”的死循环**：当脑子里再次冒出“万一搞砸了怎么办”时，强制自己回答下一个问题：“如果真的搞砸了，我现在的能力能怎么补救？”把虚无的恐慌落地为具体的对策。例如，你可以问自己：“如果我犯了错误，我能不能从中吸取教训？我能不能通过努力弥补损失？”这种思维方式可以帮助你将注意力从“灾难化”的想象转移到实际可行的解决方案上。\n\n- **云宝的安抚**：迷雾只是遮住了视线，并没有切断前方的路。你不需要看清对岸的每一根草，你只需要看清脚下这半米长的木板。把注意力从“不可控的未来”拉回到“可控的当下”。你可以尝试一些放松的技巧，比如深呼吸、正念冥想，或者写下你担心的事情，然后分析这些担忧的合理性。记住，焦虑的情绪本身并不可怕，可怕的是你如何应对它。试着用更理性和客观的态度看待自己的情绪，你会发现，很多你担心的事情其实并没有那么可怕。";
        MarkdownParser markdownParser = new MarkdownParser(text);

        System.out.println("****************************************");
        System.out.println(markdownParser.getOther());
        System.out.println("****************************************");

        for (Paragraph paragraph : markdownParser.getParagraphs()) {
            System.out.println("Paragraph:");
            System.out.println(paragraph.title);
            System.out.println("----------------------------------------");
            System.out.println(paragraph.content);
        }
    }
}