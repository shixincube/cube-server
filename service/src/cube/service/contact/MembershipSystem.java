/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.entity.Contact;
import cube.common.entity.Membership;
import cube.util.ConfigUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MembershipSystem {

    public final static String Monthly = "Monthly";
    public final static String Annual = "Annual";

    private ContactStorage storage;

    private InvitationCodeStorage invitationCodeStorage;

    public MembershipSystem(ContactStorage storage) {
        this.storage = storage;
        this.invitationCodeStorage = new InvitationCodeStorage();
        this.invitationCodeStorage.supply();
    }

    public Membership getMembership(Contact contact, int state) {
        return this.storage.readMembership(contact.getDomain().getName(), contact.getId(), state);
    }

    public Membership getMembership(String domain, long contactId, int state) {
        return this.storage.readMembership(domain, contactId, state);
    }

    public boolean activateMembership(String domain, long contactId, String name, long duration, String description,
                                      JSONObject context) {
        Membership membership = new Membership(contactId, domain, name,
                Membership.TYPE_ORDINARY, Membership.STATE_NORMAL, System.currentTimeMillis(),
                duration, description, context);
        return this.storage.writeMembership(membership);
    }

    public boolean cancelMembership(String domain, long contactId) {
        Membership membership = this.storage.readMembership(domain, contactId, Membership.STATE_NORMAL);
        if (null == membership) {
            return false;
        }

        membership.state = Membership.STATE_INVALID;
        return this.storage.writeMembership(membership);
    }

    public boolean updateMembership(Membership membership) {
        return this.storage.writeMembership(membership);
    }

    public InvitationCode verifyInvitationCode(long contactId, String code) {
        return null;
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


            return null;
        }

        public void update(InvitationCode value) {
            InvitationCode hit = null;
            for (InvitationCode invitationCode : this.ordinaryInvitationCodes) {
                if (invitationCode.code.equals(value.code)) {
                    hit = invitationCode;
                    break;
                }
            }

            if (null == hit) {

            }

            if (null != hit) {
                hit.uid = value.uid;
                hit.date = value.date;
            }

            save();
        }

        private void supply() {
            // TYPE_ORDINARY
            int remaining = this.remains(Membership.TYPE_ORDINARY, Monthly);
            if (remaining < 10) {
                this.append(Membership.TYPE_ORDINARY, Monthly, 10 - remaining);
            }
            remaining = this.remains(Membership.TYPE_ORDINARY, Annual);
            if (remaining < 10) {
                this.append(Membership.TYPE_ORDINARY, Annual, 10 - remaining);
            }
            // TYPE_SENIOR
            remaining = this.remains(Membership.TYPE_SENIOR, Monthly);
            if (remaining < 10) {
                this.append(Membership.TYPE_SENIOR, Monthly, 10 - remaining);
            }
            remaining = this.remains(Membership.TYPE_SENIOR, Annual);
            if (remaining < 10) {
                this.append(Membership.TYPE_SENIOR, Annual, 10 - remaining);
            }
            // TYPE_PREMIUM
            remaining = this.remains(Membership.TYPE_PREMIUM, Monthly);
            if (remaining < 10) {
                this.append(Membership.TYPE_PREMIUM, Monthly, 10 - remaining);
            }
            remaining = this.remains(Membership.TYPE_PREMIUM, Annual);
            if (remaining < 10) {
                this.append(Membership.TYPE_PREMIUM, Annual, 10 - remaining);
            }
            // TYPE_SUPREME
            remaining = this.remains(Membership.TYPE_SUPREME, Monthly);
            if (remaining < 10) {
                this.append(Membership.TYPE_SUPREME, Monthly, 10 - remaining);
            }
            remaining = this.remains(Membership.TYPE_SUPREME, Annual);
            if (remaining < 10) {
                this.append(Membership.TYPE_SUPREME, Annual, 10 - remaining);
            }
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
                    list.add(new InvitationCode(validity));
                }
            }
            save();
            load();
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
                        InvitationCode invitationCode = new InvitationCode(ordinaryArray.getJSONObject(i));
                        this.ordinaryInvitationCodes.add(invitationCode);
                    }

                    JSONArray seniorArray = data.getJSONArray(Membership.TYPE_SENIOR);
                    for (int i = 0; i < seniorArray.length(); ++i) {
                        InvitationCode invitationCode = new InvitationCode(seniorArray.getJSONObject(i));
                        this.seniorInvitationCodes.add(invitationCode);
                    }

                    JSONArray premiumArray = data.getJSONArray(Membership.TYPE_PREMIUM);
                    for (int i = 0; i < premiumArray.length(); ++i) {
                        InvitationCode invitationCode = new InvitationCode(premiumArray.getJSONObject(i));
                        this.premiumInvitationCodes.add(invitationCode);
                    }

                    JSONArray supremeArray = data.getJSONArray(Membership.TYPE_SUPREME);
                    for (int i = 0; i < supremeArray.length(); ++i) {
                        InvitationCode invitationCode = new InvitationCode(supremeArray.getJSONObject(i));
                        this.supremeInvitationCodes.add(invitationCode);
                    }
                } catch (Exception e) {
                    Logger.e(this.getClass(), "#load", e);
                    // 加载失败，创建新文件
                    supply();
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

        public String code;
        public String validity;
        public long uid;
        public String date;

        public InvitationCode(String validity) {
            this.validity = validity;
            this.code = Utils.randomNumberString(6);
            this.uid = 0;
            this.date = "";
        }

        public InvitationCode(JSONObject json) {
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
