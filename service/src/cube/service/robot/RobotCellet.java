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

package cube.service.robot;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.robot.RobotAction;
import org.json.JSONObject;

/**
 * 机器人 Cellet 服务。
 */
public class RobotCellet extends AbstractCellet {

    public final static String NAME = "Robot";

    private RobotService service;

    public RobotCellet() {
        super(NAME);
    }

    @Override
    public boolean install() {
        this.service = new RobotService();
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(RobotService.NAME, this.service);

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(RobotService.NAME);

        this.service = null;
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (RobotAction.Event.name.equals(action)) {
            String name = dialect.getParamAsString("name");
            JSONObject data = dialect.getParamAsJson("data");
            this.service.recordEvent(name, data);
        }
    }
}
