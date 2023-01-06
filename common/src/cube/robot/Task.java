package cube.robot;

import cube.common.JSONable;
import org.json.JSONObject;

public class Task implements JSONable {

    public static final int FLAG_DISPOSABLE = 0;

    public final static int FLAG_SUNDAY = 0x1;
    public final static int FLAG_MONDAY = 0x2;
    public final static int FLAG_TUESDAY = 0x4;
    public final static int FLAG_WEDNESDAY = 0x8;
    public final static int FLAG_THURSDAY = 0x10;
    public final static int FLAG_FRIDAY = 0x20;
    public final static int FLAG_SATURDAY = 0x40;

    public static final int FLAG_EVERYDAY = 0x7F;

    public long id;

    public String taskName;

    public long timeInMillis;

    public long timeFlag;

    public long delay = 0;

    public long interval = 0;

    public int loopTimes = 1;

    public String mainFile;

    public String taskFile;

    public long creationTime;

    public long lastModified;

    public Task(long id, String taskName, long timeInMillis, long timeFlag, String mainFile,
                String taskFile) {
        this.id = id;
        this.taskName = taskName;
        this.timeInMillis = timeInMillis;
        this.timeFlag = timeFlag;
        this.mainFile = mainFile;
        this.taskFile = taskFile;
        this.creationTime = System.currentTimeMillis();
        this.lastModified = this.creationTime;
    }

    public Task(JSONObject json) {
        this.id = json.getLong("id");
        this.taskName = json.getString("taskName");
        this.timeInMillis = json.getLong("timeInMillis");
        this.timeFlag = json.getLong("timeFlag");
        this.delay = json.has("delay") ? json.getLong("delay") : 0;
        this.interval = json.has("interval") ? json.getLong("interval") : 0;
        this.loopTimes = json.has("loopTimes") ? json.getInt("loopTimes") : 1;
        this.taskFile = json.getString("taskFile");
        this.mainFile = json.getString("mainFile");
        this.creationTime = json.has("creationTime") ? json.getLong("creationTime") : 0;
        this.lastModified = json.has("lastModified") ? json.getLong("lastModified") : 0;
    }

    public boolean isImmediate() {
        return this.timeInMillis == 0 && this.timeFlag == 0;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("taskName", this.taskName);
        json.put("timeInMillis", this.timeInMillis);
        json.put("timeFlag", this.timeFlag);
        json.put("delay", this.delay);
        json.put("interval", this.interval);
        json.put("loopTimes", this.loopTimes);
        json.put("mainFile", this.mainFile);
        json.put("taskFile", this.taskFile);
        json.put("creationTime", this.creationTime);
        json.put("lastModified", this.lastModified);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return null;
    }
}
