/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 时间片段。
 */
public class TimeSlice implements JSONable {

    public final int slice;

    public final long beginning;

    public final long ending;

    private List<Contact> contactList;

    // 记录每种设备类型的数量
    private Map<String, AtomicInteger> deviceCountMap;

    public TimeSlice(int slice, long beginning, long ending) {
        this.slice = slice;
        this.beginning = beginning;
        this.ending = ending;
        this.contactList = new ArrayList<>();
        this.deviceCountMap = new HashMap<>();
    }

    public void addContact(Contact contact, Device device) {
        int index = this.contactList.indexOf(contact);
        if (index >= 0) {
            this.contactList.get(index).addDevice(device);
        }
        else {
            contact.addDevice(device);
            this.contactList.add(contact);
        }

        AtomicInteger count = this.deviceCountMap.get(device.getName());
        if (null == count) {
            count = new AtomicInteger(1);
            this.deviceCountMap.put(device.getName(), count);
        }
        else {
            count.incrementAndGet();
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("slice", this.slice);
        json.put("beginning", this.beginning);
        json.put("ending", this.ending);

        JSONArray array = new JSONArray();
        for (Contact contact : this.contactList) {
            array.put(contact.toJSON());
        }
        json.put("list", array);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("slice", this.slice);
        json.put("beginning", this.beginning);
        json.put("ending", this.ending);

        json.put("numContacts", this.contactList.size());

        JSONObject numDevices = new JSONObject();

        for (Map.Entry<String, AtomicInteger> e : this.deviceCountMap.entrySet()) {
            String devName = e.getKey();
            AtomicInteger count = e.getValue();
            numDevices.put(devName, count.get());
        }

        json.put("numDevices", numDevices);

        return json;
    }
}
