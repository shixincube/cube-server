/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.ferry.plugin;

import cell.core.talk.dialect.ActionDialect;
import cube.common.entity.Message;
import cube.ferry.FerryAction;
import cube.ferry.FerryPacket;
import cube.ferry.FerryPort;
import cube.plugin.HookResult;
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
    public HookResult launch(PluginContext context) {
        Message message = (Message) context.get("message");

        ActionDialect actionDialect = new ActionDialect(FerryAction.Ferry.name);
        actionDialect.addParam("port", FerryPort.UpdateMessage);
        actionDialect.addParam("message", message.toCompactJSON());

        this.service.pushToBoat(message.getDomain().getName(), new FerryPacket(actionDialect));

        return null;
    }
}
