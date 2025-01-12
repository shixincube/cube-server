/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2020-2025 Ambrose Xu.
 */

package cube.service.cv;

import cell.core.talk.TalkContext;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.entity.Contact;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.plugin.PluginSystem;
import cube.service.auth.AuthService;

import java.util.ArrayList;
import java.util.List;

/**
 * 计算机视觉功能服务模块。
 */
public class CVService extends AbstractModule {

    public final static String NAME = "CV";

    private CVCellet cellet;

    private List<CVEndpoint> endpointList;

    public CVService(CVCellet cellet) {
        this.cellet = cellet;
        this.endpointList = new ArrayList<>();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        this.endpointList.clear();
    }

    @Override
    public <T extends PluginSystem> T getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(Module module, Kernel kernel) {
    }

    public void setup(Contact contact, TalkContext talkContext) {
        synchronized (this.endpointList) {
            for (CVEndpoint endpoint : this.endpointList) {
                if (endpoint.contact.getId().longValue() == contact.getId().longValue()) {
                    this.endpointList.remove(endpoint);
                    break;
                }
            }

            this.endpointList.add(new CVEndpoint(contact, talkContext));

            Logger.i(this.getClass(), "#setup - Setup id: " + contact.getId());
        }
    }

    public void teardown(Contact contact, TalkContext talkContext) {
        synchronized (this.endpointList) {
            for (CVEndpoint endpoint : this.endpointList) {
                if (endpoint.contact.getId().longValue() == contact.getId().longValue()) {
                    this.endpointList.remove(endpoint);
                    break;
                }
            }

            Logger.i(this.getClass(), "#teardown - Teardown id: " + contact.getId());
        }
    }

    /**
     * 获取令牌。
     *
     * @param tokenCode
     * @return
     */
    public AuthToken getToken(String tokenCode) {
        AuthService authService = (AuthService) this.getKernel().getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(tokenCode);
        return authToken;
    }
}
