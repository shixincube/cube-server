package cube.common.entity;

import org.json.JSONObject;

/**
 * 联系人行为描述。
 */
public class ContactBehavior extends Entity {

    private Contact contact;

    private String behavior;

    private Device device;

    public ContactBehavior(Contact contact, String behavior) {
        super(contact.id, contact.getDomain());
        this.contact = contact;
        this.behavior = behavior;
    }

    public Contact getContact() {
        return this.contact;
    }

    public String getBehavior() {
        return this.behavior;
    }

    public Device getDevice() {
        return this.device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        return json;
    }
}
