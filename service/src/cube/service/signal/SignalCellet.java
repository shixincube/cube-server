/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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
