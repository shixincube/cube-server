/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology;

/**
 * 心理学报告留存策略。
 *
 * <p>本类的常量原先声明在宿主侧 {@code MemberCenter} 中（{@code gsNonmemberRetention} /
 * {@code gsMemberRetention}），但它们的<b>唯一用途</b>是心理学报告的写入与过期过滤：
 * 写入侧在绘画推理任务中设置 {@code retention} 字段，过滤侧在
 * {@code PsychologyStorage} 中按该字段做时间窗过滤。会员中心只是历史归属地，
 * 并非该策略的固有属性。</p>
 *
 * <p>把常量迁到common 的目的是让<b>写入侧与过滤侧引用同一个编译期常量</b>。
 * 若两侧各自持有一份字面量而只改动其中之一，过滤窗口与写入值将不再匹配，
 * 后果是<b>过期报告永不回收</b>，且该问题只在数据积累到留存天数之后才暴露，
 * 排查成本极高。</p>
 */
public final class RetentionPolicy {

    /**
     * 非会员报告保存天数。0 表示永久保存。
     */
    public final static int NON_MEMBER_RETENTION_DAYS = 180;

    /**
     * 会员报告保存天数。0 表示永久保存。
     */
    public final static int MEMBER_RETENTION_DAYS = 0;

    private RetentionPolicy() {
    }
}
