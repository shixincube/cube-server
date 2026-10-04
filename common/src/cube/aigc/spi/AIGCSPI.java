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
     * <p>取值递增代表契约有增补：模块声明的版本低于此值时，
     * 说明其依赖了宿主尚未提供的能力，直接拒绝加载而非降级运行。</p>
     *
     * <p><b>宿主服务能力</b>：{@link AIGCHost#getContact}、
     * {@link AIGCHost#getMembership}、{@link AIGCHost#getRemainingUsages}、
     * {@link AIGCHost#allowPredictPainting} 供模块读取联系人档案、会员资格、
     * 剩余用量与报告可见权限。这四项的宿主实现都是单例服务（联系人服务、
     * 会员中心），模块无法自行访问；其中会员系统本身位于宿主，故 SPI 只暴露
     * 「按域名 + 联系人 ID 取资格」这一结果，不把会员系统作为返回类型。</p>
     *
     * <p>另新增 {@link AIGCHost#downloadFile}：按外部链接下载文件并登记为文件标签。
     * 供「模板文章」等动作使用——调用方只拿到第三方给出的图片地址，
     * 下载动作由文件存储服务完成，模块不直接发起网络请求。</p>
     *
     * <p>另新增 {@link AIGCHost#makeKeyFeature}：把评测报告的关键特征列表
     * 渲染为 Markdown 文本。该能力与报告内容加工同族，但服务的报告类型不同
     * （综合评测报告而非绘画报告），故单独提供。</p>
     *
     * <p>另新增 {@link AIGCHost#matchSimilarity}：以模板素材做图像相似度匹配。
     * 绘画解读需要在物体检测之外补充素材判断（如雨中人绘画里的伞），
     * 检索由计算机视觉服务承担，模块不持有其模型。
     * 为此 {@code MatchSimilarityListener} 从 service 下沉到
     * <code>common</code>——其回调参数（文件标签、素材列表、状态码）
     * 本就在common，下沉无额外依赖。</p>
     */
    public final static int VERSION = 24;

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
