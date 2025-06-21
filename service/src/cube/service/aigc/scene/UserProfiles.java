/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.ReportPermission;
import cube.common.entity.Membership;
import cube.common.entity.User;
import cube.common.state.AIGCStateCode;
import cube.service.contact.ContactManager;

import java.util.Calendar;

public final class UserProfiles {

    public final static int gsNoUserTimes = 1;

    public final static int gsNoMemberTimes = 2;

    public final static int gsOrdinaryMemberTimesPerMonth = 3;

    public final static int gsPremiumMemberTimesPerMonth = -1;

    /**
     * 非会员报告保存天数。
     */
    public final static int gsNonmemberRetention = 7;

    /**
     * 会员报告保存天数。
     */
    public final static int gsMemberRetention = 0;

    public static ReportPermission allowPredictPainting(String domain, User user, long reportSn) {
        if (!user.isRegistered()) {
            // 非注册用户
            int num = PsychologyScene.getInstance().numPsychologyReports(user.getId(), AIGCStateCode.Ok.code);
            // 只允许使用 gsNoUserTimes 次
            if (num >= gsNoUserTimes) {
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
            int num = PsychologyScene.getInstance().numPsychologyReports(user.getId(), AIGCStateCode.Ok.code);
            if (num >= gsNoMemberTimes) {
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

        Calendar calendar = Calendar.getInstance();
        // 本月第一天
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long start = calendar.getTimeInMillis();

        // 本月最后一天
        calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        long end = calendar.getTimeInMillis();

        int num = PsychologyScene.getInstance().numScaleReports(user.getId(), AIGCStateCode.Ok.code, true, start, end);
        if (membership.type.equalsIgnoreCase(Membership.TYPE_ORDINARY)) {
            // 普通会员
            if (num >= gsOrdinaryMemberTimesPerMonth) {
                // 最小权限
                return new ReportPermission(user.getId(), reportSn);
            }
            else {
                // 全部权限
                return ReportPermission.createAllPermissions(user.getId(), reportSn);
            }
        }
        else {
            // 高级会员
            if (gsPremiumMemberTimesPerMonth > 0 && num >= gsPremiumMemberTimesPerMonth) {
                // 最小权限
                return new ReportPermission(user.getId(), reportSn);
            }
            else {
                // 全部权限
                return ReportPermission.createAllPermissions(user.getId(), reportSn);
            }
        }
    }

    public static int getUsageOfThisMonth(long id) {
        Calendar calendar = Calendar.getInstance();
        // 本月第一天
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startTime = calendar.getTimeInMillis();

        // 本月最后一天
        calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        long endTime = calendar.getTimeInMillis();

        return PsychologyScene.getInstance().numPsychologyReports(id,
                AIGCStateCode.Ok.code, true, startTime, endTime);
    }

    public static int remainingUsages(User user, Membership membership) {
        if (!user.isRegistered()) {
            // 未注册
            int num = PsychologyScene.getInstance().numPsychologyReports(user.getId(),
                    AIGCStateCode.Ok.code, true);
            return gsNoUserTimes - num;
        }

        if (null == membership) {
            // 非会员
            int num = PsychologyScene.getInstance().numPsychologyReports(user.getId(),
                    AIGCStateCode.Ok.code, true);
            return gsNoMemberTimes - num;
        }
        else {
            // 会员
            if (membership.type.equals(Membership.TYPE_ORDINARY)) {
                return gsOrdinaryMemberTimesPerMonth - getUsageOfThisMonth(user.getId());
            }
            else {
                return Integer.MAX_VALUE;
            }
        }
    }
}
