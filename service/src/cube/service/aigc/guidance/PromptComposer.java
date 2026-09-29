/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import cell.util.log.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 提示词编排器。
 *
 * <p>把提示词按语义划分为若干**独立编号的段**，每段有自己的 Token 预算，逐段装配后再拼成一份文本。
 * 这样做的目的：</p>
 * <ul>
 *     <li>指令（技能）与数据（已知信息）分区呈现，模型能区分「要我做什么」与「这是给你的材料」；</li>
 *     <li>任一段超预算只影响该段，不会像过去那样把后面的内容整体挤没；</li>
 *     <li>预算按 **Token** 计量（借助 {@link TokenEstimator}，并由模型单元返回的真实用量在线校准），
 *         而不是把上下文窗口长度当字符数用。</li>
 * </ul>
 *
 * <p>段顺序：系统说明 → 技能目录 → 技能指令 → 已知信息 → 用户请求。其中用户请求**永不丢弃**
 * （超预算时截断并提示）；技能目录、技能指令、已知信息、历史对话为弹性段。</p>
 *
 * <p>弹性段的预算分配有两种模式：</p>
 * <ul>
 *     <li><b>比例模式</b>（{@code prompt.composer=true}，默认）：先按配置比例给每段一个上限，
 *         某段未用满的额度按同一次序**顺延**给后面的段；</li>
 *     <li><b>顺序模式</b>（{@code prompt.composer=false}，回退用）：不做比例切分，各段按顺序依次
 *         取用总输入预算，用满即止。用于出问题时的行为回退。</li>
 * </ul>
 *
 * <p>兼容性：若本次没有任何附加段（无系统说明、无技能、无已知信息），则最终文本与历史版本一致，
 * 直接就是用户请求原文，不会因为引入编排器而改变既有模型的输入格式。</p>
 */
public class PromptComposer {

    /**
     * 段名：系统说明。
     */
    public final static String SEGMENT_SYSTEM = "system";

    /**
     * 段名：技能目录。
     */
    public final static String SEGMENT_CATALOG = "catalog";

    /**
     * 段名：技能指令。
     */
    public final static String SEGMENT_SKILLS = "skills";

    /**
     * 段名：已知信息。
     */
    public final static String SEGMENT_RETRIEVAL = "retrieval";

    /**
     * 段名：历史对话。
     */
    public final static String SEGMENT_HISTORY = "history";

    /**
     * 段名：用户请求。
     */
    public final static String SEGMENT_USER = "user";

    private final static String HEADER_CATALOG = "[可用技能]";

    private final static String HEADER_SKILLS = "[技能指令]";

    private final static String HEADER_RETRIEVAL = "[已知信息]";

    private final static String HEADER_USER = "用户请求：";

    /**
     * 段之间使用的分隔符。
     */
    private final static String SEPARATOR = "\n\n";

    private final static String TRUNCATED_NOTICE = "\n[内容因上下文预算不足被截断]";

    /**
     * 技能段内单个技能的小节模板。
     */
    private final static String SKILL_SECTION_FORMAT = "### Skill: %s\n%s";

    /**
     * 输入预算的下限比例保护：若上下文窗口过小导致输入预算过低，则退化为整个窗口。
     */
    private final static double MIN_INPUT_BUDGET_PERCENT = 25.0;

    public static class NamedText {

        public final String name;

        public final String content;

        public NamedText(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }

    /**
     * 一个提示词段。
     */
    public static class Segment {

        public final String name;

        public String text;

        public int tokens;

        public boolean truncated = false;

        public boolean dropped = false;

        /**
         * 历史段的附加信息：被保留的历史条数。
         */
        public int count = 0;

        private Segment(String name) {
            this.name = name;
        }
    }

    /**
     * 提示词预算策略。
     */
    public static class Policy {

        /**
         * 为模型输出预留的上下文比例（%），从上下文窗口中先扣除。
         */
        public int outputReservePercent = 25;

        public int catalogPercent = 4;

        public int skillsPercent = 30;

        public int retrievalPercent = 33;

        public int historyPercent = 33;
    }

    /**
     * 编排输入。
     */
    public static class Input {

        /**
         * 上下文窗口长度（Token）。
         */
        public int contextLimitTokens = 0;

        /**
         * 系统说明，可为 {@code null}。
         */
        public String systemText = null;

        /**
         * 技能目录（索引）文本，可为 {@code null}。
         */
        public String catalogText = null;

        /**
         * 需要注入的技能全文，可为空。
         */
        public List<NamedText> skills = new ArrayList<>();

