package cube.common.entity;

import cell.util.Utils;
import org.json.JSONObject;

/**
 * 联系人行为描述。
 */
public class ContactBehavior extends Entity {

    /**
     * 联系人签入系统。
     */
    public final static String BEHAVIOR_SIGNIN = "SignIn";

    /**
     * 联系人签出系统。
     */
    public final static String BEHAVIOR_SIGNOUT = "SignOut";

    private Contact contact;

    private String behavior;

    private Device device;

    /**
     * 构造函数。
     *
     * @param contact 联系人。
     * @param behavior 行为描述。
     */
    public ContactBehavior(Contact contact, String behavior) {
        super(Utils.generateSerialNumber(), contact.getDomain());
        this.contact = contact;
        this.behavior = behavior;
    }

    /**
     * 构造函数。
     *
     * @param json 结构 JSON 。
     */
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

    /**
     * 设置设备。
     *
     * @param device 指定设备。
     */
    public void setDevice(Device device) {
        this.device = device;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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
