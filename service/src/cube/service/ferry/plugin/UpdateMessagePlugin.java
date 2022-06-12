/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2022 Cube Team.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.ferry.plugin;

import cell.core.talk.dialect.ActionDialect;
import cube.common.entity.Message;
import cube.ferry.FerryAction;
import cube.ferry.FerryPacket;
import cube.ferry.FerryPort;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.ferry.FerryService;

/**
 * 更新消息插件。
 */
public class UpdateMessagePlugin implements Plugin {

    private FerryService service;

    public UpdateMessagePlugin(FerryService service) {
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
        Message message = (Message) context.get("message");

        ActionDialect actionDialect = new ActionDialect(FerryAction.Ferry.name);
        actionDialect.addParam("port", FerryPort.UpdateMessage);
        actionDialect.addParam("message", message.toCompactJSON());

        this.service.pushToBoat(message.getDomain().getName(), new FerryPacket(actionDialect));
    }
}
