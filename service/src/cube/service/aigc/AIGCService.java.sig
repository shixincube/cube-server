public AIGCCellet getCellet() {
public AIGCChannel createChannel(AuthToken authToken, String participant, String channelCode, Language language) {
public AIGCChannel createChannel(String token, String participant, String channelCode, Language language) {
public AIGCChannel getChannel(String channelCode) {
public AIGCChannel getChannelByToken(String tokenCode) {
public AIGCChannel requestChannel(String token, String participant) {
public AIGCChannel stopProcessing(String channelCode) {
public AIGCPluginSystem getPluginSystem() {
public AIGCService(AIGCCellet cellet) {
public AIGCStorage getStorage() {
public AIGCUnit selectIdleUnitByName(String unitName) {
public AIGCUnit selectUnitByName(String unitName) {
public AIGCUnit selectUnitByName(String unitName, long cid) {
public AIGCUnit selectUnitBySubtask(String subtask) {
public AtomicInteger increaseUnitCounter(String unitName) {
public AuthToken getOrInjectAuthToken(String phoneNumber, String userName) {
public AuthToken getToken(String tokenCode) {
public ComplexContext preInfer(String token, String content) {
public ComplexContext recognizeContext(String text, AuthToken authToken) {
public ContactPreference getPreference(long contactId) {
public EventResult submitEvent(Event event) {
public ExecutorService getExecutor() {
public File getWorkingPath() {
public File loadFile(String domain, String fileCode) {
public FileLabel deleteFile(String domain, String fileCode) {
public FileLabel downloadFile(AuthToken authToken, String fileUrl) {
public FileLabel getFile(String domain, String fileCode) {
public FileLabel getVoiceStreamFile(AuthToken authToken, String streamName) {
public FileLabel performSpeakerDiarization(AuthToken authToken, FileLabel fileLabel, boolean preprocess,
public FileLabel performSpeakerDiarization(AuthToken authToken, String fileCodeOrUrl, boolean preprocess,
public FileLabel saveFile(AuthToken authToken, String fileCode, File file, String filename, boolean deleteAfterSave) {
public FileLabel saveFile(AuthToken authToken, String fileCode, File file, String filename, boolean deleteAfterSave,
public GeneratingRecord generateText(String unitName, String prompt, GeneratingOption option,
public GeneratingRecord syncGenerateText(AIGCUnit unit, String prompt, GeneratingOption option,
public GeneratingRecord syncGenerateText(AuthToken authToken, String unitName, String prompt, GeneratingOption option) {
public GeneratingRecord syncGenerateText(AuthToken authToken, String unitName, String prompt, GeneratingOption option,
public GeneratingRecord syncGenerateText(String unitName, String prompt, GeneratingOption option,
public KnowledgeBase getKnowledgeBase(String tokenCode, String baseName) {
public KnowledgeFramework getKnowledgeFramework() {
public List<AIGCChannel> getAllChannels() {
public List<AIGCUnit> getAllUnits() {
public List<AIGCUnit> setupUnit(Contact contact, List<AICapability> capabilities, TalkContext context) {
public List<AIGCUnit> teardownUnit(Contact contact) {
public List<EmotionRecord> getEmotionRecords(AuthToken authToken) {
public List<KnowledgeBase> getKnowledgeBaseByCategory(String tokenCode, String category) {
public List<KnowledgeBaseInfo> getKnowledgeBaseInfoList(String tokenCode) {
public List<ModelConfig> getModelConfigs() {
public List<ModelConfig> getModelConfigs(JSONArray modelNames) {
public List<Notification> getNotifications() {
public List<RetrieveReRankResult> syncRetrieveReRank(List<FileLabel> fileLabels, String query) {
public List<String> segmentText(String text) {
public List<Usage> queryContactUsages(long contactId) {
public List<VoiceDiarization> getVoiceDiarizations(AuthToken authToken) {
public Map<String, AtomicInteger> getGenerateTextUnitRealtimeCount() {
public Membership activateMembership(AuthToken token, String channel, String invitationCode) {
public Membership cancelMembership(AuthToken token) {
public PromptComposer getPromptComposer() {
public SkillRegistry getSkillRegistry() {
public SkillSessionStore getSkillSessionStore() {
public String newInvitationForToken(String token) {
public String performSpeechAnalysis(AuthToken authToken, String fileCode, String templateName,
public String queryTokenByInvitation(String invitationCode) {
public TokenEstimator getTokenEstimator() {
public Tokenizer getTokenizer() {
public User checkInUser(Contact contact, VerificationCode verificationCode) {
public User checkInUser(boolean register, String userName, String password, Contact contact) {
public User createUser(String appAgent, Device device, String channel) {
public User getUser(String token) {
public User getUser(long uid) {
public User modifyUser(String token, UserModification modification) {
public User signOutUser(Contact contact) {
public VoiceDiarization deleteVoiceDiarization(AuthToken authToken, String fileCode) {
public VoiceDiarization getVoiceDiarization(AuthToken authToken, String fileCode) {
public WordCloud createWordCloud(AuthToken authToken) {
public boolean analyseVoiceStream(AuthToken authToken, String fileCode, String streamName, int index,
public boolean automaticSpeechRecognition(AuthToken authToken, String fileCodeOrUrl,
public boolean executeMultimodal(String tokenCode, String channelCode,
public boolean extractKeywords(String text, ExtractKeywordsListener listener) {
public boolean facialExpressionRecognition(AuthToken token, String fileCode, boolean visualize,
public boolean fireEvent(AppEvent appEvent) {
public boolean generateFile(AIGCChannel channel, String text, GeneratingRecord attachment, TextToFileListener listener) {
public boolean generateImage(AIGCChannel channel, String text, String unitName, TextToImageListener listener) {
public boolean generateImage(String channelCode, String text, String unitName, TextToImageListener listener) {
public boolean generateSummarization(String text, SummarizationListener listener) {
public boolean generateText(String channelCode, String content, String unitName, GeneratingOption option,
public boolean hasUnit(String unitName) {
public boolean isSkillAutoEnabled() {
public boolean isSkillCatalogEnabled() {
public boolean keepAliveChannel(String token) {
public boolean retrieveReRank(List<String> queries, RetrieveReRankListener listener) {
public boolean semanticSearch(String query, SemanticSearchListener listener) {
public boolean speechEmotionRecognition(AuthToken token, String fileCode, SpeechEmotionRecognitionListener listener) {
public boolean stopVoiceStream(AuthToken authToken, String streamName) {
public boolean useRelay = false;
public class AIGCService extends AbstractModule implements Generatable {
public double sentenceSimilarity(String sentenceA, String sentenceB) {
public final File workingPath = new File("storage/tmp/");
public final static String NAME = "AIGC";
public int getSkillAutoLimit() {
public int numUnitsByName(String unitName) {
public synchronized KnowledgeBase getKnowledgeBase(Long contactId, String baseName) {
public synchronized List<KnowledgeBase> getKnowledgeBaseByCategory(Long contactId, String category) {
public void dispose() {
public void evaluate(String token, long historySN, int feedback) {
public void generateText(AIGCChannel channel, AIGCUnit unit, String query, String prompt, GeneratingOption option,
public void onCompleted(FileLabel source, VoiceDiarization diarization) {
public void onCompleted(List<RetrieveReRankResult> retrieveReRankResults) {
public void onFailed(FileLabel source, AIGCStateCode stateCode) {
public void onFailed(List<String> queries, AIGCStateCode stateCode) {
public void onTick(Module module, Kernel kernel) {
public void run() {
public void start() {
public void stop() {
