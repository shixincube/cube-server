package cube.robot;

import org.json.JSONObject;

public class Account {

    public long id;

    public String name;

    public String password;

    public boolean isAdmin;

    public String avatar;

    public String fullName;

    public long creationTime;

    public int state;

    public String token;

    public String lastAddress;

    public long lastLoginTime;

    public JSONObject lastDevice;

    public Account() {
    }
}
