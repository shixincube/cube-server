/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.service.riskmgmt.plugin;

import cube.common.entity.ContactBehavior;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactPluginContext;
import cube.service.riskmgmt.RiskManagement;

/**
 * 创建域应用插件。
 */
public class ContactPlugin implements Plugin {

    private RiskManagement service;

    public ContactPlugin(RiskManagement service) {
        this.service = service;
    }

    @Override
    public void setup() {
    }

    @Override
    public void teardown() {
    }

    @Override
    public void onAction(PluginContext context) {
        if (context instanceof ContactPluginContext) {
            ContactPluginContext ctx = (ContactPluginContext) context;
            String hook = ctx.getHookName();
            if (ContactHook.SignIn.equals(hook)) {
                ContactBehavior behavior = new ContactBehavior(ctx.getContact(), ContactBehavior.BEHAVIOR_SIGNIN);
                behavior.setDevice(ctx.getDevice());
                // 添加记录
                this.service.recordContactBehavior(behavior);
            }
            else if (ContactHook.DeviceTimeout.equals(hook)) {
                ContactBehavior behavior = new ContactBehavior(ctx.getContact(), ContactBehavior.BEHAVIOR_SIGNOUT);
                behavior.setDevice(ctx.getDevice());
                // 添加记录
                this.service.recordContactBehavior(behavior);
            }
            else if (ContactHook.Comeback.equals(hook)) {
                ContactBehavior behavior = new ContactBehavior(ctx.getContact(), ContactBehavior.BEHAVIOR_SIGNIN);
                behavior.setDevice(ctx.getDevice());
                // 添加记录
                this.service.recordContactBehavior(behavior);
            }
            else if (ContactHook.SignOut.equals(hook)) {
                ContactBehavior behavior = new ContactBehavior(ctx.getContact(), ContactBehavior.BEHAVIOR_SIGNOUT);
                behavior.setDevice(ctx.getDevice());
                // 添加记录
                this.service.recordContactBehavior(behavior);
            }
        }
    }
}