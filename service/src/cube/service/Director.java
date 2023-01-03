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

package cube.service;

import cell.core.talk.dialect.ActionDialect;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 导演机。
 */
public class Director {

    private final static String sPerformerKey = "_performer";

    private final static String sDirectorKey = "_director";

    protected Director() {
    }

    public static ActionDialect attachDirector(ActionDialect dialect, Contact contact) {
        return Director.attachDirector(dialect, contact.getId(), contact.getDomain().getName());
    }

    public static ActionDialect attachDirector(ActionDialect dialect, Contact contact, Device device) {
        return Director.attachDirector(dialect, contact.getId(), contact.getDomain().getName(), device);
    }

    public static ActionDialect attachDirector(ActionDialect dialect, long contactId, String domain) {
        JSONObject director = new JSONObject();
        try {
            director.put("id", contactId);
            director.put("domain", domain);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        dialect.addParam(sDirectorKey, director);
        return dialect;
    }

    public static ActionDialect attachDirector(ActionDialect dialect, long contactId, String domain, Device device) {
        JSONObject director = new JSONObject();
        try {
            director.put("id", contactId);
            director.put("domain", domain);
            director.put("device", device.toCompactJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        dialect.addParam(sDirectorKey, director);
        return dialect;
    }

    public static void copyPerformer(ActionDialect src, ActionDialect dest) {
        JSONObject performer = src.getParamAsJson(sPerformerKey);
        dest.addParam(sPerformerKey, performer);
    }
}
