package cube.service.filestorage;

import cell.util.log.Logger;
import cube.common.action.FileStorageAction;
import cube.common.entity.*;
import cube.common.notice.*;
import cube.service.contact.ContactManager;
import cube.util.ConfigUtils;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

public class Notifier {

    private final FileStorageService service;

    public Notifier(FileStorageService service) {
        this.service = service;
    }

    public Object deliver(JSONObject data) {
        String action = data.getString("action");

        if (FileStorageAction.GetFile.name.equals(action)) {
            // 获取文件标签
            String domain = data.getString("domain");
            String fileCode = data.getString("fileCode");
            FileLabel fileLabel = this.service.getFile(domain, fileCode);
            if (null != fileLabel) {
                JSONObject response = fileLabel.toJSON();
                if (data.getBoolean("transmitting")) {
                    // 加载文件到本地
                    String fullPath = this.service.loadFileToDisk(domain, fileCode);
                    if (null != fullPath) {
                        response.put("fullPath", fullPath);
                    }
                }
                return response;
            }
        }
        else if (FileStorageAction.PutFile.name.equals(action)) {
            JSONObject jsonData = data.getJSONObject("fileLabel");
            FileLabel label = this.service.putFile(new FileLabel(jsonData));
            if (null != label) {
                return label.toJSON();
            }
        }
        else if (FileStorageAction.FindFile.name.equals(action)) {
            String domain = data.getString("domain");
            long contactId = data.getLong("contactId");
            String fileName = data.getString("fileName");
            long lastModified = data.getLong("lastModified");
            long fileSize = data.getLong("fileSize");
            // 查找文件
            FileLabel fileLabel = this.service.findFile(domain, contactId, fileName, lastModified, fileSize);
            if (null != fileLabel) {
                return fileLabel.toCompactJSON();
            }
        }
        else if (FileStorageAction.DeleteFile.name.equals(action)) {
            String domain = data.getString("domain");
            String fileCode = data.getString("fileCode");
            FileLabel fileLabel = this.service.getFile(domain, fileCode);
            if (null != fileLabel) {
                // 删除文件
                this.service.deleteFile(domain, fileLabel);
                return fileLabel.toCompactJSON();
            }
        }
        else if (FileStorageAction.LoadFile.name.equals(action)) {
            String domain = data.getString("domain");
            String fileCode = data.getString("fileCode");
            return this.service.loadFileToDisk(domain, fileCode);
        }
        else if (FileStorageAction.SaveFile.name.equals(action)) {
            String path = data.getString("path");
            FileLabel fileLabel = new FileLabel(data.getJSONObject("fileLabel"));
            FileLabel result = this.service.saveFile(fileLabel, new File(path));
            if (null != result) {
                return result.toCompactJSON();
            }
        }
        else if (FileStorageAction.GetSharingTag.name.equals(action)) {
            String sharingCode = data.getString(GetSharingTag.SHARING_CODE);
            SharingTag sharingTag = this.service.getSharingManager().getSharingTag(sharingCode, true);
            return sharingTag;
        }
        else if (FileStorageAction.ListSharingTags.name.equals(action)) {
            Contact contact = ContactManager.getInstance().getContact(data.getString(ListSharingTags.DOMAIN),
                    data.getLong(ListSharingTags.CONTACT_ID));
            // 数据排序
            boolean desc = data.getString(ListSharingTags.ORDER).equalsIgnoreCase(ConfigUtils.ORDER_DESC);
            List<SharingTag> result = this.service.getSharingManager().listSharingTags(contact,
                    data.getBoolean(ListSharingTags.VALID),
                    data.getInt(ListSharingTags.BEGIN), data.getInt(ListSharingTags.END),
                    desc);
            return result;
        }
        else if (FileStorageAction.ListTraces.name.equals(action)) {
            Contact contact = ContactManager.getInstance().getContact(data.getString(ListSharingTraces.DOMAIN),
                    data.getLong(ListSharingTraces.CONTACT_ID));
            List<VisitTrace> result = this.service.getSharingManager().listSharingVisitTrace(contact, data.getString(ListSharingTraces.SHARING_CODE),
                    data.getInt(ListSharingTraces.BEGIN), data.getInt(ListSharingTraces.END));
            return result;
        }
        else if (FileStorageAction.Performance.name.equals(action)) {
            Contact contact = new Contact(data.getLong("contactId"), data.getString("domain"));
            FileStoragePerformance performance = this.service.getPerformance(contact);
            if (null != performance) {
                return performance.toJSON();
            }
        }
        else if (FileStorageAction.UpdatePerformance.name.equals(action)) {
            // 更新配置数据
            Contact contact = new Contact(data.getJSONObject("contact"));
            FileStoragePerformance performance = new FileStoragePerformance(data.getJSONObject("data"));
            FileStoragePerformance result = this.service.updatePerformance(contact, performance);
            return (null != result) ? result.toJSON() : null;
        }
        else if (CountSharingTags.ACTION.equals(action)) {
            Contact contact = ContactManager.getInstance().getContact(data.getString(CountSharingTags.DOMAIN),
                    data.getLong(CountSharingTags.CONTACT_ID));
            JSONObject result = new JSONObject();
            result.put(CountSharingTags.VALID_NUMBER, this.service.getSharingManager().countSharingTags(contact, true));
            result.put(CountSharingTags.INVALID_NUMBER, this.service.getSharingManager().countSharingTags(contact, false));
            return result;
        }
        else if (CountSharingVisitTraces.ACTION.equals(action)) {
            Contact contact = ContactManager.getInstance().getContact(data.getString(CountSharingVisitTraces.DOMAIN),
                    data.getLong(CountSharingVisitTraces.CONTACT_ID));
            data.put(CountSharingVisitTraces.TOTAL,
                    this.service.getSharingManager().countSharingVisitTrace(contact,
                            data.getString(CountSharingVisitTraces.SHARING_CODE)));
            return data;
        }
        else if (TraverseVisitTrace.ACTION.equals(action)) {
            Contact contact = ContactManager.getInstance().getContact(data.getString(TraverseVisitTrace.DOMAIN),
                    data.getLong(TraverseVisitTrace.CONTACT_ID));
            // Traverse visit trace
            return this.service.getSharingManager().traverseVisitTrace(contact, data.getString(TraverseVisitTrace.SHARING_CODE));
        }

        Logger.i(Notifier.class, "Unknown notifier action: " + action);
        return null;
    }
}
