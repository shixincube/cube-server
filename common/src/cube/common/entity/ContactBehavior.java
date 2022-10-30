package cube.common.entity;

import cell.util.Utils;
import org.json.JSONObject;

/**
 * 联系人行为描述。
 */
public class ContactBehavior extends Entity {

    public final static String BEHAVIOR_SIGNIN = "SignIn";
    public final static String BEHAVIOR_SIGNOUT = "SignOut";

    private Contact contact;

    private String behavior;

    private Device device;

    public ContactBehavior(Contact contact, String behavior) {
        super(Utils.generateSerialNumber(), contact.getDomain());
        this.contact = contact;
        this.behavior = behavior;
    }

    public ContactBehavior(JSONObject json) {
        super(json);
        this.contact = new Contact(json.getJSONObject("contact"));
        this.behavior = json.getString("behavior");
        if (json.has("device")) {
            this.device = new Device(json.getJSONObject("device"));
        }
    }

    /**
     * 获取联系人。
     *
     * @return 返回联系人。
     */
    public Contact getContact() {
        return this.contact;
    }

    /**
     * 获取行为描述。
     *
     * @return 返回行为描述。
     */
    public String getBehavior() {
        return this.behavior;
    }

    /**
     * 获取行为发生时的设备。
     *
     * @return 返回行为发生时的设备。
     */
    public Device getDevice() {
        return this.device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("contact", this.contact.toJSON());
        json.put("behavior", this.behavior);
        if (null != this.device) {
            json.put("device", this.device.toJSON());
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = super.toCompactJSON();
        json.put("contact", this.contact.toCompactJSON());
        json.put("behavior", this.behavior);
        if (null != this.device) {
            json.put("device", this.device.toCompactJSON());
        }
        return json;
    }
}
