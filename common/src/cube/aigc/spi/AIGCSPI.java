/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.spi;

/**
 * AIGC 业务模块 SPI 全局常量。
 *
 * <p>SPI 一旦发布到 {@code deploy/libs/} 即成为业务模块的二进制接口，
 * 任何签名变更都必须递增 {@link #VERSION}。</p>
 */
public final class AIGCSPI {

    /**
     * SPI 契约版本。
     *
     * <p>模块各自编译期固化此值；运行期由
     * {@link ModuleDescriptor#spiVersion} 与本值比对，不匹配即拒绝加载，
     * 不做静默降级。</p>
     *
     * <p><b>变更历史</b>：</p>
     * <ul>
     *   <li>v1：首批契约，含 29 个宿主能力方法；</li>
     *   <li>v2：新增 {@link AIGCHost#isRegistered(String)}。该方法使插件得以
     *       判定「令牌对应的用户是否已注册」，而无需接触 {@code cube.common.entity.User}
     *       或宿主的服务模块。此前 {@code isRegistered()} 依赖
     *       {@code ContactManager}（位于 service 模块），插件不可见，
     *       导致客户/日程类动作无法迁移。</li>
     *   <li>v3：新增 {@link AIGCHost#extractKeywords(String, int)}。主观题答案
     *       评级需按 TF-IDF 权重匹配模型输出与备选项的关键词，而
     *       {@code TFIDFAnalyzer}（位于 service 模块，且依赖宿主
     *       {@code Tokenizer} 与 847KB 语料）插件不可见。此前 SPI 的
     *       {@link AIGCHost#tokenize(String)} 只能返回普通分词，
     *       无法给出权重排序，导致量表类动作无法迁移。</li>
     *   <li>v4：新增第 ⑧ 组「报告运行态」共 6 个方法。报告在生成过程中先落在
     *       宿主的内存表、此刻尚未入库，模块若自行读存储将查不到
     *       「正在生成中」的报告——这是线协议可见的行为回归。
     *       因此整条报告读取与生成控制（停止、重置关注等级）
     *       均经宿主暴露，模块只做协议层、不持有运行态。</li>
     *   <li>v5：新增第 ⑨ 组「报告内容加工」共 7 个方法。报告正文的渲染
     *       （六维描述、指标列表、评级、页面链接、绘画特征）依赖宿主的
     *       {@code ContentTools}，而它虽只有 2 个方法用到宿主分词器，
     *       其余 16 个方法却散布在 14 个调用文件里。整类下沉的改动面远大于
     *       补窄接口，故该类留在宿主，模块经本组方法访问。
     *       另新增 {@link AIGCHost#getPaintingFeatureSet(long)}：特征集与
     *       报告同属「生成中尚未入库」的数据，必须经宿主读取。</li>
 *   <li>v6：新增第 ⑩ 组「计算机视觉与绘画」共 4 个方法。绘画校验需检测
 *       图像中的物体，该能力由宿主转发至计算机视觉服务，模块无法自行实现；
 *       绘画预测则需调用远端绘画单元并可能写回标注图。
 *       另需 {@link AIGCHost#getPaintingInferenceData(long)}：构图图表
 *       依赖报告的运行中内存态。</li>
 *   <li>v7：报告生成工作器迁入模块后补 3 个方法。
 *       {@link AIGCHost#syncGenerateText(AuthToken, String, String, GeneratingOption, List, Contact)}
 *       承载「按联系人亲和选点」的生成：报告生成是同一联系人的连续多段生成，
 *       单元亲和保证上下文缓存命中，而 SPI 既有的
 *       {@link AIGCHost#selectUnit(String)} 是全局选点，二者不等价。
 *       {@link AIGCHost#segmentWords(String)} 承载整句分词：既有的
 *       {@link AIGCHost#tokenize(String)} 走索引模式会额外插入 2/3-gram，
 *       而代词改写需要按完整词元替换，用索引模式会破坏词形边界。
 *       {@link AIGCHost#getGuidePrompt(String)} 承载宿主引导提示词读取：
 *       模块语料与宿主提示词虽同有 <code>FORMAT_POLISH</code>，但措辞不同，
 *       提示词直接影响模型输出，不可互相替代。</li>
 *   <li>v8：新增第 ⑪ 组「报告生成编排」共 2 个方法。
 *       {@link AIGCHost#generatePaintingReport} 与
 *       {@link AIGCHost#generateScaleReport}：报告生成需「排队 + 起工作线程 +
 *       按单元数限并发 + 写回存储」，队列与线程是宿主运行时职责，模块拿不到，
 *       故编排实现在宿主、由门面经本组方法转发，使门面不再直接引用业务场景单例。
 *       配套把 {@code PaintingReportListener} 与 {@code ScaleReportListener}
 *       从 service 下沉到 <code>common</code>（依赖全在 common，可无成本迁移），
 *       否则回调类型无法出现在 SPI 签名上。</li>
 * </ul>
     */
    public final static int VERSION = 8;

    private AIGCSPI() {
    }

    /**
     * 判断模块声明的 SPI 版本是否受当前宿主支持。
     *
     * @param spiVersion 模块声明的 SPI 版本。
     * @return 版本一致时返回 <code>true</code>。
     */
    public static boolean isCompatible(int spiVersion) {
        return VERSION == spiVersion;
    }
}
