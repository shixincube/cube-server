/*
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
import cell.util.log.Logger;
import cube.common.UniqueKey;
import cube.common.entity.Conference;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.common.entity.Invitation;
import cube.core.*;
import cube.plugin.PluginSystem;
import cube.service.auth.AuthService;
import cube.service.contact.ContactManager;
import cube.storage.StorageType;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
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

    /**
     * 创建会议。
     *
     * @param founder
     * @param subject
     * @param password
     * @param summary
     * @param scheduleTime
     * @param expireTime
     * @param invitees
     * @return
     */
    public Conference createConference(Contact founder, String subject, String password, String summary,
                                       long scheduleTime, long expireTime, List<Invitation> invitees) {
        // 创建会议
        Conference conference = new Conference(Utils.generateSerialNumber(),
                founder.getDomain().getName(), founder);

        // 设置属性
        conference.setSubject(subject);
        if (null != password)
            conference.setPassword(password);
        if (null != summary)
            conference.setSummary(summary);
        conference.setScheduleTime(scheduleTime);
        conference.setExpireTime(expireTime);

        // 设置邀请人
        if (null != invitees) {
            for (Invitation invitation : invitees) {
                conference.addInvitee(invitation);
            }
        }

        // 创建会议管理联系人
        ConferenceAdministrator ca = new ConferenceAdministrator(conference.getId(), founder.getDomain().getName(),
                founder.getName());
        // 创建群组
        Group group = new Group(conference.getId(), founder.getDomain().getName(),
                conference.getCode(), ca, conference.getCreation());
        // 设置群组的标签为 conference
        group.setTag("conference");
        group = ContactManager.getInstance().createGroup(group);
        // 设置群组
        conference.getRoom().setParticipantGroup(group);

        // 写入缓存
        this.conferenceCache.put(new CacheKey(conference.getUniqueKey()), new CacheValue(conference.toJSON()));

        // 将会议写入存储
        this.storage.writeConference(conference);

        return conference;
    }

    /**
     * 查询指定联系人参与或者创建的会议。
     *
     * @param contact
     * @param beginning
     * @param ending
     * @return
     */
    public List<Conference> listConferences(Contact contact, long beginning, long ending) {
        List<Conference> result = new ArrayList<>();

        String domain = contact.getDomain().getName();
        // 从存储里读取 ID
        List<Long> confIdList = this.storage.listConferenceId(domain, contact.getId(),
                beginning, ending);

        if (null == confIdList || confIdList.isEmpty()) {
            return result;
        }

        for (Long confId : confIdList) {
            Conference conference = null;

            CacheValue cv = this.conferenceCache.get(new CacheKey(UniqueKey.make(confId, domain)));
            if (null != cv) {
                conference = new Conference(cv.get());
            }
            else {
                conference = this.storage.readConference(domain, confId);
            }

            if (null != conference) {
                // 填充数据
                fillConference(conference);
                result.add(conference);
            }
            else {
                Logger.e(this.getClass(), "#listConference - Can NOT find conference : " + confId);
            }
        }

        // 排序
        result.sort(new Comparator<Conference>() {
            @Override
            public int compare(Conference c1, Conference c2) {
                return (int)(c2.getScheduleTime() - c1.getScheduleTime());
            }
        });

        return result;
    }

    /**
     * 获取指定 ID 的会议。
     *
     * @param domain
     * @param conferenceId
     * @return
     */
    public Conference getConference(String domain, Long conferenceId) {
        CacheValue cv = this.conferenceCache.get(new CacheKey(UniqueKey.make(conferenceId, domain)));
        if (null != cv) {
            Conference conference = new Conference(cv.get());
            this.fillConference(conference);
            return conference;
        }

        Conference conference = this.storage.readConference(domain, conferenceId);
        this.fillConference(conference);
        return conference;
    }

    /**
     * 接受会议邀请。
     *
     * @param conferenceId
     * @param invitee
     * @return
     */
    public Conference acceptInvitation(Long conferenceId, Contact invitee) {
        Conference conference = this.getConference(invitee.getDomain().getName(), conferenceId);
        if (null == conference) {
            return null;
        }

        Invitation invitation = conference.getInvitee(invitee.getId());
        if (null != invitation) {
            invitation.setAccepted(true);

            // 更新缓存
            this.conferenceCache.put(new CacheKey(conference.getUniqueKey()), new CacheValue(conference.toJSON()));

            // 更新存储
            this.storage.writeParticipant(invitee.getDomain().getName(), conferenceId, invitation);
        }

        return conference;
    }

    /**
     * 拒绝会议邀请。
     *
     * @param conferenceId
     * @param invitee
     * @return
     */
    public Conference declineInvitation(Long conferenceId, Contact invitee) {
        Conference conference = this.getConference(invitee.getDomain().getName(), conferenceId);
        if (null == conference) {
            return null;
        }

        Invitation invitation = conference.getInvitee(invitee.getId());
        if (null != invitation) {
            invitation.setAccepted(false);

            // 更新缓存
            this.conferenceCache.put(new CacheKey(conference.getUniqueKey()), new CacheValue(conference.toJSON()));

            // 更新存储
            this.storage.writeParticipant(invitee.getDomain().getName(), conferenceId, invitation);
        }

        return conference;
    }

    @Override
    public PluginSystem<?> getPluginSystem() {
        return null;
    }

    @Override
    public void onTick(cube.core.Module module, Kernel kernel) {

    }

    private Conference fillConference(Conference conference) {
        String domain = conference.getDomain().getName();
        if (null == conference.getFounder()) {
            Contact contact = ContactManager.getInstance().getContact(domain, conference.getFounderId());
            conference.setFounder(contact);
        }

        if (null == conference.getPresenter()) {
            Contact contact = ContactManager.getInstance().getContact(domain, conference.getPresenterId());
            conference.setPresenter(contact);
        }

        // 填写群组数据
        Group group = ContactManager.getInstance().getGroup(conference.getRoom().getParticipantGroupId(),
                domain);
        conference.getRoom().setParticipantGroup(group);

        return conference;
    }
}
