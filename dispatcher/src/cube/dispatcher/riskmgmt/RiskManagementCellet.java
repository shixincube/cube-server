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
