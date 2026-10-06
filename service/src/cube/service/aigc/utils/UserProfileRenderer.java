/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.utils;

import cube.aigc.Consts;
import cube.aigc.psychology.Resource;
import cube.common.entity.Membership;
import cube.common.entity.User;

import java.util.Calendar;
import java.util.List;

/**
 * 用户档案文案渲染。
 *
 * <p><b>为何在宿主而不在插件的 {@code ReportRenderer}</b>：本方法渲染的是
 * 「个人知识库 Profile」条目——宿主在用户注册、开通会员等时刻把这段文字
 * 写进该用户的私有知识库，属平台侧的档案维护，与心理学的报告渲染无关。
 * 其输入输出全部是 common 类型（{@link User} / {@link Membership}），
 * 保留在插件只会让宿主为一句文案反向依赖业务模块。</p>
 */
public class UserProfileRenderer {

    private UserProfileRenderer() {
    }

    /**
     * 渲染会员状态描述。
     *
     * @param user 用户档案。
     * @param membership 会员资格；{@code null} 表示非会员。
     * @return 返回 Markdown 文本。
     */
    public static String makeMembership(User user, Membership membership) {
        StringBuffer buf = new StringBuffer();
        if (null == membership) {
            buf.append("用户“").append(user.getName()).append("”不是白泽灵思会员，");
            List<String> benefitsList = null;
            if (user.isRegistered()) {
                buf.append("其是注册用户，享受免费版权益。\n\n");
                benefitsList = Resource.getInstance().getMemberBenefits(Consts.USER_TYPE_FREE);
            }
            else {
                buf.append("其是访客，享受访客权益。\n\n");
                benefitsList = Resource.getInstance().getMemberBenefits(Consts.USER_TYPE_VISITOR);
            }
            buf.append("其可享受的产品权益有：\n");
            for (String line : benefitsList) {
                buf.append("* ").append(line).append("\n");
            }
            buf.append("\n");
        }
        else {
            buf.append("用户“").append(user.getName()).append("”是白泽灵思");
            if (membership.type.equals(Membership.TYPE_ORDINARY)) {
                buf.append("专业版会员。\n\n");
                buf.append("其可享受的专业版会员权益有：\n");
            }
            else {
                buf.append("旗舰版会员。\n\n");
                buf.append("其可享受的旗舰版会员权益有：\n");
            }
            List<String> benefitsList = Resource.getInstance().getMemberBenefits(membership.type);
            for (String line : benefitsList) {
                buf.append("* ").append(line).append("\n");
            }
            buf.append("\n");

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(membership.getTimestamp());
            buf.append("会员有效期从");
            buf.append(calendar.get(Calendar.YEAR)).append("年");
            buf.append(calendar.get(Calendar.MONTH) + 1).append("月");
            buf.append(calendar.get(Calendar.DATE)).append("日");
            buf.append("至");
            calendar.setTimeInMillis(membership.getTimestamp() + membership.duration);
            buf.append(calendar.get(Calendar.YEAR)).append("年");
            buf.append(calendar.get(Calendar.MONTH) + 1).append("月");
            buf.append(calendar.get(Calendar.DATE)).append("日");
            buf.append("\n\n");
        }
        return buf.toString();
    }
}
