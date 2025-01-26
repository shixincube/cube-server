/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.signal;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.common.Packet;
import cube.common.action.SignalAction;
import cube.common.entity.Contact;
import cube.common.entity.Signal;
import cube.common.state.SignalStateCode;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.Director;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 信号收发单元。
 */
public class SignalCellet extends AbstractCellet {

    private SignalService signalService;

    public SignalCellet() {
        super(SignalService.NAME);
    }

    @Override
    public boolean install() {
        this.signalService = new SignalService(this);

        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.installModule(SignalService.NAME, this.signalService);

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.getNucleus().getParameter("kernel");
        kernel.uninstallModule(SignalService.NAME);
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        super.onListened(talkContext, primitive);

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        Packet packet = new Packet(dialect);

        JSONObject data = packet.data;

        if (SignalAction.Direct.name.equals(action)) {
            Signal signal = new Signal(data);
            this.signalService.emitSignalDirectly(signal);

            // 响应
            this.makeResponse(dialect, packet, SignalStateCode.Ok.code, signal.toCompactJSON());
        }
        else if (SignalAction.Broadcast.name.equals(action)) {
            JSONObject signalJson = data.getJSONObject("signal");
            JSONArray broadcastArray = data.getJSONArray("list");

            ArrayList<Contact> destination = new ArrayList<>();
            for (int i = 0; i < broadcastArray.length(); ++i) {
                JSONObject contactJson = broadcastArray.getJSONObject(i);
                destination.add(new Contact(contactJson));
            }

            Signal signal = new Signal(signalJson);

            this.signalService.emitSignalBroadcast(signal, destination);

            // 响应
            this.makeResponse(dialect, packet, SignalStateCode.Ok.code, signal.toCompactJSON());
        }
        else {
            // 响应
            this.makeResponse(dialect, packet, SignalStateCode.Failure.code, data);
        }
    }

    private ActionDialect makeResponse(ActionDialect actionDialect, Packet request, int stateCode, JSONObject data) {
        JSONObject payload = this.makePacketPayload(stateCode, data);
        return this.makeDispatcherResponse(actionDialect, request, payload);
    }

    private JSONObject makePacketPayload(int stateCode, JSONObject data) {
        JSONObject payload = new JSONObject();
        payload.put("code", stateCode);
        payload.put("data", data);
        return payload;
    }

    private ActionDialect makeDispatcherResponse(ActionDialect actionDialect, Packet request, JSONObject packetPayload) {
        Packet response = new Packet(request.sn, request.name, packetPayload);
        ActionDialect responseDialect = response.toDialect();
        Director.copyPerformer(actionDialect, responseDialect);
        return responseDialect;
    }
}
