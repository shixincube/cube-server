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

package cube.dispatcher.aigc.handler.app;

import cube.dispatcher.aigc.handler.AIGCHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

public final class Helper {

    private Helper() {
    }

    public static void respondOk(AIGCHandler handler, HttpServletResponse response, JSONObject data) {
        JSONObject payload = new JSONObject();
        payload.put("status", "Success");
        payload.put("message", "");
        payload.put("data", data);
        handler.respondOk(response, payload);
        handler.complete();
    }

    public static void respondFailure(AIGCHandler handler, HttpServletResponse response, int status) {
        JSONObject payload = new JSONObject();
        payload.put("status", "Fail");
        payload.put("message", "");
        handler.respond(response, status);
        handler.complete();
    }
}
