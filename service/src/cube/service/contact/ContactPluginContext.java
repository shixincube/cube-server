/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.contact;

import cube.auth.AuthToken;
import cube.common.JSONable;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.plugin.PluginContext;
import cube.util.DummyDevice;
import org.json.JSONObject;

/**
 * 联系人模块插件上下文。
 */
public class ContactPluginContext extends PluginContext implements JSONable {

    private final String hookName;

    private final Contact contact;

    private Device device;

    private AuthToken authToken;

    private String newName;

    private JSONObject newContext;

    public ContactPluginContext(String hookName, Contact contact) {
        super();
        this.hookName = hookName;
        this.contact = contact;
    }

    public ContactPluginContext(String hookName, Contact contact, Device device) {
        super();
        this.hookName = hookName;
        this.contact = contact;
        this.device = device;
    }

    public ContactPluginContext(String hookName, AuthToken authToken, Contact contact, Device device) {
        super();
        this.hookName = hookName;
        this.authToken = authToken;
        this.contact = contact;
        this.device = device;
    }

    public String getHookName() {
        return this.hookName;
    }

    public Contact getContact() {
        return this.contact;
    }

    public Device getDevice() {
        return this.device;
    }

    public AuthToken getAuthToken() {
        return this.authToken;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public String getNewName() {
        return this.newName;
    }

    public void setNewContext(JSONObject newContext) {
        this.newContext = newContext;
    }

    public JSONObject getNewContext() {
        return this.newContext;
    }

    @Override
    public Object get(String name) {
        if (name.equalsIgnoreCase("contact")) {
            return this.contact;
        }
        else if (name.equalsIgnoreCase("authToken")) {
            return this.authToken;
        }
        else if (name.equalsIgnoreCase("device")) {
            return this.device;
        }
        else if (name.equalsIgnoreCase("newName")) {
            return this.newName;
        }
        else if (name.equalsIgnoreCase("newContext")) {
            return this.newContext;
        }
        else {
            return null;
        }
    }

    @Override
    public void set(String name, Object value) {
        if (name.equalsIgnoreCase("newName")) {
            this.newName = (null != value && value instanceof String) ? (String) value : null;
        }
        else if (name.equalsIgnoreCase("newContext")) {
            this.newContext = (null != value && value instanceof JSONObject) ? (JSONObject) value : null;
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("contact", this.contact.toJSON());

        if (null != this.device) {
            json.put("device", this.device.toJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("contact", this.contact.toCompactJSON());

        if (null != this.device) {
            json.put("device", this.device.toCompactJSON());
        }

        return json;
    }
}
