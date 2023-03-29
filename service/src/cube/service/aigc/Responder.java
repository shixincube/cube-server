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

package cube.service.aigc;

import cell.core.cellet.Cellet;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import org.json.JSONObject;

/**
 * 应答机。
 */
public class Responder {

    public final static String NotifierKey = "_notifier";

    private Long sn;

    private JSONObject notifier;

    private ActionDialect response;

    public Responder(ActionDialect actionDialect) {
        this.sn = Utils.generateSerialNumber();
        this.notifier = createNotifier();
        actionDialect.addParam(Responder.NotifierKey, this.notifier);
    }

    public ActionDialect waitingFor(long timeout) {
        synchronized (this.notifier) {
            try {
                this.notifier.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return this.response;
    }

    public void notifyResponse(ActionDialect response) {
        response.removeParam(Responder.NotifierKey);

        this.response = response;
        synchronized (this.notifier) {
            this.notifier.notify();
        }
    }

    public boolean isResponse(ActionDialect response) {
        JSONObject notifier = response.getParamAsJson(Responder.NotifierKey);
        if (notifier.getLong("sn") == this.sn.longValue()) {
            return true;
        }

        return false;
    }

    private JSONObject createNotifier() {
        JSONObject json = new JSONObject();
        json.put("sn", this.sn.longValue());
        json.put("ts", System.currentTimeMillis());
        return json;
    }
}