        /**
         * 已知信息（知识释义、附件检索结果等）文本，可为 {@code null}。
         */
        public String retrievalText = null;

        /**
         * 用户请求原文。该段永不被丢弃，超出预算时截断并提示。
         */
        public String userText = null;

        /**
         * 历史对话文本，按**由旧到新**排列，每项是一条历史记录序列化后的文本；可为空。
         */
        public List<String> historyTexts = new ArrayList<>();

        /**
         * 调用方要求的历史条数上限，&lt;= 0 表示不额外限制。
         */
        public int historyCap = 0;
    }

    /**
     * 编排结果。
     */
    public static class Result {

        /**
         * 最终提示词文本。
         */
        public String content = "";

        /**
         * 允许携带的历史记录条数（最近的若干条）。
         */
        public int historyLimit = 0;

        public final List<Segment> segments = new ArrayList<>();

        public int contextLimitTokens = 0;

        public int inputBudgetTokens = 0;

        public int outputReserveTokens = 0;

        /**
         * 按估算器折算出的提示词 Token 数。
         */
        public int estimatedInputTokens = 0;

        public boolean truncated = false;

        /**
         * 因预算不足被截断或整体丢弃的技能名称（技能段专用），用于留痕。
         */
        public final List<String> truncatedSkills = new ArrayList<>();

        public Segment segment(String name) {
            for (Segment segment : this.segments) {
                if (segment.name.equals(name)) {
                    return segment;
                }
            }
            return null;
        }

        /**
         * 输出分段留痕，便于排查「为什么模型没看到某段内容」。
         */
        public JSONObject toTraceJSON() {
            JSONObject json = new JSONObject();
            json.put("truncated", this.truncated);

            JSONArray array = new JSONArray();
            for (Segment segment : this.segments) {
                JSONObject item = new JSONObject();
                item.put("name", segment.name);
                item.put("tokens", segment.tokens);
                if (segment.count > 0) {
                    item.put("count", segment.count);
                }
                if (segment.truncated) {
                    item.put("truncated", true);
                }
                if (segment.dropped) {
                    item.put("dropped", true);
                }
                array.put(item);
            }
            json.put("segments", array);

            JSONObject budget = new JSONObject();
            budget.put("context", this.contextLimitTokens);
            budget.put("outputReserve", this.outputReserveTokens);
            budget.put("inputBudget", this.inputBudgetTokens);
            budget.put("estimatedInput", this.estimatedInputTokens);
            json.put("budget", budget);

            return json;
        }
    }

    private static final PromptComposer sInstance = new PromptComposer();

    public static PromptComposer getInstance() {
        return sInstance;
    }

    private volatile boolean enabled = true;

    private volatile Policy policy = new Policy();

    private PromptComposer() {
    }

