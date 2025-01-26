/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
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
