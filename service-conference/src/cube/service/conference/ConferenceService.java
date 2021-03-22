/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.service.conference;

import cell.util.Utils;
import cube.common.entity.Conference;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.core.*;
import cube.plugin.PluginSystem;
import cube.service.auth.AuthService;
import cube.service.contact.ContactManager;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.util.List;

/**
 * 会议服务。
 */
public class ConferenceService extends AbstractModule {

    public static String NAME = "Conference";

    private ConferenceStorage storage;

    private Cache conferenceCache;

    public ConferenceService() {
    }

    @Override
    public void start() {
        // 获取通用缓存器
        this.conferenceCache = this.getKernel().getCache("General");

        // 读取存储配置
        JSONObject config = ConfigUtils.readStorageConfig();
        if (config.has(ConferenceService.NAME)) {
            config = config.getJSONObject(ConferenceService.NAME);
            if (config.getString("type").equalsIgnoreCase("SQLite")) {
                this.storage = new ConferenceStorage(StorageType.SQLite, config);
            }
            else {
                this.storage = new ConferenceStorage(StorageType.MySQL, config);
            }
        }
        else {
            config.put("file", "storage/ConferenceService.db");
            this.storage = new ConferenceStorage(StorageType.SQLite, config);
        }
        // 开启存储
        this.storage.open();

        AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
        this.storage.execSelfChecking(authService.getDomainList());
    }

    @Override
    public void stop() {
        this.storage.close();
    }

    public Conference createConference(Contact founder, String subject, String password, String summary,
            long scheduleTime, long expireTime, Group pregroup) {
        // 创建实例
        Conference conference = new Conference(Utils.generateSerialNumber(),
                founder.getDomain().getName(), founder);

        // 创建会议管理联系人
        ConferenceAdministrator ca = new ConferenceAdministrator(conference.getId(), founder.getDomain().getName(),
                founder.getName());
        // 创建群组
        Group group = new Group(conference.getId(), founder.getDomain().getName(),
                conference.getCode(), ca, conference.getCreation());
        for (Contact member : pregroup.getMembers()) {
            group.addMember(member);
        }
        group = ContactManager.getInstance().createGroup(group);
        // 设置群组
        conference.setParticipantGroup(group);

        // 设置属性
        conference.setSubject(subject);
        if (null != password)
            conference.setPassword(password);
        if (null != summary)
            conference.setSummary(summary);
        conference.setScheduleTime(scheduleTime);
        conference.setExpireTime(expireTime);

        // 更新邀请参与人数据
        for (Contact member : group.getMembers()) {
            if (member.getId().equals(founder.getId())) {
                continue;
            }

            this.storage.writeParticipant(founder.getDomain().getName(), conference.getId(),
                    member.getId(), "messaging");
        }

        // 写入缓存
        this.conferenceCache.put(new CacheKey(conference.getCode()), new CacheValue(conference.toJSON()));

        // 将会议写入存储
        this.storage.writeConference(conference);

        return conference;
    }

    public List<Conference> getConference(Contact founder) {
        // 从存储器里读取数据
        List<Conference> list = this.storage.readConferences(founder.getDomain().getName(), founder.getId());
        if (null == list) {
            return null;
        }

        for (Conference conference : list) {
            conference.setFounder(founder);
            fillConference(conference);
        }

        return list;
    }

    public List<Conference> listConference(long beginning, long ending) {
        return null;
    }

    public void updateConference(Conference conference) {

    }

    @Override
    public PluginSystem<?> getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(cube.core.Module module, Kernel kernel) {

    }

    private Conference fillConference(Conference conference) {
        if (null != conference.getFounder()) {
            Contact contact = ContactManager.getInstance().getContact(conference.getDomain().getName(),
                    conference.getFounderId());
            conference.setFounder(contact);
        }

        // 填写群组数据
        Group group = ContactManager.getInstance().getGroup(conference.getParticipantGroupId(),
                conference.getDomain().getName());
        conference.setParticipantGroup(group);

        return conference;
    }
}
