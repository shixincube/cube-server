/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.service.aigc;

import cell.core.talk.TalkContext;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.CapabilitySet;
import cube.common.entity.Contact;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.plugin.PluginSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * AIGC 服务。
 */
public class AIGCService extends AbstractModule {

    public final static String NAME = "AIGC";

    private AIGCCellet cellet;

    private List<AIGCUnit> unitList;

    public AIGCService(AIGCCellet cellet) {
        this.cellet = cellet;
        this.unitList = new ArrayList<>();
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public <T extends PluginSystem> T getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {

    }

    public AIGCUnit setupUnit(Contact contact, CapabilitySet capabilitySet, TalkContext context) {
        synchronized (this.unitList) {
            for (AIGCUnit unit : this.unitList) {
                if (unit.getContact().getId().equals(contact.getId())) {
                    unit.setCapabilitySet(capabilitySet);
                    unit.setTalkContext(context);
                    return unit;
                }
            }

            AIGCUnit unit = new AIGCUnit(contact, capabilitySet, context);
            this.unitList.add(unit);
            return unit;
        }
    }

    public void teardownUnit(Contact contact) {

    }

    public AIGCChannel requestChannel() {
        return null;
    }

    public void chat() {

    }
}
