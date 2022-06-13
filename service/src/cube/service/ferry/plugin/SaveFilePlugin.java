/*
 * This source file is part of Cube.
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2020-2022 Cube Team.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.ferry.plugin;

import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.common.entity.FileLabel;
import cube.core.AbstractModule;
import cube.ferry.FerryAction;
import cube.ferry.FerryPacket;
import cube.ferry.FerryPort;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.ferry.FerryService;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 保存文件插件。
 */
public class SaveFilePlugin implements Plugin {

    private FerryService service;

    public SaveFilePlugin(FerryService service) {
        this.service = service;
    }

    @Override
    public void setup() {
    }

    @Override
    public void teardown() {
    }

    @Override
    public void onAction(PluginContext context) {
        FileLabel fileLabel = (FileLabel) context.get("fileLabel");
        if (null != fileLabel) {
            if (!this.service.isOnlineDomain(fileLabel.getDomain().getName())) {
                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "Domain is offline: " + fileLabel.getDomain().getName());
                }
                return;
            }

            ActionDialect actionDialect = new ActionDialect(FerryAction.Ferry.name);
            actionDialect.addParam("port", FerryPort.SaveFile);
            actionDialect.addParam("fileLabel", fileLabel.toJSON());

            this.service.pushToBoat(fileLabel.getDomain().getName(), new FerryPacket(actionDialect));

            // 传输文件流
            this.transmitFileStream(fileLabel);
        }
    }

    private void transmitFileStream(FileLabel fileLabel) {
        // 从文件存储器加载文件
        AbstractModule fileStorageModule = this.service.getKernel().getModule("FileStorage");
        JSONObject data = new JSONObject();
        data.put("action", "loadFile");
        data.put("domain", fileLabel.getDomain().getName());
        data.put("fileCode", fileLabel.getFileCode());
        String fullPath = (String) fileStorageModule.notify(data);
        if (null == fullPath) {
            Logger.e(this.getClass(), "#transmitFileStream - Load file error from file storage: "
                    + fileLabel.getFileCode());
            return;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(fullPath));
        } catch (IOException e) {
            Logger.e(this.getClass(), "#transmitFileStream - Create file stream error: " + fullPath);
            return;
        }

        // 向 Boat 发送流
        this.service.pushToBoat(fileLabel.getDomain().getName(), fileLabel.getFileCode(), fis);
    }
}
