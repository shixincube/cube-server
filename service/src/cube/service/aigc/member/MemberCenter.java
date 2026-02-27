/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.member;

import cube.aigc.psychology.ReportPermission;
import cube.aigc.psychology.app.UserProfile;
import cube.auth.AuthToken;
import cube.common.entity.Contact;
import cube.common.entity.Membership;
import cube.common.entity.User;
import cube.service.aigc.AIGCService;
import cube.service.contact.ContactManager;

public class MemberCenter {

    /**
     * 非注册用户配额。
     */
    public final static int gsVisitorQuotas = 100;

    /**
     * 非会员的注册用户配额。
     */
    public final static int gsUserQuotas = 1000;

    /**
     * 专业版会员每月配额。
     */
    public final static int gsOrdinaryMemberQuotasPerMonth = 10000;

    /**
     * 旗舰版会员每月配置。
     */
    public final static int gsPremiumMemberQuotasPerMonth = -1;

    /**
     * 咨询督导消耗量。
     */
    public final static int gsPowerOfSupervisor = 1000;

    /**
     * 绘画推理消耗量。
     */
    public final static int gsPowerOfPredictPainting = 100;

    /**
     * 咨询内容整理为笔记消耗量。
     */
    public final static int gsPowerOfClinicalRecord = 100;

    /**
     * 非会员报告保存天数。
     */
    public final static int gsNonmemberRetention = 180;

    /**
     * 会员报告保存天数。
     */
    public final static int gsMemberRetention = 0;

    private final static MemberCenter instance = new MemberCenter();

    private AIGCService service;

    private MemberCenter() {
    }

    public static MemberCenter getInstance() {
        return MemberCenter.instance;
    }

    public void start(AIGCService service) {
        this.service = service;
    }

    public void stop() {
    }

    /**
     * 获取用户
     * @param domain
     * @param user
     * @return
     */
    public int getUsageOfThisMonth(String domain, User user) {
        Membership membership = ContactManager.getInstance().getMembershipSystem().getMembership(
                domain, user.getId(), Membership.STATE_NORMAL);
        // TODO XJW
        return 0;
    }

    public int getUsageOfThisMonth(Contact contact) {
        // TODO XJW
        return 0;
    }

    /**
     * 用户剩余的可用量。
     *
     * @param user
     * @param membership
     * @return
     */
    public int getRemainingUsages(User user, Membership membership) {
        // 本月用量
        int usage = this.getUsageOfThisMonth(user.getDomain().getName(), user);

        if (!user.isRegistered()) {
            // 未注册
            return gsVisitorQuotas - usage;
        }

        if (null == membership) {
            // 非会员
            return gsUserQuotas - usage;
        }
        else {
            // 会员
            if (membership.isOrdinaryRank()) {
                return gsOrdinaryMemberQuotasPerMonth - usage;
            }
            else {
                return Integer.MAX_VALUE;
            }
        }
    }

