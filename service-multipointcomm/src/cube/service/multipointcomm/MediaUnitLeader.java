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

import cell.api.TalkService;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.common.entity.CommField;
import cube.service.multipointcomm.signaling.Signaling;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 媒体单元的主机。
 */
public class MediaUnitLeader implements MediaUnitListener {

    private TalkService talkService;

    private List<AbstractMediaUnit> mediaUnitList;

    private ConcurrentHashMap<Long, MediaUnitBundle> bundles;

//    private HashMap<Speakable, MediaUnit> speakableMap;

//    private ConcurrentHashMap<Long, MediaUnit> processingMap;

    /**
     * 构造函数。
     */
    public MediaUnitLeader() {
        this.mediaUnitList = new ArrayList<>();
        this.bundles = new ConcurrentHashMap<>();

//        this.speakableMap = new HashMap<>();

//        this.processingMap = new ConcurrentHashMap<>();
    }

    /**
     * 启动。
     */
    public void start(Properties properties) {
        // 读取配置
        this.readConfig(properties);


//        this.talkService = talkService;
//        this.talkService.setListener(MediaUnit.CELLET_NAME, this);

//        this.contactsAdapter = contactsAdapter;

//        for (MediaUnit mediaUnit : this.mediaUnitList) {
//            mediaUnit.speaker = this.talkService.call(mediaUnit.address, mediaUnit.port);
//            this.speakableMap.put(mediaUnit.speaker, mediaUnit);
//        }
    }

    /**
     * 停止。
     */
    public void stop() {
        for (AbstractMediaUnit mu : this.mediaUnitList) {
            try {
                mu.destroy();
            } catch (Throwable e) {
                Logger.w(this.getClass(), "stop", e);
            }
        }

        this.mediaUnitList.clear();

//        this.talkService.removeListener(CELLET_NAME);

//        for (MediaUnit mediaUnit : this.mediaUnitList) {
//            this.talkService.hangup(mediaUnit.address, mediaUnit.port, false);
//        }
    }

    /**
     * 向 Media Unit 分发信令。
     *
     * @param commField
     * @param signaling
     */
    public void dispatch(CommField commField, Signaling signaling) {
    }

    /**
     * 向 Media Unit 分发信令。
     *
     * @param commField
     * @param signaling
     * @param signalingCallback
     */
    public void dispatch(CommField commField, Signaling signaling, SignalingCallback signalingCallback) {
    }

    /**
     * 选择媒体单元节点。
     *
     * @param commField
     * @return
     */
    private AbstractMediaUnit selectMediaUnit(CommField commField) {
        MediaUnitBundle bundle = this.bundles.get(commField.getId());
        if (null == bundle) {
            // 随机媒体单元，需要通过服务能力进行选择
            int index = Utils.randomInt(0, this.mediaUnitList.size() - 1);
            AbstractMediaUnit mediaUnit = this.mediaUnitList.get(index);
            bundle = new MediaUnitBundle(mediaUnit, commField);
            this.bundles.put(commField.getId(), bundle);
        }

        return bundle.mediaUnit;
    }

    /**
     * 发送信令给媒体单元。
     *
     */
//    private void sendSignaling(MediaUnit mediaUnit, Signaling signaling, SignalingCallback signalingCallback) {
//        Packet packet = new Packet(MediaUnitAction.Signaling.name, signaling.toJSON());
//        if (mediaUnit.transmit(packet, signaling, signalingCallback)) {
//            this.processingMap.put(packet.sn, mediaUnit);
//        }
//    }

    private void readConfig(Properties properties) {
        // 读取 Unit 配置
        for (int i = 1; i <= 50; ++i) {
            String keyUrl = "unit." + i + ".kms.url";
            if (properties.containsKey(keyUrl)) {
                KurentoMediaUnit kurentoMediaUnit = new KurentoMediaUnit(properties.getProperty(keyUrl));
                this.mediaUnitList.add(kurentoMediaUnit);
            }
        }
    }

    @Override
    public Signaling onSignaling(Signaling signaling) {
//        Contact contact = signaling.getContact();
//
//        // 将信令推送到集群
//        ModuleEvent event = new ModuleEvent(MultipointCommService.NAME,
//                signaling.getName(), signaling.toJSON());
//        this.contactsAdapter.publish(contact.getUniqueKey(), event.toJSON());

        return signaling;
    }
}
