/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.test;

import cell.api.Nucleus;
import cell.api.Speakable;
import cell.api.TalkListener;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkError;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.auth.AuthConsts;
import cube.common.Packet;
import cube.common.action.ContactAction;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import org.json.JSONException;
import org.json.JSONObject;

public class ContactTest implements TalkListener {

    private Nucleus nucleus;

    private String host = "127.0.0.1";

    private int port = 7000;

    private Object mutex = new Object();

    public ContactTest() {

    }

    public void start() {
        Logger.i(this.getClass(), "Start");

        this.nucleus = new Nucleus();
        this.nucleus.getTalkService().setListener("Contact", this);
        this.nucleus.getTalkService().call(host, port);

        if (!this.nucleus.getTalkService().isCalled(host, port)) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        Logger.i(this.getClass(), "Stop");
        this.nucleus.destroy();
    }

    public void testSignIn() {
        Contact self = new Contact(100200300L, AuthConsts.DEFAULT_DOMAIN, "时信魔方");
        Device device = new Device("Mac", "MacBookPro Ambrose");
        self.addDevice(device);

        Logger.i(this.getClass(), "Start [testSetSelf] - " + self.getId());

        Packet packet = new Packet(ContactAction.SignIn.name, self.toJSON());
        this.nucleus.getTalkService().speak("Contact", packet.toDialect());

        synchronized (this.mutex) {
            try {
                this.mutex.wait(10000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Logger.i(this.getClass(), "End [testSetSelf] - " + self.getId());
    }

    public void testGetContact() throws JSONException {
        Logger.i(this.getClass(), "Start [testGetContact]");

        JSONObject json = new JSONObject();
        json.put("id", 300200100);
        Packet packet = new Packet(ContactAction.GetContact.name, json);
        this.nucleus.getTalkService().speak("Contact", packet.toDialect());

        synchronized (this.mutex) {
            try {
                this.mutex.wait(10000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Logger.i(this.getClass(), "End [testGetContact]");
    }

    @Override
    public void onListened(Speakable speakable, String cellet, Primitive primitive) {
        ActionDialect dialect = new ActionDialect(primitive);
        String action = dialect.getName();
        if (action.equals(ContactAction.SignIn.name)) {
            Packet packet = new Packet(dialect);
            Logger.i(action, "Data: " + packet.data.toString());
            synchronized (this.mutex) {
                this.mutex.notify();
            }
        }
        else if (action.equals(ContactAction.GetContact.name)) {
            Packet packet = new Packet(dialect);
            Logger.i(action, "Data: " + packet.data.toString());
            synchronized (this.mutex) {
                this.mutex.notify();
            }
        }
    }

    @Override
    public void onListened(Speakable speakable, String s, PrimitiveInputStream primitiveInputStream) {

    }

    @Override
    public void onSpoke(Speakable speakable, String s, Primitive primitive) {

    }

    @Override
    public void onAck(Speakable speakable, String s, Primitive primitive) {

    }

    @Override
    public void onSpeakTimeout(Speakable speakable, String s, Primitive primitive) {

    }

    @Override
    public void onContacted(Speakable speakable) {
        Logger.i(this.getClass(), "onContacted");
    }

    @Override
    public void onQuitted(Speakable speakable) {
        Logger.i(this.getClass(), "onQuitted");
    }

    @Override
    public void onFailed(Speakable speakable, TalkError talkError) {

    }

    public static void main(String[] args) {
        ContactTest test = new ContactTest();

        test.start();

        try {

            test.testSignIn();

            test.testGetContact();

        } catch (Exception e) {
            e.printStackTrace();
        }

        test.stop();
    }
}
