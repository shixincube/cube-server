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
import cube.core.AbstractModule;
import cube.core.Cache;
import cube.core.Kernel;
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
            long scheduleTime, long expireTime) {
        Conference conference = new Conference(Utils.generateSerialNumber(), founder.getDomain().getName());

        Group group = new Group(conference.getId(), founder.getDomain().getName(),
                conference.getCode(), founder, conference.getCreation());
        group = ContactManager.getInstance().createGroup(group);
        conference.setParticipantGroup(group);



        return null;
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
}
