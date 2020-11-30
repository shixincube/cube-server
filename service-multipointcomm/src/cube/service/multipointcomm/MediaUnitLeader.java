/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cube.service.multipointcomm;

import cell.adapter.CelletAdapter;
import cell.api.Speakable;
import cell.api.TalkListener;
import cell.api.TalkService;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkError;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.ModuleEvent;
import cube.common.Packet;
import cube.common.entity.CommField;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.service.multipointcomm.signaling.Signaling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 媒体单元的主机。
 */
public class MediaUnitLeader implements TalkListener, MediaUnitListener {

    private final static String CELLET_NAME = "MediaUnit";

    private TalkService talkService;

    private CelletAdapter contactsAdapter;

    private List<MediaUnit> mediaUnitList;

    private HashMap<Speakable, MediaUnit> speakableMap;

    private ConcurrentHashMap<Long, MediaUnitBundle> bundles;

    private ConcurrentHashMap<Long, MediaUnit> processingMap;

    /**
     * 构造函数。
     */
    public MediaUnitLeader() {
        this.mediaUnitList = new ArrayList<>();
        this.speakableMap = new HashMap<>();
        this.bundles = new ConcurrentHashMap<>();
        this.processingMap = new ConcurrentHashMap<>();
    }

    /**
     * 启动。
     *
     * @param talkService
     */
    public void start(TalkService talkService, CelletAdapter contactsAdapter) {
        this.talkService = talkService;
        this.talkService.setListener(CELLET_NAME, this);

        this.contactsAdapter = contactsAdapter;

        for (MediaUnit mediaUnit : this.mediaUnitList) {
            mediaUnit.speaker = this.talkService.call(mediaUnit.address, mediaUnit.port);
            this.speakableMap.put(mediaUnit.speaker, mediaUnit);
        }
    }

    /**
     * 停止。
     */
    public void stop() {
        this.talkService.removeListener(CELLET_NAME);

        for (MediaUnit mediaUnit : this.mediaUnitList) {
            this.talkService.hangup(mediaUnit.address, mediaUnit.port, false);
        }
    }

    /**
     * 向 Media Unit 分发信令。
     *
     * @param commField
     * @param signaling
     */
    public void dispatch(CommField commField, Signaling signaling) {
        this.dispatch(commField, signaling, null);
    }

    /**
     * 向 Media Unit 分发信令。
     *
     * @param commField
     * @param signaling
     * @param signalingCallback
     */
    public void dispatch(CommField commField, Signaling signaling, SignalingCallback signalingCallback) {
        // 选择媒体单元
        MediaUnit mediaUnit = this.selectMediaUnit(commField);

        // 向媒体单元发送信令
        this.sendSignaling(mediaUnit, signaling, signalingCallback);
    }

    /**
     * 选择媒体单元节点。
     *
     * @param commField
     * @return
     */
    private MediaUnit selectMediaUnit(CommField commField) {
        MediaUnitBundle bundle = this.bundles.get(commField.getId());
        if (null == bundle) {
            // 随机媒体单元，需要通过服务能力进行选择
            int index = Utils.randomInt(0, this.mediaUnitList.size() - 1);
            MediaUnit mediaUnit = this.mediaUnitList.get(index);
            bundle = new MediaUnitBundle(mediaUnit, commField);
            this.bundles.put(commField.getId(), bundle);
        }

        return bundle.mediaUnit;
    }

    /**
     * 发送信令给媒体单元。
     *
     * @param mediaUnit
     * @param signaling
     * @param signalingCallback
     */
    private void sendSignaling(MediaUnit mediaUnit, Signaling signaling, SignalingCallback signalingCallback) {
        Packet packet = new Packet(MediaUnitAction.Signaling.name, signaling.toJSON());
        if (mediaUnit.transmit(packet, signaling, signalingCallback)) {
            this.processingMap.put(packet.sn, mediaUnit);
        }
    }

    protected void readConfig(Properties properties) {
        // 读取 Unit 配置
        for (int i = 1; i <= 50; ++i) {
            String keyAddress = "unit." + i + ".address";
            if (properties.containsKey(keyAddress)) {
                String keyPort = "unit." + i + ".port";
                String address = properties.getProperty(keyAddress);
                int port = Integer.parseInt(properties.getProperty(keyPort, "7777"));

                MediaUnit unit = new MediaUnit(address, port, this);
                this.mediaUnitList.add(unit);
            }
        }
    }

    @Override
    public Signaling onSignaling(Signaling signaling) {
        Contact contact = signaling.getContact();

        // 将信令推送到集群
        ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
                signaling.getName(), signaling.toJSON());
        this.contactsAdapter.publish(contact.getUniqueKey(), event.toJSON());

        return signaling;
    }

    @Override
    public void onListened(Speakable speaker, String cellet, Primitive primitive) {
        ActionDialect actionDialect = new ActionDialect(primitive);
        Packet packet = new Packet(actionDialect);
        MediaUnit unit = this.processingMap.remove(packet.sn);
        if (null == unit) {
            unit = this.speakableMap.get(speaker);
        }

        if (null != unit) {
            unit.receive(packet);
        }
        else {
            Logger.e(this.getClass(), "Can NOT find media unit: " + speaker.getRemoteAddress().getHostString());
        }
    }

    @Override
    public void onListened(Speakable speaker, String cellet, PrimitiveInputStream primitiveInputStream) {
        // Nothing
    }

    @Override
    public void onSpoke(Speakable speaker, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onAck(Speakable speaker, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onSpeakTimeout(Speakable speaker, String cellet, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onContacted(Speakable speaker) {
        // Nothing
    }

    @Override
    public void onQuitted(Speakable speaker) {
        // Nothing
    }

    @Override
    public void onFailed(Speakable speaker, TalkError talkError) {
        // Nothing
    }
}
