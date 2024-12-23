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
