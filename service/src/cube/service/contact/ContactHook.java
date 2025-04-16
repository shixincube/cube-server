/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cube.plugin.Hook;

/**
 * 联系人模块的 Hook 基类。
 */
public class ContactHook extends Hook {

    /**
     * 验证联系人身份。
     */
    public final static String VerifyIdentity = "VerifyIdentity";

    public final static String SignIn = "SignIn";

    public final static String SignOut = "SignOut";

    public final static String DeviceTimeout = "DeviceTimeout";

    public final static String Comeback = "Comeback";

    public final static String NewContact = "NewContact";

    public final static String ModifyContactName = "ModifyContactName";

    public final static String ModifyContactContext = "ModifyContactContext";

    public final static String VerifyVerificationCode = "VerifyVerificationCode";

    public ContactHook(String key) {
        super(key);
    }
}
