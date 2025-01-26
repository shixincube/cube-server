/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.hub;

import cube.hub.signal.*;
import org.json.JSONObject;

/**
 * 信令构建器。
 */
public class SignalBuilder {

    private SignalBuilder() {
    }

    public static Signal build(JSONObject signalJson) {
        String name = signalJson.getString("name");

        if (ChannelCodeSignal.NAME.equals(name)) {
            return new ChannelCodeSignal(signalJson);
        }
        else if (GetAccountSignal.NAME.equals(name)) {
            return new GetAccountSignal(signalJson);
        }
        else if (GetConversationsSignal.NAME.equals(name)) {
            return new GetConversationsSignal(signalJson);
        }
        else if (GetContactZoneSignal.NAME.equals(name)) {
            return new GetContactZoneSignal(signalJson);
        }
        else if (GetMessagesSignal.NAME.equals(name)) {
            return new GetMessagesSignal(signalJson);
        }
        else if (GetGroupSignal.NAME.equals(name)) {
            return new GetGroupSignal(signalJson);
        }
        else if (ReportSignal.NAME.equals(name)) {
            return new ReportSignal(signalJson);
        }
        else if (GetFileLabelSignal.NAME.equals(name)) {
            return new GetFileLabelSignal(signalJson);
        }
        else if (RollPollingSignal.NAME.equals(name)) {
            return new RollPollingSignal(signalJson);
        }
        else if (SendMessageSignal.NAME.equals(name)) {
            return new SendMessageSignal(signalJson);
        }
        else if (LoginQRCodeSignal.NAME.equals(name)) {
            return new LoginQRCodeSignal(signalJson);
        }
        else if (LogoutSignal.NAME.equals(name)) {
            return new LogoutSignal(signalJson);
        }
        else if (AddFriendSignal.NAME.equals(name)) {
            return new AddFriendSignal(signalJson);
        }
        else if (PassBySignal.NAME.equals(name)) {
            return new PassBySignal(signalJson);
        }
        else if (SilenceSignal.NAME.equals(name)) {
            return new SilenceSignal(signalJson);
        }
        else if (ReadySignal.NAME.equals(name)) {
            return new ReadySignal(signalJson);
        }
        else if (QueryChannelCodeSignal.NAME.equals(name)) {
            return new QueryChannelCodeSignal(signalJson);
        }
        else if (AckSignal.NAME.equals(name)) {
            return new AckSignal(signalJson);
        }

        return null;
    }
}
