/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.auth.AuthConsts;
import cube.common.entity.Contact;
import cube.common.entity.Membership;
import cube.util.ConfigUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MembershipSystem {

    /**
     * 月卡。
     */
    public final static String VALIDITY_MONTHLY = "Monthly";

    /**
     * 年卡。
     */
    public final static String VALIDITY_ANNUAL = "Annual";

    private ContactStorage storage;

    private InvitationCodeStorage invitationCodeStorage;

    public MembershipSystem(ContactStorage storage) {
        this.storage = storage;
        this.invitationCodeStorage = new InvitationCodeStorage();
        if (this.invitationCodeStorage.supply()) {
            this.invitationCodeStorage.save();
        }
    }

    public Membership getMembership(Contact contact, int state) {
        return this.storage.readMembership(contact.getDomain().getName(), contact.getId(), state);
    }

    public Membership getMembership(String domain, long contactId, int state) {
        return this.storage.readMembership(domain, contactId, state);
    }

    public Membership activateMembership(String domain, long contactId, String name, InvitationCode invitationCode,
                                      JSONObject context) {
        long now = System.currentTimeMillis();

        // 查找当前联系人有效的会员数据
        long remaining = 0;
        Membership activeMembership = this.storage.readMembership(domain, contactId, Membership.STATE_NORMAL);
        if (null != activeMembership) {
            // 将会员有效期累加
            // 月卡可升级到年卡，年卡可升级到年卡，但是年卡不能升级到月卡，即低级别年卡不能升级为高级别月卡
            if (activeMembership.description.equals(VALIDITY_MONTHLY)) {
                // 目前是月卡，均可升级
                remaining = activeMembership.getTimestamp() + activeMembership.duration - now;
            }
            else if (activeMembership.description.equals(VALIDITY_ANNUAL)) {
                // 目前是年卡，只能升级到年卡
                if (invitationCode.validity.equals(VALIDITY_ANNUAL)) {
                    remaining = activeMembership.getTimestamp() + activeMembership.duration - now;
                }
                else {
                    Logger.e(this.getClass(), "#activateMembership - Validity rule conflicts: " + contactId);
                    return null;
                }
            }
            else {
                Logger.e(this.getClass(), "#activateMembership - Rule conflicts: " + contactId);
                return null;
            }

            // 会员可以同级升级，向上升级，但不能向下降级
            if (activeMembership.type.equalsIgnoreCase(Membership.TYPE_SENIOR)) {
                if (invitationCode.type.equalsIgnoreCase(Membership.TYPE_ORDINARY)) {
                    Logger.e(this.getClass(), "#activateMembership - No downgrade: " + contactId +
                            " " + activeMembership.type + " -> " + invitationCode.type);
                    return null;
                }
            }
            else if (activeMembership.type.equalsIgnoreCase(Membership.TYPE_PREMIUM)) {
                if (invitationCode.type.equalsIgnoreCase(Membership.TYPE_ORDINARY) ||
                        invitationCode.type.equalsIgnoreCase(Membership.TYPE_SENIOR)) {
                    Logger.e(this.getClass(), "#activateMembership - No downgrade: " + contactId +
                            " " + activeMembership.type + " -> " + invitationCode.type);
                    return null;
                }
            }
            else if (activeMembership.type.equalsIgnoreCase(Membership.TYPE_SUPREME)) {
                if (invitationCode.type.equalsIgnoreCase(Membership.TYPE_ORDINARY) ||
                        invitationCode.type.equalsIgnoreCase(Membership.TYPE_SENIOR) ||
                        invitationCode.type.equalsIgnoreCase(Membership.TYPE_PREMIUM)) {
                    Logger.e(this.getClass(), "#activateMembership - No downgrade: " + contactId +
                            " " + activeMembership.type + " -> " + invitationCode.type);
                    return null;
                }
            }
        }

        // 计算结束时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        if (invitationCode.validity.equals(VALIDITY_MONTHLY)) {
            calendar.add(Calendar.MONTH, 1);
        }
        else {
            calendar.add(Calendar.YEAR, 1);
        }

        // 累加剩余时长
        if (remaining > 0) {
            Logger.i(this.getClass(), "#activateMembership - Append membership duration: " + contactId
                    + " - " + remaining);
            long timeInMillis = calendar.getTimeInMillis();
            calendar.setTimeInMillis(timeInMillis + remaining);
        }

        Membership membership = new Membership(contactId, domain, name,
                invitationCode.type, Membership.STATE_NORMAL, now,
                calendar.getTimeInMillis() - now, invitationCode.validity, context);
        if (this.storage.writeMembership(membership)) {
            return membership;
        }
        else {
            Logger.w(this.getClass(), "#activateMembership - Update DB error: " + contactId);
            return null;
        }
    }

    public Membership cancelMembership(String domain, long contactId) {
        Membership membership = this.storage.readMembership(domain, contactId, Membership.STATE_NORMAL);
        if (null == membership) {
            Logger.w(this.getClass(), "#cancelMembership - No membership data in DB: " + contactId);
            return null;
        }

        membership.state = Membership.STATE_INVALID;
        this.storage.writeMembership(membership);
        return membership;
    }

    public boolean updateMembership(Membership membership) {
        return this.storage.writeMembership(membership);
    }

    public InvitationCode verifyInvitationCode(String code) {
        InvitationCode result = this.invitationCodeStorage.search(code);
        if (null == result) {
            Logger.w(this.getClass(), "#verifyInvitationCode - Can NOT find the invitation code: " + code);
            return null;
        }

        if (result.uid != 0) {
            Logger.w(this.getClass(), "#verifyInvitationCode - The invitation code has been used: " + code +
                    " - " + result.uid);
            return null;
        }
        return result;
    }

    public InvitationCode bindInvitationCode(final InvitationCode invitationCode, long contactId) {
        invitationCode.uid = contactId;
        invitationCode.date = Utils.gsDateFormat.format(new Date());
        (new Thread() {
            @Override
            public void run() {
                boolean success = invitationCodeStorage.update(invitationCode);
                if (success) {
                    Logger.i(getClass(), "#verifyInvitationCode - Updates code: " + invitationCode.code + " - " + contactId);
                }
                else {
                    Logger.i(getClass(), "#verifyInvitationCode - Updates code failed: " + invitationCode.code + " - " + contactId);
                }
            }
        }).start();
        return invitationCode;
    }

    public void onTick(long now) {
        // 检测所有过期的会员，设置为无效状态
        List<Membership> memberships = this.storage.readExpiredMemberships(AuthConsts.DEFAULT_DOMAIN);
        List<Long> idList = new ArrayList<>();
        for (Membership membership : memberships) {
            Logger.i(this.getClass(), "#onTick - Update expired member: " + membership.getId());
            idList.add(membership.getId());
        }
        if (this.storage.updateMembershipsState(AuthConsts.DEFAULT_DOMAIN, idList, Membership.STATE_INVALID)) {
            Logger.i(this.getClass(), "#onTick - Update memberships state");
        }
        else {
            Logger.w(this.getClass(), "#onTick - Update memberships state failed");
        }
    }


    public class InvitationCodeStorage {

        private List<InvitationCode> ordinaryInvitationCodes = new ArrayList<>();
        private List<InvitationCode> seniorInvitationCodes = new ArrayList<>();
        private List<InvitationCode> premiumInvitationCodes = new ArrayList<>();
        private List<InvitationCode> supremeInvitationCodes = new ArrayList<>();

        public InvitationCodeStorage() {
            this.load();
        }

        public InvitationCode search(String code) {
            for (InvitationCode invitationCode : this.ordinaryInvitationCodes) {
                if (invitationCode.code.equals(code)) {
                    return invitationCode;
                }
            }
            for (InvitationCode invitationCode : this.seniorInvitationCodes) {
                if (invitationCode.code.equals(code)) {
                    return invitationCode;
                }
            }
            for (InvitationCode invitationCode : this.premiumInvitationCodes) {
                if (invitationCode.code.equals(code)) {
                    return invitationCode;
                }
            }
            for (InvitationCode invitationCode : this.supremeInvitationCodes) {
                if (invitationCode.code.equals(code)) {
                    return invitationCode;
                }
            }
            return null;
        }

        public boolean update(InvitationCode value) {
            InvitationCode hit = search(value.code);
            if (null != hit) {
                hit.uid = value.uid;
                hit.date = value.date;
                supply();
                save();
                return true;
            }
            else {
                return false;
            }
        }

        public boolean supply() {
            boolean exec = false;
            // TYPE_ORDINARY
            int remaining = this.remains(Membership.TYPE_ORDINARY, VALIDITY_MONTHLY);
            if (remaining < 10) {
                exec = true;
                this.append(Membership.TYPE_ORDINARY, VALIDITY_MONTHLY, 10 - remaining);
            }
            remaining = this.remains(Membership.TYPE_ORDINARY, VALIDITY_ANNUAL);
            if (remaining < 10) {
                exec = true;
                this.append(Membership.TYPE_ORDINARY, VALIDITY_ANNUAL, 10 - remaining);
            }
            // TYPE_SENIOR
            remaining = this.remains(Membership.TYPE_SENIOR, VALIDITY_MONTHLY);
            if (remaining < 10) {
                exec = true;
                this.append(Membership.TYPE_SENIOR, VALIDITY_MONTHLY, 10 - remaining);
            }
            remaining = this.remains(Membership.TYPE_SENIOR, VALIDITY_ANNUAL);
            if (remaining < 10) {
                exec = true;
                this.append(Membership.TYPE_SENIOR, VALIDITY_ANNUAL, 10 - remaining);
            }
            // TYPE_PREMIUM
            remaining = this.remains(Membership.TYPE_PREMIUM, VALIDITY_MONTHLY);
            if (remaining < 10) {
                exec = true;
                this.append(Membership.TYPE_PREMIUM, VALIDITY_MONTHLY, 10 - remaining);
            }
            remaining = this.remains(Membership.TYPE_PREMIUM, VALIDITY_ANNUAL);
            if (remaining < 10) {
                exec = true;
                this.append(Membership.TYPE_PREMIUM, VALIDITY_ANNUAL, 10 - remaining);
            }
            // TYPE_SUPREME
            remaining = this.remains(Membership.TYPE_SUPREME, VALIDITY_MONTHLY);
            if (remaining < 10) {
                exec = true;
                this.append(Membership.TYPE_SUPREME, VALIDITY_MONTHLY, 10 - remaining);
            }
            remaining = this.remains(Membership.TYPE_SUPREME, VALIDITY_ANNUAL);
            if (remaining < 10) {
                exec = true;
                this.append(Membership.TYPE_SUPREME, VALIDITY_ANNUAL, 10 - remaining);
            }
            return exec;
        }

        public int remains(String type, String validity) {
            List<InvitationCode> list = this.matchingList(type);
            int count = 0;
            for (InvitationCode code : list) {
                if (code.uid == 0 && code.validity.equalsIgnoreCase(validity)) {
                    ++count;
                }
            }
            return count;
        }

        private void append(String type, String validity, int amount) {
            synchronized (this) {
                List<InvitationCode> list = this.matchingList(type);
                for (int i = 0; i < amount; ++i) {
                    list.add(new InvitationCode(type, validity));
                }
            }
        }

        private void load() {
            synchronized (this) {
                this.ordinaryInvitationCodes.clear();
                this.seniorInvitationCodes.clear();
                this.premiumInvitationCodes.clear();
                this.supremeInvitationCodes.clear();
                try {
                    JSONObject data = ConfigUtils.readJsonFile("assets/MembershipInvitationCode.json");

                    JSONArray ordinaryArray = data.getJSONArray(Membership.TYPE_ORDINARY);
                    for (int i = 0; i < ordinaryArray.length(); ++i) {
                        InvitationCode invitationCode = new InvitationCode(Membership.TYPE_ORDINARY,
                                ordinaryArray.getJSONObject(i));
                        this.ordinaryInvitationCodes.add(invitationCode);
                    }

                    JSONArray seniorArray = data.getJSONArray(Membership.TYPE_SENIOR);
                    for (int i = 0; i < seniorArray.length(); ++i) {
                        InvitationCode invitationCode = new InvitationCode(Membership.TYPE_SENIOR,
                                seniorArray.getJSONObject(i));
                        this.seniorInvitationCodes.add(invitationCode);
                    }

                    JSONArray premiumArray = data.getJSONArray(Membership.TYPE_PREMIUM);
                    for (int i = 0; i < premiumArray.length(); ++i) {
                        InvitationCode invitationCode = new InvitationCode(Membership.TYPE_PREMIUM,
                                premiumArray.getJSONObject(i));
                        this.premiumInvitationCodes.add(invitationCode);
                    }

                    JSONArray supremeArray = data.getJSONArray(Membership.TYPE_SUPREME);
                    for (int i = 0; i < supremeArray.length(); ++i) {
                        InvitationCode invitationCode = new InvitationCode(Membership.TYPE_SUPREME,
                                supremeArray.getJSONObject(i));
                        this.supremeInvitationCodes.add(invitationCode);
                    }
                } catch (Exception e) {
                    Logger.e(this.getClass(), "#load", e);
                    // 加载失败，创建新文件
                    if (supply()) {
                        save();
                    }
                }
            }
        }

        private void save() {
            synchronized (this) {
                JSONObject data = new JSONObject();
                JSONArray ordinaryArray = new JSONArray();
                for (InvitationCode code : this.ordinaryInvitationCodes) {
                    ordinaryArray.put(code.toJSON());
                }
                data.put(Membership.TYPE_ORDINARY, ordinaryArray);

                JSONArray seniorArray = new JSONArray();
                for (InvitationCode code : this.seniorInvitationCodes) {
                    seniorArray.put(code.toJSON());
                }
                data.put(Membership.TYPE_SENIOR, seniorArray);

                JSONArray premiumArray = new JSONArray();
                for (InvitationCode code : this.premiumInvitationCodes) {
                    premiumArray.put(code.toJSON());
                }
                data.put(Membership.TYPE_PREMIUM, premiumArray);

                JSONArray supremeArray = new JSONArray();
                for (InvitationCode code : this.supremeInvitationCodes) {
                    supremeArray.put(code.toJSON());
                }
                data.put(Membership.TYPE_SUPREME, supremeArray);

                ConfigUtils.writeJsonFile("assets/MembershipInvitationCode.json", data);
            }
        }

        private List<InvitationCode> matchingList(String type) {
            List<InvitationCode> list = null;
            if (Membership.TYPE_ORDINARY.equalsIgnoreCase(type)) {
                list = this.ordinaryInvitationCodes;
            }
            else if (Membership.TYPE_SENIOR.equalsIgnoreCase(type)) {
                list = this.seniorInvitationCodes;
            }
            else if (Membership.TYPE_PREMIUM.equalsIgnoreCase(type)) {
                list = this.premiumInvitationCodes;
            }
            else if (Membership.TYPE_SUPREME.equalsIgnoreCase(type)) {
                list = this.supremeInvitationCodes;
            }
            return list;
        }
    }

    public class InvitationCode {

        public final String type;
        public String code;
        public String validity;
        public long uid;
        public String date;

        public InvitationCode(String type, String validity) {
            this.type = type;
            this.validity = validity;
            this.code = Utils.randomNumberString(6);
            this.uid = 0;
            this.date = "";
        }

        public InvitationCode(String type, JSONObject json) {
            this.type = type;
            this.code = json.getString("code");
            this.validity = json.getString("validity");
            this.uid = json.getLong("uid");
            this.date = json.getString("date");
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("code", this.code);
            json.put("validity", this.validity);
            json.put("uid", this.uid);
            json.put("date", this.date);
            return json;
        }
    }
}
