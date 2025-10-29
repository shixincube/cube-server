/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.module.task;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.AppEvent;
import cube.aigc.Generatable;
import cube.aigc.ModelConfig;
import cube.aigc.StrategyNode;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.entity.Contact;
import cube.common.entity.GeneratingOption;
import cube.common.entity.GeneratingRecord;
import cube.common.entity.User;
import cube.service.aigc.AIGCService;
import cube.service.contact.ContactManager;
import cube.util.TextUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QueryUser extends StrategyNode {

    public final static String TASK_NAME = "{{app_query_user}}";

    private AuthToken token;

    private int maxNum = 50;

    public QueryUser(AIGCService service, AuthToken token, String query) {
        super(query, TextUtils.isTextMainlyInEnglish(query) ? Language.English : Language.Chinese);
        this.token = token;
    }

    public GeneratingRecord generate(Generatable generator) {
        List<String> platforms = new ArrayList<>();
        platforms.add("Android");
        platforms.add("iOS");
        List<Contact> contacts = ContactManager.getInstance().listContacts(this.token.getDomain(), platforms);
        String data = this.makeContactTable(contacts);

        StringBuilder prompt = new StringBuilder();
        prompt.append("已知用户数据表格：\n\n");
        prompt.append(data);
        prompt.append("\n\n");
        prompt.append("请根据以上表格数据回答问题：").append("");

        return generator.generateText(ModelConfig.BAIZE_NEXT_UNIT, prompt.toString(),
                new GeneratingOption(), null);
    }

    private String makeContactTable(List<Contact> contacts) {
        StringBuilder buf = new StringBuilder();
        buf.append("| ID |");
        buf.append(" 账号名 |");
        buf.append(" 注册日期 |");
        buf.append(" 电话号码 |");
        buf.append(" 用户名称 |");
        buf.append(" 设备类型 |");
        buf.append(" 用户渠道 |");
        buf.append(" 最近登录日期 |");
        buf.append(" 最近登录IP |");
        buf.append(" 最近登录设备名 |");
        buf.append(" 最近登录设备类型 |\n");
        buf.append("|----|----|----|----|----|----|----|----|----|----|----|\n");

        int num = 0;
        for (Contact contact : contacts) {
            try {
                User user = new User(contact.getContext());
                List<AppEvent> appEvents = null;//this.service.getStorage().readAppEvents(contact.getId(), AppEvent.Session, 1);
                AppEvent sessionEvent = appEvents.isEmpty() ? null : appEvents.get(0);

                long id = contact.getId();
                String name = contact.getName();
                String date = Utils.gsDateFormat.format(new Date(contact.getTimestamp()));
                String phoneNumber = user.getPhoneNumber();
                String displayName = user.getDisplayName();
                String devicePlatform = contact.getDevice().getPlatform();
                String userChannel = user.getChannel();
                String recentTime = (null != sessionEvent) ? sessionEvent.time : "";
                String recentIP = (null != sessionEvent) ? sessionEvent.getSafeString("ip") : "";
                String recentDevice = (null != sessionEvent) ? sessionEvent.getSafeString("device") : "";
                String recentPlatform = (null != sessionEvent) ? sessionEvent.getSafeString("platform") : "";
                buf.append("|").append(id);
                buf.append("|").append(name);
                buf.append("|").append(date);
                buf.append("|").append(phoneNumber);
                buf.append("|").append(displayName);
                buf.append("|").append(devicePlatform);
                buf.append("|").append(userChannel);
                buf.append("|").append(recentTime);
                buf.append("|").append(recentIP);
                buf.append("|").append(recentDevice);
                buf.append("|").append(recentPlatform);
                buf.append("|\n");

                ++num;
                if (num >= this.maxNum) {
                    break;
                }
                if (buf.length() + 1024 > ModelConfig.BAIZE_NEXT_CONTEXT_LIMIT) {
                    break;
                }
            } catch (Exception e) {
                Logger.w(this.getClass(), "#makeContactTable", e);
            }
        }
        buf.append("\n");
        return buf.toString();
    }

    @Override
    public String perform(GeneratingRecord input) {
        return null;
    }
}