    /**
     * 应用配置。
     *
     * @param enabled 是否启用比例模式编排。关闭时退化为顺序模式（不做比例切分）。
     * @param policy 预算策略，{@code null} 时使用默认策略。
     */
    public void configure(boolean enabled, Policy policy) {
        this.enabled = enabled;
        this.policy = (null != policy) ? policy : new Policy();
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public Policy getPolicy() {
        return this.policy;
    }

    /**
     * 按段装配提示词。
     *
     * @param input 编排输入。
     * @return 返回编排结果，永不为 {@code null}。
     */
    public Result compose(Input input) {
        Result result = new Result();
        if (null == input) {
            return result;
        }

        final TokenEstimator estimator = TokenEstimator.getInstance();

        int contextLimit = (input.contextLimitTokens > 0)
                ? input.contextLimitTokens : 128 * 1024;
        result.contextLimitTokens = contextLimit;

        int outputReserve = (int) ((long) contextLimit * this.policy.outputReservePercent / 100L);
        result.outputReserveTokens = outputReserve;

        int inputBudget = contextLimit - outputReserve;
        if (inputBudget < contextLimit * MIN_INPUT_BUDGET_PERCENT / 100.0) {
            // 上下文窗口过小，退化为整个窗口，避免预算为负数
            inputBudget = contextLimit;
        }
        result.inputBudgetTokens = inputBudget;

        // 用户请求：必须保留
        Segment userSegment = new Segment(SEGMENT_USER);
        String userText = (null != input.userText) ? input.userText : "";
        userSegment.tokens = estimator.estimate(userText);
        if (userSegment.tokens > inputBudget) {
            userText = truncate(userText, estimator.tokensToChars(inputBudget));
            userSegment.tokens = estimator.estimate(userText);
            userSegment.truncated = true;
        }

        // 系统说明：短小，不参与比例分配，直接占用预算
        Segment systemSegment = new Segment(SEGMENT_SYSTEM);
        String systemText = (null != input.systemText) ? input.systemText.trim() : "";
        if (!systemText.isEmpty()) {
            systemSegment.text = systemText;
            systemSegment.tokens = estimator.estimate(systemText);
        }

        // 弹性段的可用额度
        int flexible = inputBudget - userSegment.tokens - systemSegment.tokens;
        if (flexible < 0) {
            flexible = 0;
        }

        Segment catalogSegment = new Segment(SEGMENT_CATALOG);
        Segment skillsSegment = new Segment(SEGMENT_SKILLS);
        Segment retrievalSegment = new Segment(SEGMENT_RETRIEVAL);
        Segment historySegment = new Segment(SEGMENT_HISTORY);

        if (flexible > 0) {
            // 比例模式：按配置比例给出各段上限；顺序模式：不设上限，按顺序取用总预算
            final boolean ratioMode = this.enabled;
            int catalogCap;
            int skillsCap;
            int retrievalCap;
            int historyCap;
            if (ratioMode) {
                catalogCap = (int) ((long) flexible * this.policy.catalogPercent / 100L);
                skillsCap = (int) ((long) flexible * this.policy.skillsPercent / 100L);
                retrievalCap = (int) ((long) flexible * this.policy.retrievalPercent / 100L);
                historyCap = (int) ((long) flexible * this.policy.historyPercent / 100L);
            }
            else {
                catalogCap = flexible;
                skillsCap = flexible;
                retrievalCap = flexible;
                historyCap = flexible;
            }

            int remaining = flexible;
            int pool = 0;

            // 1) 技能目录
            String catalogText = (null != input.catalogText) ? input.catalogText.trim() : "";
            if (!catalogText.isEmpty()) {
                catalogText = HEADER_CATALOG + "\n" + catalogText;
                int needed = estimator.estimate(catalogText);
                int avail = Math.min(remaining, catalogCap + (ratioMode ? pool : 0));
                if (needed <= avail) {
                    catalogSegment.text = catalogText;
                    catalogSegment.tokens = needed;
                }
                else {
                    catalogSegment.text = truncate(catalogText, estimator.tokensToChars(avail));
                    catalogSegment.tokens = estimator.estimate(catalogSegment.text);
                    catalogSegment.truncated = true;
                }
                remaining -= catalogSegment.tokens;
                if (ratioMode) {
                    pool = Math.max(0, avail - catalogSegment.tokens);
                }
            }

            // 2) 技能指令
            if (null != input.skills && !input.skills.isEmpty()) {
                int avail = (remaining > 0)
                        ? Math.min(remaining, skillsCap + (ratioMode ? pool : 0)) : 0;
                int used = 0;
                StringBuilder buf = new StringBuilder();
                for (int i = 0; i < input.skills.size(); ++i) {
                    NamedText skill = input.skills.get(i);
                    String content = (null == skill.content) ? "" : skill.content.trim();
                    String section = String.format(SKILL_SECTION_FORMAT, skill.name, content);
                    int needed = estimator.estimate(section);
                    int left = avail - used;

                    if (needed <= left) {
                        if (buf.length() > 0) {
                            buf.append(SEPARATOR);
                        }
                        buf.append(section);
                        used += needed;
                    }
                    else if (left > 0) {
                        // 预算仍可容纳部分内容：截断当前技能，后续技能整体丢弃
                        if (buf.length() > 0) {
                            buf.append(SEPARATOR);
                        }
                        buf.append(truncate(section, estimator.tokensToChars(left)));
                        used = avail;
                        skillsSegment.truncated = true;
                        result.truncatedSkills.add(skill.name);
                        for (int j = i + 1; j < input.skills.size(); ++j) {
                            result.truncatedSkills.add(input.skills.get(j).name);
                        }
                        break;
                    }
                    else {
                        // 预算已耗尽：当前及其后技能整体丢弃
                        skillsSegment.truncated = true;
                        for (int j = i; j < input.skills.size(); ++j) {
                            result.truncatedSkills.add(input.skills.get(j).name);
                        }
                        break;
                    }
                }

                if (buf.length() > 0) {
                    skillsSegment.text = HEADER_SKILLS + "\n" + buf.toString();
                    skillsSegment.tokens = estimator.estimate(skillsSegment.text);
                    remaining -= skillsSegment.tokens;
                }
                else {
                    skillsSegment.dropped = true;
                }
                if (ratioMode) {
                    pool = Math.max(0, avail - (skillsSegment.dropped ? 0 : skillsSegment.tokens));
                }
            }

            // 3) 已知信息
            String retrievalText = (null != input.retrievalText) ? input.retrievalText.trim() : "";
            if (remaining > 0 && !retrievalText.isEmpty()) {
                retrievalText = HEADER_RETRIEVAL + "\n" + retrievalText;
                int needed = estimator.estimate(retrievalText);
                int avail = Math.min(remaining, retrievalCap + (ratioMode ? pool : 0));
                if (needed <= avail) {
                    retrievalSegment.text = retrievalText;
                    retrievalSegment.tokens = needed;
                }
                else {
                    retrievalSegment.text = truncate(retrievalText, estimator.tokensToChars(avail));
                    retrievalSegment.tokens = estimator.estimate(retrievalSegment.text);
                    retrievalSegment.truncated = true;
                }
                remaining -= retrievalSegment.tokens;
                if (ratioMode) {
                    pool = Math.max(0, avail - retrievalSegment.tokens);
                }
            }

            // 4) 历史对话：由预算反推可携带的条数（保留最新的若干条）
            int historyAvail = Math.min(remaining, historyCap + (ratioMode ? pool : 0));
            List<String> historyTexts = input.historyTexts;
            if (historyAvail > 0 && null != historyTexts && !historyTexts.isEmpty()) {
                int cap = (input.historyCap > 0) ? input.historyCap : historyTexts.size();
                int used = 0;
                int count = 0;
                for (int i = historyTexts.size() - 1; i >= 0 && count < cap; --i) {
                    int needed = estimator.estimate(historyTexts.get(i));
                    if (used + needed > historyAvail) {
                        break;
                    }
                    used += needed;
                    ++count;
                }
                historySegment.count = count;
                historySegment.tokens = used;
                historySegment.truncated = (count < Math.min(cap, historyTexts.size()));
                historySegment.dropped = (count == 0);
                result.historyLimit = count;
            }
        }

        // 组装
        List<Segment> ordered = new ArrayList<>();
        addIfPresent(ordered, systemSegment);
        addIfPresent(ordered, catalogSegment);
        addIfPresent(ordered, skillsSegment);
        addIfPresent(ordered, retrievalSegment);
        userSegment.text = userText;
        ordered.add(userSegment);

        boolean hasExtra = ordered.size() > 1;
        StringBuilder buf = new StringBuilder();
        for (Segment segment : ordered) {
            if (buf.length() > 0) {
                buf.append(SEPARATOR);
            }
            if (SEGMENT_USER.equals(segment.name) && hasExtra) {
                buf.append(HEADER_USER).append(segment.text);
            }
            else {
                buf.append(segment.text);
            }
        }

        result.content = buf.toString();
        result.estimatedInputTokens = estimator.estimate(result.content);
        result.segments.add(systemSegment);
        result.segments.add(catalogSegment);
        result.segments.add(skillsSegment);
        result.segments.add(retrievalSegment);
        result.segments.add(historySegment);
        result.segments.add(userSegment);
        for (Segment segment : result.segments) {
            if (segment.truncated || segment.dropped) {
                result.truncated = true;
            }
        }

        if (Logger.isDebugLevel()) {
            Logger.d(PromptComposer.class, "#compose - budget: " + result.inputBudgetTokens
                    + ", estimated: " + result.estimatedInputTokens
                    + ", history: " + result.historyLimit
                    + ", skills: " + skillsSegment.tokens);
        }

        return result;
    }

    /**
     * 按字符额度截断文本，保留开头并追加截断提示。
     *
     * @param text 原文。
     * @param allowChars 允许的最大字符数。
     * @return 返回不超过额度的文本。
     */
    public static String truncate(String text, int allowChars) {
        if (null == text || text.isEmpty()) {
            return "";
        }

        if (allowChars <= 0) {
            return "";
        }

        if (text.length() <= allowChars) {
            return text;
        }

        int bodyBudget = allowChars - TRUNCATED_NOTICE.length();
        if (bodyBudget <= 0) {
            return text.substring(0, allowChars);
        }

        return text.substring(0, bodyBudget) + TRUNCATED_NOTICE;
    }

    private static void addIfPresent(List<Segment> list, Segment segment) {
        if (null == segment.text || segment.text.isEmpty()) {
            return;
        }
        list.add(segment);
    }

    /**
     * 把技能列表按名称排序后返回，保证提示词稳定（便于复用与排查）。
     */
    public static List<NamedText> sortSkills(List<NamedText> skills) {
        List<NamedText> list = new ArrayList<>(skills);
        Collections.sort(list, (o1, o2) -> o1.name.compareTo(o2.name));
        return list;
    }
}
