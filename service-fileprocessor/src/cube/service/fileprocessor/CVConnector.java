/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.fileprocessor;

import cell.api.Speakable;
import cell.api.TalkListener;
import cell.api.TalkService;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.PrimitiveOutputStream;
import cell.core.talk.TalkError;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.Utils;
import cube.common.action.FileProcessorAction;
import cube.common.state.FileProcessorStateCode;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 计算机视觉系统连接器。
 */
public class CVConnector implements TalkListener {

    private String celletName;

    private TalkService talkService;

    private String host;

    private int port;

    private Timer retryTimer;

    private ConcurrentHashMap<Long, Block> blockMap;

    public CVConnector(String celletName, TalkService talkService) {
        this.celletName = celletName;
        this.talkService = talkService;
        this.blockMap = new ConcurrentHashMap<>();
    }

    public void start(String host, int port) {
        this.host = host;
        this.port = port;

        this.talkService.setListener(this.celletName, this);
        this.talkService.call(host, port);
    }

    public void stop() {
        if (null != this.retryTimer) {
            this.retryTimer.cancel();
            this.retryTimer = null;
        }

        this.talkService.hangup(this.host, this.port, false);
        this.talkService.removeListener(this.celletName);
    }

    /**
     * 校验是否是本地文件，如果是不进行传输。
     *
     * @param file
     * @return
     */
    private boolean checkLocalFile(File file) {
        if (this.host.equals("127.0.0.1") || this.host.equals("localhost")) {
            if (file.exists()) {
                return true;
            }
        }

        return false;
    }

    public void detectObjects(File file, CVCallback callback) {
        if (!this.talkService.isCalled(this.host, this.port)) {
            callback.handleFailure(FileProcessorStateCode.NoCVConnection, new CVResult(file.getName()));
            return;
        }

        long sn = Utils.generateSerialNumber();
        ActionDialect dialect = new ActionDialect(FileProcessorAction.DetectObject.name);
        dialect.addParam("sn", sn);
        dialect.addParam("file", file.getName());

        if (this.checkLocalFile(file)) {
            // 文件在本地，不进行传输
            dialect.addParam("fullpath", file.getAbsolutePath());
        }
        else {
            // 将文件数据发送给服务器
            FileInputStream fis = null;
            PrimitiveOutputStream stream = this.talkService.speakStream(this.celletName, file.getName());

            try {
                fis = new FileInputStream(file);
                int length = 0;
                byte[] buf = new byte[4096];
                while ((length = fis.read(buf)) > 0) {
                    stream.write(buf, 0, length);
                }
                stream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }

                if (null != stream) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        // 发送请求
        this.talkService.speak(this.celletName, dialect);

        Block block = new Block(sn, file.getName());
        this.blockMap.put(sn, block);

        synchronized (block) {
            try {
                block.wait(30 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 应答的数据
        ActionDialect response = block.response;

        this.blockMap.remove(sn);

        if (null == response) {
            callback.handleFailure(FileProcessorStateCode.Failure, new CVResult(file.getName()));
            return;
        }

        // 应答状态码
        int code = response.getParamAsInt("code");

        if (code != FileProcessorStateCode.Ok.code) {
            callback.handleFailure(FileProcessorStateCode.parse(code), new CVResult(file.getName()));
            return;
        }

        JSONObject data = response.getParamAsJson("data");

        CVResult result = new CVResult(file.getName());
        result.setDetectedObjects(data.getJSONArray("list"));
        callback.handleSuccess(result);
    }

    @Override
    public void onListened(Speakable speakable, String cellet, Primitive primitive) {
        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (FileProcessorAction.DetectObjectAck.name.equals(action)) {
            Long sn = dialect.getParamAsLong("sn");
            Block block = this.blockMap.remove(sn);
            if (null != block) {
                block.response = dialect;
                synchronized (block) {
                    block.notify();
                }
            }
        }
    }

    @Override
    public void onListened(Speakable speakable, String cellet, PrimitiveInputStream primitiveInputStream) {

    }

    @Override
    public void onSpoke(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onAck(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onSpeakTimeout(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onContacted(Speakable speakable) {

    }

    @Override
    public void onQuitted(Speakable speakable) {

    }

    @Override
    public void onFailed(Speakable speakable, TalkError talkError) {
        (new Thread() {
            @Override
            public void run() {
                talkService.hangup(host, port, false);
            }
        }).start();

        if (null != this.retryTimer) {
            this.retryTimer.cancel();
        }

        this.retryTimer = new Timer();
        this.retryTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                retryTimer = null;
                talkService.call(host, port);
            }
        }, 45 * 1000);
    }

    /**
     * 阻塞块。
     */
    protected class Block {

        protected Long sn;

        protected String fileCode;

        protected ActionDialect response;

        protected Block(Long sn, String fileCode) {
            this.sn = sn;
            this.fileCode = fileCode;
        }
    }
}
