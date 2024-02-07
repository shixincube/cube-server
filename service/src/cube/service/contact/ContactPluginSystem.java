/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
