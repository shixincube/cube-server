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

package cube.dispatcher.hub.handler;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.dispatcher.Performer;
import cube.dispatcher.hub.HubCellet;
import cube.hub.HubAction;
import cube.hub.HubStateCode;
import cube.hub.SignalBuilder;
import cube.hub.data.ChannelCode;
import cube.hub.signal.ChannelCodeSignal;
import cube.hub.signal.Signal;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 辅助函数。
 */
public class Helper {

    private Helper() {
    }

    public static ChannelCode checkChannelCode(String code, HttpServletResponse response,
                                               Performer performer) {
        if (null == code || code.length() == 0) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return null;
        }

        ChannelCodeSignal channelCodeSignal = new ChannelCodeSignal(code);
        ActionDialect requestAction = new ActionDialect(HubAction.Channel.name);
        requestAction.addParam("signal", channelCodeSignal.toJSON());

        ActionDialect responseAction = performer.syncTransmit(HubCellet.NAME, requestAction);
        if (null == responseAction) {
            response.setStatus(HttpStatus.FORBIDDEN_403);
            return null;
        }

        int stateCode = responseAction.getParamAsInt("code");
        if (HubStateCode.Ok.code != stateCode) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return null;
        }

        Signal resultSignal = SignalBuilder.build(responseAction.getParamAsJson("signal"));
        if (resultSignal instanceof ChannelCodeSignal) {
            ChannelCode channelCode = ((ChannelCodeSignal)resultSignal).getChannelCode();
            if (null != channelCode) {
                // 校验有效期
                if (System.currentTimeMillis() >= channelCode.expiration) {
                    // 过期
                    Logger.w(Helper.class, "#checkChannelCode - channel code expired : " + channelCode.code);
                    return null;
                }
                else {
                    return channelCode;
                }
            }
            else {
                response.setStatus(HttpStatus.NOT_FOUND_404);
                return null;
            }
        }
        else {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return null;
        }
    }
}
