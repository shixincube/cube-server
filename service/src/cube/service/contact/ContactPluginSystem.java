/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cube.plugin.PluginSystem;

/**
 * 联系模块插件系统。
 */
public class ContactPluginSystem extends PluginSystem<ContactHook> {

    public ContactPluginSystem() {
        this.build();
    }

    public ContactHook getSignInHook() {
        return this.getHook(ContactHook.SignIn);
    }

    public ContactHook getSignOutHook() {
        return this.getHook(ContactHook.SignOut);
    }

    public ContactHook getDeviceTimeoutHook() {
        return this.getHook(ContactHook.DeviceTimeout);
    }

    public ContactHook getComebackHook() {
        return this.getHook(ContactHook.Comeback);
    }

    public ContactHook getVerifyIdentity() {
        return this.getHook(ContactHook.VerifyIdentity);
    }

    public ContactHook getNewContact() {
        return this.getHook(ContactHook.NewContact);
    }

    public ContactHook getModifyContactNameHook() {
        return this.getHook(ContactHook.ModifyContactName);
    }

    public ContactHook getModifyContactContextHook() {
        return this.getHook(ContactHook.ModifyContactContext);
    }

    private void build() {
        ContactHook hook = new ContactHook(ContactHook.SignIn);
        this.addHook(hook);

        hook = new ContactHook(ContactHook.DeviceTimeout);
        this.addHook(hook);

        hook = new ContactHook(ContactHook.Comeback);
        this.addHook(hook);

        hook = new ContactHook(ContactHook.SignOut);
        this.addHook(hook);

        hook = new ContactHook(ContactHook.VerifyIdentity);
        this.addHook(hook);

        hook = new ContactHook(ContactHook.NewContact);
        this.addHook(hook);

        hook = new ContactHook(ContactHook.ModifyContactName);
        this.addHook(hook);

        hook = new ContactHook(ContactHook.ModifyContactContext);
        this.addHook(hook);
    }
}
