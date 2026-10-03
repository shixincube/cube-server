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
     * </ul>
     */
    public final static int VERSION = 4;

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
