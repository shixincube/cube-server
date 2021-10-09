/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.service.riskmgmt;

import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.plugin.PluginSystem;
import cube.service.contact.ContactHook;
import cube.service.contact.ContactManager;
import cube.service.contact.ContactManagerListener;
import cube.service.riskmgmt.plugin.ModifyContactNamePlugin;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * 风险管理。
 */
public class RiskManagement extends AbstractModule implements ContactManagerListener {

    /**
     * 服务单元名。
     */
    public final static String NAME = "RiskMgmt";

    private MainStorage mainStorage;

    private HashMap<String, SensitiveWord> sensitiveWordMap;

    private ModifyContactNamePlugin modifyContactNamePlugin;

    public RiskManagement() {
        super();
        this.sensitiveWordMap = new HashMap<>();
    }

    @Override
    public void start() {
        ContactManager.getInstance().addListener(this);

        JSONObject config = ConfigUtils.readStorageConfig();
    }

    @Override
    public void stop() {
        ContactManager.getInstance().removeListener(this);
    }

    @Override
    public PluginSystem<?> getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {

    }

    @Override
    public void onStarted(ContactManager manager) {
        this.modifyContactNamePlugin = new ModifyContactNamePlugin(this);
        manager.getPluginSystem().register(ContactHook.ModifyContactName, this.modifyContactNamePlugin);
    }

    @Override
    public void onStopped(ContactManager manager) {
        manager.getPluginSystem().deregister(ContactHook.ModifyContactName, this.modifyContactNamePlugin);
        this.modifyContactNamePlugin = null;
    }

    public List<SensitiveWord> recognizeSensitiveWord(String text) {

        return null;
    }
}
