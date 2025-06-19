/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.ReportPermission;
import cube.common.entity.Contact;
import cube.common.entity.Membership;
import cube.common.entity.User;
import cube.common.state.AIGCStateCode;
import cube.service.contact.ContactManager;

import java.util.Calendar;

public final class UserProfiles {

    public final static int gsNoUserTimers = 1;

    public final static int gsNonmemberTimesPerMonth = 1;

    public final static int gsOrdinaryMemberTimesPerMonth = 3;

    public final static int gsPremiumMemberTimesPerMonth = -1;

    public static ReportPermission allowPredictPainting(User user, Contact contact, long reportSn) {
        if (!user.isRegistered()) {
            // 非注册用户
            int num = PsychologyScene.getInstance().numPsychologyReports(user.getId());
            // 只允许使用一次
            if (num >= gsNoUserTimers) {
                // 最小权限
                return new ReportPermission(user.getId(), reportSn);
            }
            else {
                // 全部权限
                return ReportPermission.createAllPermissions(user.getId(), reportSn);
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

        Membership membership = ContactManager.getInstance().getMembershipSystem().getMembership(
                contact.getDomain().getName(), user.getId());

        if (null == membership) {
            // 非会员
            if (num >= gsNonmemberTimesPerMonth) {
                // 最小权限
                return new ReportPermission(user.getId(), reportSn);
            }
            else {
                // 全部权限
                return ReportPermission.createAllPermissions(user.getId(), reportSn);
            }
        }

        if (membership.state != Membership.STATE_NORMAL) {
            // 会员状态异常
            // 最小权限
            return new ReportPermission(user.getId(), reportSn);
        }

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

    public static int getUsageOfThisMonth(Contact contact) {
        long startTime = 0;
        long endTime = 0;
        return PsychologyScene.getInstance().numPsychologyReports(contact.getId(),
                AIGCStateCode.Ok.code, true, startTime, endTime);
    }
}