    public ReportPermission allowPredictPainting(String domain, User user, long reportSn) {
        // 本月用量
        int usage = this.getUsageOfThisMonth(domain, user);

        if (!user.isRegistered()) {
            // 非注册用户
            // 计算用量
            if (usage >= gsVisitorQuotas) {
                // 最小权限
                return new ReportPermission(user.getId(), reportSn);
            }
            else {
                // 权限
                ReportPermission permission = ReportPermission.createAllPermissions(user.getId(), reportSn);
                permission.attention = false;
                permission.dimensionScore = false;
                return permission;
            }
        }

        Membership membership = ContactManager.getInstance().getMembershipSystem().getMembership(
                domain, user.getId(), Membership.STATE_NORMAL);
        if (null == membership) {
            // 注册用户，非会员
            if (usage >= gsUserQuotas) {
                // 最小权限
                return new ReportPermission(user.getId(), reportSn);
            }
            else {
                // 权限
                ReportPermission permission = ReportPermission.createAllPermissions(user.getId(), reportSn);
                permission.attention = false;
                permission.dimensionScore = false;
                return permission;
            }
        }
        else {
            // 会员
            if (membership.isOrdinaryRank()) {
                // 专业版会员
                if (usage >= gsOrdinaryMemberQuotasPerMonth) {
                    // 最小权限
                    return new ReportPermission(user.getId(), reportSn);
                }
            }

            // 权限
            ReportPermission permission = ReportPermission.createAllPermissions(user.getId(), reportSn);
            permission.attention = false;
            permission.dimensionScore = false;
            return permission;
        }

        // 起始日期
//        long startTime = membership.getTimestamp();
//
//        // 截止日期
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTimeInMillis(startTime);
//        calendar.add(Calendar.MONTH, 1);
//        long endTime = calendar.getTimeInMillis();
//
//        int num = PsychologyScene.getInstance().numScaleReports(user.getId(), AIGCStateCode.Ok.code,
//                true, startTime, endTime);
//        if (membership.type.equalsIgnoreCase(Membership.TYPE_ORDINARY)) {
//            // 普通会员
//            if (num >= gsOrdinaryMemberTimesPerMonth) {
//                // 最小权限
//                return new ReportPermission(user.getId(), reportSn);
//            }
//            else {
//                // 全部权限
//                return ReportPermission.createAllPermissions(user.getId(), reportSn);
//            }
//        }
//        else {
//            // 高级会员
//            if (gsPremiumMemberTimesPerMonth > 0 && num >= gsPremiumMemberTimesPerMonth) {
//                // 最小权限
//                return new ReportPermission(user.getId(), reportSn);
//            }
//            else {
//                // 全部权限
//                return ReportPermission.createAllPermissions(user.getId(), reportSn);
//            }
//        }
    }

//    public static int getUsageOfThisMonth(long id, Membership membership) {
//        // 起始日期
//        long startTime = membership.getTimestamp();
//
//        // 截止日期
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTimeInMillis(startTime);
//        calendar.add(Calendar.MONTH, 1);
//        long endTime = calendar.getTimeInMillis();
//
//        return PsychologyScene.getInstance().numPsychologyReports(id,
//                AIGCStateCode.Ok.code, true, startTime, endTime);
//    }

    public UserProfile getAppUserProfile(AuthToken authToken) {
        UserProfile profile = new UserProfile();

        Contact contact = ContactManager.getInstance().getContact(authToken.getDomain(), authToken.getContactId());
        profile.totalPoints = ContactManager.getInstance().getPointSystem().total(contact);
        profile.pointList = ContactManager.getInstance().getPointSystem().listPoints(contact);

        // 本月用量
        profile.usageOfThisMonth = this.getUsageOfThisMonth(contact);

        Membership membership = ContactManager.getInstance().getMembershipSystem().getMembership(contact, Membership.STATE_NORMAL);
        if (null != membership) {
            // 是会员
            profile.membership = membership;
            if (membership.isOrdinaryRank()) {
                // 每月限制
                profile.limitPerMonth = gsOrdinaryMemberQuotasPerMonth;
            }
            else {
                // 每月限制
                profile.limitPerMonth = gsPremiumMemberQuotasPerMonth;
            }
        }
        else {
            User user = this.service.getUser(authToken.getCode());
            if (user.isRegistered()) {
                // 注册用户
                // 每月限制
                profile.limitPerMonth = gsUserQuotas;
            }
            else {
                // 访客
                // 每月限制
                profile.limitPerMonth = gsVisitorQuotas;
            }
        }

        // 已作废数据
//        profile.permissibleReports = this.numPsychologyReports(authToken.getContactId(), AIGCStateCode.Ok.code, true);
//        List<PaintingReport> reports = this.getPsychologyReports(authToken.getContactId(), AIGCStateCode.Ok.code, 1);
//        if (!reports.isEmpty()) {
//            try {
//                profile.hexagonScore = reports.get(0).getDimensionScore();
//                profile.personality = reports.get(0).getEvaluationReport().getPersonalityAccelerator().getBigFivePersonality();
//            } catch (Exception e) {
//                Logger.w(this.getClass(), "#getAppUserProfile", e);
//            }
//        }

        return profile;
    }
}
