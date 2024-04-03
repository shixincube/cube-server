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

package cube.service.aigc.plugin;

import cube.common.entity.Contact;
import cube.common.entity.KnowledgeBaseInfo;
import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.aigc.AIGCService;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactPluginContext;

import java.util.List;

public class ContactEventPlugin implements Plugin {

    private final AIGCService service;

    public ContactEventPlugin(AIGCService service) {
        this.service = service;
    }

    @Override
    public void setup() {
        // Nothing
    }

    @Override
    public void teardown() {
        // Nothing
    }

    @Override
    public HookResult launch(PluginContext context) {
        ContactPluginContext ctx = (ContactPluginContext) context;
        String hook = ctx.getHookName();

        if (ContactHook.SignIn.equals(hook)) {
            Contact contact = ctx.getContact();
            List<KnowledgeBaseInfo> list = this.service.getKnowledgeFramework().getKnowledgeBaseInfos(contact.getId());
            if (null != list) {
                for (KnowledgeBaseInfo info : list) {
                    this.service.getKnowledgeFramework().getKnowledgeBase(contact.getId(), info.name);
                }
            }
        }
        else if (ContactHook.SignOut.equals(hook) || ContactHook.DeviceTimeout.equals(hook)) {
            Contact contact = ctx.getContact();
            this.service.getKnowledgeFramework().freeBase(contact.getDomain().getName(),
                    contact.getId());
        }

        return null;
    }
}
