/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.spi;

import cell.core.net.Endpoint;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.aigc.psychology.Painting;
import cube.aigc.psychology.ReportPermission;
import cube.aigc.complex.attachment.Attachment;
import cube.aigc.SemanticSearchListener;
import cube.aigc.text.Keyword;
import cube.aigc.listener.GenerateTextListener;
import cube.aigc.listener.VoiceDiarizationListener;
import cube.aigc.cv.MatchSimilarityListener;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.consultation.ConsultationTheme;
import cube.aigc.psychology.EvaluationReport;
import cube.common.entity.AIGCChatHistory;
import cube.common.entity.Chart;
import cube.aigc.psychology.PaintingReport;
import cube.aigc.psychology.ScaleReport;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.algorithm.Attention;
import cube.aigc.psychology.composition.HexagonDimensionScore;
import cube.aigc.psychology.composition.PaintingFeatureSet;
import cube.aigc.psychology.composition.Scale;
import cube.aigc.psychology.listener.PaintingReportListener;
import cube.aigc.psychology.listener.ScaleReportListener;
import cube.aigc.spi.AIGCHost;
import cube.aigc.spi.AIGCPluginContextLite;
import cube.aigc.spi.ActionModule;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.Contact;
import cube.common.entity.Membership;
import cube.common.entity.FileLabel;
import cube.common.entity.GeneratingOption;
import cube.common.entity.GeneratingRecord;
import cube.common.entity.ObjectInfo;
import cube.common.entity.User;
import cube.core.Module;
import cube.core.Storage;
import cube.service.aigc.AIGCCellet;
import cube.service.aigc.member.MemberCenter;
import cube.service.contact.ContactManager;
import cube.service.aigc.AIGCHook;
import cube.service.aigc.AIGCPluginContext;
import cube.service.aigc.AIGCService;
import cube.service.aigc.guidance.Prompts;
import cube.service.psychology.scene.ContentTools;
import cube.service.cv.CVService;
import cube.util.AudioUtils;
import cube.service.aigc.utils.WavToMp3Context;
import cube.service.aigc.utils.AudioProcessor;
import cube.service.psychology.scene.PsychologyScene;
import cube.util.tokenizer.SegToken;
import cube.util.tokenizer.Tokenizer;
import cube.util.tokenizer.keyword.TFIDFAnalyzer;
import cube.storage.StorageFactory;
import cube.storage.StorageType;
import cube.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 宿主能力实现。
 *
 * <p>本类是 SPI 与宿主之间的<b>能力侧</b>桥（动作侧桥为 {@link ActionRunner}）。
 * 设计约束：</p>
 *
 * <ul>
 *   <li><b>逐方法纯委托</b>：24 个方法直接转发到 {@link AIGCService} 的同名公共方法，
 *       不加任何判断、不改语义，因此可逐字段对拍；</li>
 *   <li><b>派生方法只做「适配」不做「业务」</b>：如 {@link #generateStructured} 仅负责
 *       「提问 → 解析 → 校验」，{@link #extractKeywords} 仅负责「委托分析器 → 收敛为词列表」，
 *       {@link #queryPaintingReport} 仅负责「委托场景 → 收敛为 JSON」，
 *       均不含任何业务规则；</li>
 *   <li><b>失败一律降级不抛</b>：模块是可选增强，其异常不得影响主服务，
 *       因此内部异常统一转为「返回空值 + 记 ERROR」。</li>
 * </ul>
 *
 * <p><b>线程约束</b>：本类的每个方法都可能在通信线程、后台池线程或模块自有线程中被调用。
 * 除 {@link #analyzer}（延迟创建的单例缓存，经双检锁保证安全发布）外，
 * 本类<b>不持有任何可变状态</b>，全部委托给线程安全的宿主组件。</p>
 */
public final class AIGCHostImpl implements AIGCHost {

    /**
     * 模块资源的新路径前缀（相对工作目录）。
     */
    private final static String MODULE_ASSET_PREFIX = "assets/modules/";

    /**
     * 模块资源的旧路径前缀（相对工作目录），仅用于回退兼容。
     */
    private final static String LEGACY_MODULE_ASSET_PREFIX = "assets/";

    /**
     * 模块工作目录前缀（相对宿主工作目录）。
     */
    private final static String MODULE_WORKING_PREFIX = "modules/";

    /**
     * 生成结构化结果时的默认超时（毫秒）。
     */
    private final static long DEFAULT_STRUCTURED_TIMEOUT = 3 * 60 * 1000L;

    /**
     * AIGC 服务。
     */
    private final AIGCService service;

    /**
     * 模块注册表，用于兄弟模块查找。
     */
    private final ModuleRegistry registry;

    @Override
    public User getUserById(long uid) {
        return this.service.getUser(uid);
    }

    /**
     * 进程内唯一的 TF-IDF 分析器，延迟创建。
     *
     * <p>见 {@link #acquireAnalyzer()}：其语料与权重表是进程级静态缓存，
     * 多实例会导致并发重复加载。</p>
     */
    private volatile TFIDFAnalyzer analyzer;

    @Override
    public User getUser(String tokenCode) {
        if (null == tokenCode) {
            return null;
        }

        return this.service.getUser(tokenCode);
    }

    @Override
    public AIGCUnit selectUnitForContact(String capabilityName, long contactId) {
        if (null == capabilityName) {
            return null;
        }

        return this.service.selectUnitByName(capabilityName, contactId);
    }

    @Override
    public AIGCChannel createChannelByCode(String tokenCode, String participant, String channelCode,
            Language language) {
        if (null == tokenCode) {
            return null;
        }

        return this.service.createChannel(tokenCode, participant, channelCode, language);
    }

    @Override
    public List<Keyword> extractWeightedKeywords(String content, int topN) {
        if (null == content || content.isEmpty() || topN <= 0) {
            return Collections.emptyList();
        }

        TFIDFAnalyzer analyzer = this.acquireAnalyzer();
        if (null == analyzer) {
            return Collections.emptyList();
        }

        try {
            List<Keyword> words = analyzer.analyze(content, topN);
            return (null == words) ? Collections.<Keyword>emptyList() : words;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#extractWeightedKeywords - Analyze failed", e);
            return Collections.emptyList();
        }
    }

    /**
     * 构造函数。
     *
     * @param service AIGC 服务。
     * @param registry 模块注册表。
     */
    public AIGCHostImpl(AIGCService service, ModuleRegistry registry) {
        this.service = service;
        this.registry = registry;
    }

    // ───────── 身份与文本 ─────────

    @Override
    public AuthToken resolveToken(String tokenCode) {
        if (null == tokenCode) {
            return null;
        }

        return this.service.getToken(tokenCode);
    }

    @Override
    public boolean isRegistered(String tokenCode) {
        if (null == tokenCode) {
            return false;
        }

        // getUser 内部会依次解析令牌与联系人，二者任一缺失都返回 null
        User user = this.service.getUser(tokenCode);
        return null != user && user.isRegistered();
    }

    @Override
    public boolean isReady() {
        return this.service.isStarted();
    }

    @Override
    public Contact getContact(String tokenCode) {
        if (null == tokenCode) {
            return null;
        }

        return ContactManager.getInstance().getContact(tokenCode);
    }

    @Override
    public Membership getMembership(String domain, long contactId, int state) {
        if (null == domain) {
            return null;
        }

        return ContactManager.getInstance().getMembershipSystem()
                .getMembership(domain, contactId, state);
    }

    @Override
    public int getRemainingUsages(User user, Membership membership) {
        if (null == user) {
            return 0;
        }

        return MemberCenter.getInstance().getRemainingUsages(user, membership);
    }

    @Override
    public ReportPermission allowPredictPainting(String domain, User user, long reportSn) {
        if (null == user) {
            return null;
        }

        return MemberCenter.getInstance().allowPredictPainting(domain, user, reportSn);
    }

    @Override
    public List<String> tokenize(String text) {
        if (null == text || text.isEmpty()) {
            return Collections.emptyList();
        }

        Tokenizer tokenizer = this.service.getTokenizer();
        if (null == tokenizer) {
            Logger.w(this.getClass(), "#tokenize - Tokenizer is NOT available");
            return Collections.emptyList();
        }

        // SegToken 是 service 类型，不能出现在 SPI 上，故在此收敛为词列表
        List<SegToken> tokens = tokenizer.process(text, Tokenizer.SegMode.INDEX);
        if (null == tokens || tokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> words = new ArrayList<>(tokens.size());
        for (SegToken token : tokens) {
            if (null != token && null != token.word) {
                words.add(token.word);
            }
        }

        return words;
    }

    @Override
    public List<String> segmentWords(String text) {
        if (null == text || text.isEmpty()) {
            return Collections.emptyList();
        }

        Tokenizer tokenizer = this.service.getTokenizer();
        if (null == tokenizer) {
            Logger.w(this.getClass(), "#segmentWords - Tokenizer is NOT available");
            return Collections.emptyList();
        }

        // 走 sentenceProcess 而非 process(INDEX)：后者会额外插入命中词典的
        // 2/3-gram，调用方按整词做代词改写时会被破坏词形边界
        return tokenizer.sentenceProcess(text);
    }

    @Override
    public List<String> extractKeywords(String content, int topN) {
        if (null == content || content.isEmpty() || topN <= 0) {
            return Collections.emptyList();
        }

        TFIDFAnalyzer analyzer = this.acquireAnalyzer();
        if (null == analyzer) {
            return Collections.emptyList();
        }

        try {
            List<String> words = analyzer.analyzeOnlyWords(content, topN);
            return (null == words) ? Collections.<String>emptyList() : words;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#extractKeywords - Analyze failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    public AIGCUnit selectIdleUnit(String capabilityName) {
        if (null == capabilityName) {
            return null;
        }

        return this.service.selectIdleUnitByName(capabilityName);
    }

    /**
     * 取得进程内唯一的 TF-IDF 分析器。
     *
     * <p><b>必须复用单例</b>：{@link TFIDFAnalyzer} 内部持有进程级静态的
     * IDF 权重表与停用词表，且其懒加载<b>不是同步的</b>——
     * 若每个动作各自新建分析器，并发首调会重复加载整份语料。
     * 这里用双检锁保证只初始化一次。</p>
     *
     * @return 返回分析器；宿主分词器不可用时返回 <code>null</code>。
     */
    private TFIDFAnalyzer acquireAnalyzer() {
        TFIDFAnalyzer result = this.analyzer;
        if (null != result) {
            return result;
        }

        synchronized (this) {
            result = this.analyzer;
            if (null != result) {
                return result;
            }

            Tokenizer tokenizer = this.service.getTokenizer();
            if (null == tokenizer) {
                Logger.w(this.getClass(), "#acquireAnalyzer - Tokenizer is NOT available");
                return null;
            }

            result = new TFIDFAnalyzer(tokenizer);
            this.analyzer = result;
            return result;
        }
    }

    // ───────── 频道 ─────────

    @Override
    public AIGCChannel getChannelByToken(String tokenCode) {
        if (null == tokenCode) {
            return null;
        }

        return this.service.getChannelByToken(tokenCode);
    }

    @Override
    public AIGCChannel acquireChannel(AuthToken token, String participant, String channelCode,
            Language language) {
        if (null == token) {
            return null;
        }

        return this.service.createChannel(token, participant, channelCode, language);
    }

    @Override
    public AIGCChannel requestChannel(String tokenCode, String participant) {
        if (null == tokenCode) {
            return null;
        }

        return this.service.requestChannel(tokenCode, participant);
    }

    @Override
    public AIGCChannel stopChannel(String channelCode) {
        if (null == channelCode) {
            return null;
        }

        return this.service.stopProcessing(channelCode);
    }

    // ───────── 模型单元 ─────────

    @Override
    public AIGCUnit selectUnit(String capabilityName) {
        if (null == capabilityName) {
            return null;
        }

        return this.service.selectUnitByName(capabilityName);
    }

    @Override
    public AIGCUnit selectUnitBySubtask(String subtask) {
        if (null == subtask) {
            return null;
        }

        return this.service.selectUnitBySubtask(subtask);
    }

    @Override
    public int countUnits(String capabilityName) {
        if (null == capabilityName) {
            return 0;
        }

        return this.service.numUnitsByName(capabilityName);
    }

    @Override
    public boolean hasUnit(String capabilityName) {
        if (null == capabilityName) {
            return false;
        }

        return this.service.hasUnit(capabilityName);
    }

    @Override
    public AtomicInteger increaseUnitCounter(String unitName) {
        if (null == unitName) {
            Logger.w(this.getClass(), "#increaseUnitCounter - Unit name is NULL");
            return null;
        }

        return this.service.increaseUnitCounter(unitName);
    }

    @Override
    public GeneratingRecord syncGenerateText(AIGCUnit unit, String prompt, GeneratingOption option,
            List<GeneratingRecord> history, Contact participantContact) {
        if (null == unit) {
            Logger.w(this.getClass(), "#syncGenerateText - Unit is NULL");
            return null;
        }

        return this.service.syncGenerateText(unit, prompt, option, history, participantContact);
    }

    @Override
    public GeneratingRecord syncGenerateText(AuthToken token, String capabilityName, String prompt,
            GeneratingOption option, List<GeneratingRecord> history, Contact participantContact) {
        if (null == token) {
            Logger.w(this.getClass(), "#syncGenerateText - Token is NULL");
            return null;
        }

        return this.service.syncGenerateText(token, capabilityName, prompt, option, history, participantContact);
    }

    /**
     * 生成可校验的结构化结果。
     *
     * <p><b>本方法只做三件事</b>，不含任何业务判断：把 schema 作为约束条件并入提示词
     * → 复用同步文本生成 → 从自由文本中提取第一个可解析的 JSON 值。
     * 提取失败返回 <code>null</code> 并记 WARN，由模块决定是否重试。</p>
     *
     * <p>之所以不引入 JSON Schema 校验库：本工程无依赖解析机制（ant + 手写 classpath），
     * 新增三方 jar 会影响发布产物；且模型输出的结构合规性由提示词约束加解析校验
     * 已能满足「巡检任务计划」这类场景，真需要强校验时再升级 SPI 版本引入。</p>
     */
    @Override
    public JSONObject generateStructured(AIGCUnit unit, String prompt, String jsonSchema,
            String schemaName, long timeoutMs) {
        if (null == unit || null == prompt) {
            Logger.w(this.getClass(), "#generateStructured - Unit or prompt is NULL");
            return null;
        }

        String schemaPrompt = prompt;
        if (null != jsonSchema && !jsonSchema.trim().isEmpty()) {
            schemaPrompt = prompt + "\n\n请仅输出符合下述 JSON Schema 的 JSON，不要输出任何解释文字：\n"
                    + jsonSchema;
        }

        GeneratingOption option = new GeneratingOption();
        GeneratingRecord record = this.service.syncGenerateText(unit, schemaPrompt, option, null, null);
        if (null == record) {
            Logger.w(this.getClass(), "#generateStructured - Generate failed, schema: " + schemaName);
            return null;
        }

        Object parsed = this.extractJson(record.answer);
        if (null == parsed) {
            Logger.w(this.getClass(), "#generateStructured - Can NOT extract JSON from the response,"
                    + " schema: " + schemaName);
            return null;
        }

        // 契约声明返回 JSONObject：数组形态按「首个对象元素」归一，避免模块侧做类型分支
        if (parsed instanceof JSONObject) {
            return (JSONObject) parsed;
        }

        JSONArray array = (JSONArray) parsed;
        return (array.length() > 0 && array.opt(0) instanceof JSONObject)
                ? array.getJSONObject(0) : new JSONObject();
    }

    /**
     * 从模型自由文本中提取第一个可解析的 JSON 值。
     *
     * <p>模型常在 JSON 前后附带说明文字或代码块围栏，因此不直接整体解析，
     * 而是从首个 <code>{</code> 或 <code>[</code> 起做括号配平截取。</p>
     *
     * @param text 模型输出文本。
     * @return 返回解析后的对象或数组；未找到合法 JSON 时返回 <code>null</code>。
     */
    private Object extractJson(String text) {
        if (null == text) {
            return null;
        }

        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        char open = trimmed.charAt(0);
        if (open == '{' || open == '[') {
            Object parsed = this.tryParse(trimmed);
            if (null != parsed) {
                return parsed;
            }
        }

        // 回退：括号配平截取
        int start = -1;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{' || c == '[') {
                start = i;
                break;
            }
        }

        if (start < 0) {
            return null;
        }

        char begin = trimmed.charAt(start);
        char end = ('{' == begin) ? '}' : ']';

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                }
                else if ('\\' == c) {
                    escaped = true;
                }
                else if ('"' == c) {
                    inString = false;
                }
                continue;
            }

            if ('"' == c) {
                inString = true;
            }
            else if (c == begin) {
                ++depth;
            }
            else if (c == end) {
                --depth;
                if (0 == depth) {
                    return this.tryParse(trimmed.substring(start, i + 1));
                }
            }
        }

        return null;
    }

    @Override
    public FileLabel downloadFile(AuthToken authToken, String fileUrl) {
        if (null == authToken || null == fileUrl) {
            return null;
        }

        return this.service.downloadFile(authToken, fileUrl);
    }

    /**
     * 尝试把文本解析为 JSON 对象或数组。
     *
     * @param text 待解析文本。
     * @return 返回解析结果；失败时返回 <code>null</code>。
     */
    private Object tryParse(String text) {
        try {
            String trimmed = text.trim();
            if (trimmed.startsWith("{")) {
                return new JSONObject(trimmed);
            }

            if (trimmed.startsWith("[")) {
                return new JSONArray(trimmed);
            }
        } catch (Exception e) {
            // 交给调用方继续回退，不在此处记录，避免同一段文本产生多条噪声日志
        }

        return null;
    }

    @Override
    public ActionDialect invokeUnit(AIGCUnit unit, String action, JSONObject data, long timeoutMs) {
        if (null == unit || null == action) {
            Logger.w(this.getClass(), "#invokeUnit - Unit or action is NULL");
            return null;
        }

        AIGCCellet cellet = this.service.getCellet();
        if (null == cellet) {
            Logger.w(this.getClass(), "#invokeUnit - Cellet is NOT available");
            return null;
        }

        long timeout = (timeoutMs > 0) ? timeoutMs : DEFAULT_STRUCTURED_TIMEOUT;

        try {
            cube.common.Packet request = new cube.common.Packet(action, (null != data) ? data : new JSONObject());
            return cellet.transmit(unit.getContext(), request.toDialect(), timeout);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#invokeUnit - Invoke failed, action: " + action
                    + ", unit: " + unit.getCapability().getName(), e);
            return null;
        }
    }

    // ───────── 文件与资源 ─────────

    @Override
    public FileLabel getFile(String domain, String fileCode) {
        if (null == domain || null == fileCode) {
            return null;
        }

        return this.service.getFile(domain, fileCode);
    }

    @Override
    public File loadFile(String domain, String fileCode) {
        if (null == domain || null == fileCode) {
            return null;
        }

        return this.service.loadFile(domain, fileCode);
    }

    @Override
    public FileLabel saveFile(AuthToken token, String fileCode, File file, String filename,
            boolean deleteAfterSave) {
        if (null == token) {
            Logger.w(this.getClass(), "#saveFile - Auth token is NULL");
            return null;
        }

        return this.service.saveFile(token, fileCode, file, filename, deleteAfterSave);
    }

    @Override
    public FileLabel deleteFile(String domain, String fileCode) {
        if (null == domain || null == fileCode) {
            return null;
        }

        return this.service.deleteFile(domain, fileCode);
    }

    @Override
    public File getModuleWorkingPath(String moduleName) {
        if (null == moduleName || moduleName.trim().isEmpty()) {
            Logger.w(this.getClass(), "#getModuleWorkingPath - Module name is invalid");
            return null;
        }

        File path = new File(this.service.getWorkingPath(), MODULE_WORKING_PREFIX + moduleName.trim());
        if (!path.exists() && !path.mkdirs()) {
            Logger.w(this.getClass(), "#getModuleWorkingPath - Can NOT create: " + path.getAbsolutePath());
        }

        return path;
    }

    @Override
    public String readModuleResource(String moduleName, String relativePath) {
        if (null == moduleName || moduleName.trim().isEmpty()
                || null == relativePath || relativePath.trim().isEmpty()) {
            return null;
        }

        String name = moduleName.trim();
        String relative = relativePath.trim();

        // 新路径优先
        File file = new File(MODULE_ASSET_PREFIX + name + "/" + relative);
        if (file.exists()) {
            return FileUtils.readTextFile(file.getAbsolutePath());
        }

        // 回退到旧资源路径，兼容既有部署
        File legacy = new File(LEGACY_MODULE_ASSET_PREFIX + name + "/" + relative);
        if (legacy.exists()) {
            Logger.d(this.getClass(), "#readModuleResource - Fallback to legacy asset path: "
                    + legacy.getAbsolutePath());
            return FileUtils.readTextFile(legacy.getAbsolutePath());
        }

        return null;
    }

    @Override
    public String getGuidePrompt(String promptName) {
        if (null == promptName || promptName.trim().isEmpty()) {
            return null;
        }

        return Prompts.getPrompt(promptName.trim());
    }

    // ───────── 存储 ─────────

    @Override
    public Storage openModuleStorage(String storageName, StorageType type, JSONObject config) {
        if (null == storageName || null == type) {
            Logger.w(this.getClass(), "#openModuleStorage - Storage name or type is NULL");
            return null;
        }

        try {
            return StorageFactory.getInstance().createStorage(type, storageName, config);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#openModuleStorage - Open failed: " + storageName, e);
            return null;
        }
    }

    @Override
    public List<cube.aigc.Usage> queryUsages(long contactId) {
        List<cube.aigc.Usage> usages = this.service.queryContactUsages(contactId);
        return (null != usages) ? usages : Collections.emptyList();
    }

    @Override
    public List<cube.aigc.AppEvent> queryAppEvents(long contactId, String eventName, int limit) {
        if (null == eventName || limit <= 0) {
            return Collections.emptyList();
        }

        try {
            List<cube.aigc.AppEvent> events = this.service.getStorage().readAppEvents(contactId, eventName, limit);
            return (null != events) ? events : Collections.emptyList();
        } catch (Exception e) {
            Logger.e(this.getClass(), "#queryAppEvents - Read failed, event: " + eventName, e);
            return Collections.emptyList();
        }
    }

    // ───────── 调度与横切 ─────────

    /**
     * 提交延迟任务。
     *
     * <p><b>实现约束</b>：零延迟任务路由到 {@link AIGCCellet#getExecutor()}
     * （即单元级 {@code sExecutor}），这与插件动作处理器
     * （{@link ActionRunner}）运行在同一个池上，即「模块代码一律在单元池执行」，
     * 语义一致。</p>
     *
     * <p>之所以不用服务级后台池（{@code AIGCService#getExecutor()}）：该池由约 90 处
     * 短任务共用且为共享 FIFO，插件的长阻塞任务会堵死它们。之所以不用单元独占队列：
     * 它要求 {@code UnitMeta} 元任务，与本方法的裸 {@link Runnable} 语义不匹配，
     * 因此模块需自行控制并发度。</p>
     *
     * <p>{@code delayMs} 大于 0 时由本类自建的延迟执行器承担——单元池是普通
     * {@code ExecutorService}，无延迟能力。</p>
     */
    @Override
    public void schedule(String taskKey, long delayMs, Runnable job) {
        if (null == job) {
            return;
        }

        if (delayMs <= 0) {
            AIGCCellet cellet = this.service.getCellet();
            java.util.concurrent.ExecutorService executor = (null != cellet) ? cellet.getExecutor() : null;
            if (null == executor) {
                Logger.w(this.getClass(), "#schedule - Cellet executor is NOT available, task \""
                        + taskKey + "\" is dropped");
                return;
            }

            try {
                executor.execute(() -> {
                    try {
                        job.run();
                    } catch (Exception e) {
                        Logger.e(this.getClass(), "#schedule - Task \"" + taskKey + "\" failed", e);
                    }
                });
            } catch (Exception e) {
                Logger.e(this.getClass(), "#schedule - Can NOT submit task \"" + taskKey + "\"", e);
            }

            return;
        }

        this.scheduleDelayed(taskKey, delayMs, job);
    }

    /**
     * 延迟任务执行器。
     *
     * <p>宿主后台池无延迟能力，故本类自建一个单线程延迟执行器：模块的延迟任务
     * 本身是低频的（超时检查、定时刷新），单线程足够，且能避免多模块各自起池。</p>
     */
    private volatile java.util.concurrent.ScheduledExecutorService delayExecutor;

    // ───────── 会话与语音 ─────────

    @Override
    public AIGCChannel getChannel(String channelCode) {
        if (null == channelCode) {
            return null;
        }

        return this.service.getChannel(channelCode);
    }

    @Override
    public AIGCChannel createChannel(AuthToken authToken, String participant, String channelCode,
            Language language) {
        if (null == authToken) {
            return null;
        }

        return this.service.createChannel(authToken, participant, channelCode, language);
    }

    @Override
    public void generateText(AIGCChannel channel, AIGCUnit unit, String query, String prompt, GeneratingOption option,
            List<GeneratingRecord> histories, int maxHistories, List<Attachment> attachments,
            List<String> categories, boolean recordable, GenerateTextListener listener) {
        if (null == channel || null == unit) {
            return;
        }

        this.service.generateText(channel, unit, query, prompt, option, histories, maxHistories,
                attachments, categories, recordable, listener);
    }

    @Override
    public FileLabel performSpeakerDiarization(AuthToken authToken, FileLabel fileLabel, boolean preprocess,
            boolean storage, boolean jumpToFirst, VoiceDiarizationListener listener) {
        if (null == authToken || null == fileLabel) {
            return null;
        }

        return this.service.performSpeakerDiarization(authToken, fileLabel, preprocess, storage, jumpToFirst, listener);
    }

    // ───────── 对话历史与图表 ─────────

    @Override
    public void writeChatHistory(AIGCChatHistory history) {
        if (null == history) {
            return;
        }

        this.service.getStorage().writeHistory(history);
    }

    @Override
    public List<AIGCChatHistory> readChatHistories(long contactId, String domain, long startTime, long endTime) {
        return this.service.getStorage().readHistoriesByContactId(contactId, domain, startTime, endTime);
    }

    @Override
    public Chart readLastChart(String name) {
        if (null == name) {
            return null;
        }

        return this.service.getStorage().readLastChart(name);
    }

    @Override
    public boolean insertChart(Chart chart) {
        if (null == chart) {
            return false;
        }

        return this.service.getStorage().insertChart(chart);
    }

    @Override
    public boolean writeCounselingRecording(AuthToken authToken, String streamName, long timestamp, long duration,
            Attribute attribute, ConsultationTheme theme, String fileCode) {
        if (null == authToken) {
            return false;
        }

        return this.service.getStorage().writeCounselingRecording(authToken, streamName, timestamp, duration,
                attribute, theme, fileCode);
    }

    /**
     * 取得计算机视觉服务实例。
     *
     * <p>该服务由独立的视觉单元安装到内核模块表，类型为 {@code AbstractModule}，
     * 调用前必须做类型转换——此处失败即表示视觉单元未部署。</p>
     *
     * @return 返回视觉服务；未部署时返回 {@code null}。
     */
    private CVService acquireCVService() {
        Module module = this.service.getKernel().getModule(CVService.NAME);
        if (module instanceof CVService) {
            return (CVService) module;
        }

        Logger.w(this.getClass(), "#acquireCVService - CV module is NOT available");
        return null;
    }

    /**
     * 构造一个仅含存储域的访问令牌。
     *
     * <p>视觉服务只需要令牌中的存储域信息（用于定位文件），不需要具体联系人身份。</p>
     *
     * @param domain 存储域。
     * @return 返回访问令牌。
     */
    private AuthToken makeAuthToken(String domain) {
        return new AuthToken("", domain, "", 0L, System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000L, false);
    }

    /**
     * 获取心理学场景单例。
     *
     * <p>报告运行态（生成中报告的内存表与任务队列）由该单例持有，
     * 是本组方法的唯一数据来源。</p>
     *
     * <p><b>场景的装配由模块的 setup 完成</b>，早于任何一次动作派发；
     * 但若模块因配置缺失等原因未装载成功，场景会保持未装配状态。
     * 此时返回 <code>null</code>，由调用方判空后回明确失败，
     * 而不是让空引用异常被动作执行层 catch 成无信息的
     * {@code Failure}。</p>
     *
     * @return 返回已装配的场景；未装配时返回 <code>null</code>。
     */
    private PsychologyScene scene() {
        PsychologyScene scene = PsychologyScene.getInstance();
        if (!scene.isReady()) {
            Logger.e(AIGCHostImpl.class, "#scene - Psychology scene is NOT ready, "
                    + "the business module is probably NOT loaded");
            return null;
        }

        return scene;
    }

    /**
     * 获取宿主服务，供派生能力按需使用。
     *
     * <p>仅供宿主内部桥接类使用，<b>不得</b>暴露给模块——否则插件可经此拿到
     * service 类型，依赖方向约束随即失效。</p>
     *
     * @return 返回 AIGC 服务。
     */
    public AIGCService getService() {
        return this.service;
    }

    /**
     * 按钩子键取钩子实例。
     *
     * <p>钩子键常量位于 service 模块，SPI 只能传裸字符串，故在此做显式映射；
     * 未知键返回 <code>null</code> 而非抛异常，避免插件拼错键导致主服务中断。</p>
     *
     * @param hookKey 钩子键。
     * @return 返回钩子实例；键未知时返回 <code>null</code>。
     */
    private AIGCHook resolveHook(String hookKey) {
        if (AIGCHook.AppEvent.equalsIgnoreCase(hookKey)) {
            return this.service.getPluginSystem().getAppEventHook();
        }
        else if (AIGCHook.ImportKnowledgeDoc.equalsIgnoreCase(hookKey)) {
            return this.service.getPluginSystem().getImportKnowledgeDocHook();
        }
        else if (AIGCHook.RemoveKnowledgeDoc.equalsIgnoreCase(hookKey)) {
            return this.service.getPluginSystem().getRemoveKnowledgeDocHook();
        }
        else if (AIGCHook.TaskProcessing.equalsIgnoreCase(hookKey)) {
            return this.service.getPluginSystem().getTaskProcessingHook();
        }

        return null;
    }

    /**
     * 把 SPI 侧上下文翻译成宿主侧上下文。
     *
     * <p>翻译而非直接传递的原因：钩子消费者（如风控的 AI 任务留痕）会强转为
     * service 侧上下文类型，若直接传递 SPI 上下文将抛出类型转换异常。</p>
     *
     * @param lite SPI 侧上下文。
     * @return 返回宿主侧上下文。
     */
    private AIGCPluginContext translate(AIGCPluginContextLite lite) {
        AIGCPluginContext context = new AIGCPluginContext(lite.getAuthToken(), lite.getTask());
        context.setUnit(lite.getUnit());
        context.setInputTokens(lite.getInputTokens());
        context.setOutputTokens(lite.getOutputTokens());

        for (String fileCode : lite.getFileCodeList()) {
            FileLabel fileLabel = this.service.getFile(lite.getAuthToken().getDomain(), fileCode);
            if (null != fileLabel) {
                context.addFileLabel(fileLabel);
            }
        }

        return context;
    }

    @Override
    public void fireHook(String hookKey, AIGCPluginContextLite context) {
        if (null == hookKey || null == context) {
            Logger.w(this.getClass(), "#fireHook - Hook key or context is NULL");
            return;
        }

        AIGCHook hook = this.resolveHook(hookKey);
        if (null == hook) {
            Logger.w(this.getClass(), "#fireHook - Unknown hook key: " + hookKey);
            return;
        }

        hook.apply(this.translate(context));
    }

    @Override
    public boolean fireEvent(cube.aigc.AppEvent appEvent) {
        if (null == appEvent) {
            return false;
        }

        return this.service.fireEvent(appEvent);
    }

    @Override
    public void writeUsage(cube.aigc.TaskDescriptor descriptor) {
        if (null == descriptor) {
            Logger.w(this.getClass(), "#writeUsage - Descriptor is NULL");
            return;
        }

// 留痕由「任务处理」钩子的消费者完成（含风控侧按 domain 分表写入），
// 因此此处不直接写库，而是把描述符还原成上下文后触发钩子。
// 注意：描述符持有的是令牌码而非令牌对象，需先解析才能构造上下文。
AuthToken authToken = this.resolveToken(descriptor.token);
if (null == authToken) {
    Logger.w(this.getClass(), "#writeUsage - Can NOT resolve token: " + descriptor.token);
    return;
}

AIGCPluginContextLite lite = new AIGCPluginContextLite(authToken, descriptor.task);
lite.setInputTokens(descriptor.getInputTokens());
lite.setOutputTokens(descriptor.getOutputTokens());

// 描述符只保存单元 ID，而宿主上下文要求单元对象，故单元字段不在此还原：
// 钩子消费者若需要单元信息，应从模块自身的调用期上下文取，而非依赖此处回填。

this.fireHook(AIGCHook.TaskProcessing, lite);
}

    @Override
    public <T> T getSiblingModule(String moduleName, Class<T> type) {
        if (null == moduleName || null == type || null == this.registry) {
            return null;
        }

        ActionModule module = this.registry.findModule(moduleName);
        if (null == module) {
            Logger.w(this.getClass(), "#getSiblingModule - No such module: " + moduleName);
            return null;
        }

        if (!type.isInstance(module)) {
            Logger.w(this.getClass(), "#getSiblingModule - Module \"" + moduleName + "\" is NOT a "
                    + type.getName());
            return null;
        }

        return type.cast(module);
    }

    @Override
    public JSONObject queryPaintingReport(long sn, String format) {
        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }
        PaintingReport report = scene.getPaintingReport(sn);
        if (null == report) {
            return null;
        }

        try {
            JSONObject json;
            if ("markdown".equalsIgnoreCase(format)) {
                json = report.exportMarkdown();
            }
            else if ("sections".equalsIgnoreCase(format)) {
                json = report.exportReportSectionJSON();
            }
            else {
                json = report.toCompactJSON();
                // 所在队列位置：仅摘要形态需要，与迁移前一致
                json.put("queuePosition", scene.getGeneratingQueuePosition(sn));
            }

            return json;
        } catch (Exception e) {
            Logger.e(this.getClass(), "#queryPaintingReport - Export failed, sn: " + sn, e);
            return null;
        }
    }

    @Override
    public PaintingReport getPaintingReport(long sn) {
        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }
        return scene.getPaintingReport(sn);
    }

    @Override
    public JSONObject queryScaleReport(long sn) {
        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }
        ScaleReport report = scene.getScaleReport(sn);
        return (null == report) ? null : report.toJSON();
    }

    @Override
    public JSONObject listPaintingReports(long contactId, int page, int size, boolean descending, int state) {
        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }
        int num = (state == -1) ? scene.numPaintingReports(contactId)
                : scene.numPaintingReports(contactId, state);

        List<PaintingReport> list = (state == -1)
                ? scene.getPaintingReports(contactId, page, size, descending)
                : scene.getPaintingReportsWithState(contactId, page, size, descending, state);

        JSONArray array = new JSONArray();
        for (PaintingReport report : list) {
            array.put(report.toCompactJSON());
        }

        JSONObject data = new JSONObject();
        data.put("total", num);
        data.put("page", page);
        data.put("size", size);
        data.put("list", array);

        return data;
    }

    @Override
    public JSONObject listScaleReports(long contactId, boolean descending, int state) {
        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }
        int num = (state == -1) ? scene.numScaleReports(contactId)
                : scene.numScaleReports(contactId, state);

        List<ScaleReport> list = (state == -1)
                ? scene.getScaleReports(contactId, descending)
                : scene.getScaleReports(contactId, state, descending);

        JSONArray array = new JSONArray();
        for (ScaleReport report : list) {
            array.put(report.toCompactJSON());
        }

        JSONObject data = new JSONObject();
        data.put("total", num);
        data.put("list", array);

        return data;
    }

    @Override
    public JSONObject stopReportGeneration(long sn) {
        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }
        PaintingReport report = scene.stopGenerating(sn);
        return (null == report) ? null : report.toCompactJSON();
    }

    @Override
    public JSONObject resetReportAttention(long sn, Integer newAttention) {
        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }
        Attention attention = (null == newAttention) ? null : Attention.parse(newAttention);
        PaintingReport report = scene.resetReportAttention(sn, attention);
        return (null == report) ? null : report.toCompactJSON();
    }

    @Override
    public void fillHexagonScoreDescription(HexagonDimensionScore hds, Language language) {
        if (null == hds) {
            return;
        }

        ContentTools.fillHexagonScoreDescription(this.service.getTokenizer(), hds, language);
    }

    @Override
    public String extractContent(String query) {
        if (null == query || query.isEmpty()) {
            return null;
        }

        return ContentTools.extract(query, this.service.getTokenizer());
    }

    @Override
    public PaintingFeatureSet getPaintingFeatureSet(long reportSn) {
        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }
        return scene.getPaintingFeatureSet(reportSn);
    }

    @Override
    public PaintingReport generatePaintingReport(AIGCChannel channel, Attribute attribute, FileLabel fileLabel,
            Theme theme, int maxIndicators, boolean adjust, int retention, String remark,
            PaintingReportListener listener) {
        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }
        if (null == channel || null == attribute || null == fileLabel) {
            Logger.w(this.getClass(), "#generatePaintingReport - Channel, attribute or file is NULL");
            return null;
        }

        return scene.generatePaintingReport(channel, attribute, fileLabel, theme, maxIndicators,
                adjust, retention, remark, listener);
    }

    @Override
    public ScaleReport generateScaleReport(AIGCChannel channel, Scale scale, Language language,
            ScaleReportListener listener) {
        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }
        if (null == channel || null == scale) {
            Logger.w(this.getClass(), "#generateScaleReport - Channel or scale is NULL");
            return null;
        }

        return scene.generateScaleReport(channel, scale, language, listener);
    }

    @Override
    public Scale getScale(long sn) {
        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }
        return scene.getScale(sn);
    }

    @Override
    public ObjectInfo detectObject(String domain, String fileCode, boolean visualize) {
        if (null == domain || null == fileCode) {
            return null;
        }

        CVService cvService = this.acquireCVService();
        if (null == cvService) {
            return null;
        }

        try {
            return cvService.detectObject(this.makeAuthToken(domain), fileCode, visualize);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#detectObject - Detect failed, fileCode: " + fileCode, e);
            return null;
        }
    }

    @Override
    public JSONObject getPaintingInferenceData(long sn) {
        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }
        // 绘画推理数据不依赖令牌，故不传
        return scene.getPaintingInferenceData(null, sn);
    }

    @Override
    public Painting getPredictedPainting(AuthToken token, String fileCode) {
        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }
        if (null == token || null == fileCode) {
            return null;
        }

        return scene.getPredictedPainting(token, fileCode);
    }

    @Override
    public FileLabel getPredictedPainting(AuthToken token, long sn, boolean boundingBox,
            boolean visualParam, double probability) {
        if (null == token) {
            return null;
        }

        PsychologyScene scene = this.scene();
        if (null == scene) {
            return null;
        }

        return scene.getPredictedPainting(token, sn, boundingBox, visualParam, probability);
    }

    @Override
    public String generatePersonalKnowledge(String tokenCode, String query, boolean english) {
        if (null == query) {
            return null;
        }

        return this.service.generatePersonalKnowledge(tokenCode, query, english);
    }

    @Override
    public File getWorkingPath() {
        return this.service.getWorkingPath();
    }

    @Override
    public boolean matchSimilarity(FileLabel fileLabel, List<String> templateNames,
            MatchSimilarityListener listener) {
        if (null == fileLabel || null == templateNames || templateNames.isEmpty()) {
            return false;
        }

        CVService cvService = this.acquireCVService();
        if (null == cvService) {
            return false;
        }

        return cvService.matchSimilarity(fileLabel, templateNames, listener);
    }

    @Override
    public boolean semanticSearch(String query, SemanticSearchListener listener) {
        if (null == query) {
            return false;
        }

        return this.service.semanticSearch(query, listener);
    }

    /**
     * 提交延迟任务。
     *
     * @param taskKey 任务键，用于日志。
     * @param delayMs 延迟（毫秒）。
     * @param job 任务体。
     */
    private void scheduleDelayed(String taskKey, long delayMs, Runnable job) {
        java.util.concurrent.ScheduledExecutorService executor = this.delayExecutor;
        if (null == executor) {
            synchronized (this) {
                executor = this.delayExecutor;
                if (null == executor) {
                    executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread thread = new Thread(r, "AIGC-module-delay");
                        thread.setDaemon(true);
                        return thread;
                    });
                    this.delayExecutor = executor;
                }
            }
        }

        try {
            executor.schedule(() -> {
                try {
                    job.run();
                } catch (Exception e) {
                    Logger.e(this.getClass(), "#scheduleDelayed - Task \"" + taskKey + "\" failed", e);
                }
            }, delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Logger.e(this.getClass(), "#scheduleDelayed - Can NOT schedule task \"" + taskKey + "\"", e);
        }
    }

    /**
     * 关闭延迟任务执行器。
     *
     * <p>由宿主在服务停止时调用，避免线程泄漏。</p>
     */
    public void shutdown() {
        java.util.concurrent.ScheduledExecutorService executor = this.delayExecutor;
        if (null != executor) {
            executor.shutdownNow();
            this.delayExecutor = null;
        }
    }

    @Override
    public File convertWavToMp3(String wavFileName, String mp3FileName) {
        if (null == wavFileName || null == mp3FileName) {
            return null;
        }

        File workingPath = this.service.getWorkingPath();
        if (null == workingPath) {
            return null;
        }

        WavToMp3Context context = new WavToMp3Context(wavFileName, mp3FileName,
                AudioUtils.SAMPLE_RATE, AudioUtils.CHANNELS);
        AudioProcessor processor = new AudioProcessor(workingPath.toPath());
        processor.go(context);

        return context.isSuccessful() ? new File(workingPath, mp3FileName) : null;
    }

    @Override
    public FileLabel saveFileWithContext(AuthToken token, String fileCode, File file, String filename,
            boolean deleteAfterSave, JSONObject context) {
        if (null == token || null == file) {
            return null;
        }

        return this.service.saveFile(token, fileCode, file, filename, deleteAfterSave, context);
    }
}