/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.riskmgmt;

import cube.core.AbstractCellet;
import cube.dispatcher.Performer;
import cube.util.HttpServer;
import org.eclipse.jetty.server.handler.ContextHandler;

/**
 * 风险管理单元。
 */
public class RiskManagementCellet extends AbstractCellet {

    public final static String NAME = "RiskMgmt";

    /**
     * 执行机。
     */
    private Performer performer;

    public RiskManagementCellet() {
        super(NAME);
    }

    @Override
    public boolean install() {
        this.performer = (Performer) nucleus.getParameter("performer");

        // 配置 HTTP/HTTPS 服务的句柄
        HttpServer httpServer = this.performer.getHttpServer();

        ContextHandler contactRiskHandler = new ContextHandler();
        contactRiskHandler.setContextPath(ContactRiskHandler.PATH);
        contactRiskHandler.setHandler(new ContactRiskHandler(this.performer));
        httpServer.addContextHandler(contactRiskHandler);

        return true;
    }

    @Override
    public void uninstall() {
    }
}
