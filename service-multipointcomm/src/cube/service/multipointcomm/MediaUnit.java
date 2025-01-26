/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.common.state.MultipointCommStateCode;
import org.json.JSONObject;

import java.util.Collection;

/**
 * 媒体单元。
 */
public interface MediaUnit {

    /**
     * 订阅指定目标的数据流。
     *
     * @param sn
     * @param commField
     * @param endpoint
     * @param target
     * @param offerSDP
     * @param callback
     * @return
     */
    public MultipointCommStateCode subscribe(Long sn, CommField commField, CommFieldEndpoint endpoint,
            CommFieldEndpoint target, String offerSDP, MediaUnitCallback callback);

    /**
     * 取消定于指定目标的数据流。
     *
     * @param commField
     * @param endpoint
     * @param target
     * @param callback
     * @return
     */
    public MultipointCommStateCode unsubscribe(CommField commField, CommFieldEndpoint endpoint,
            CommFieldEndpoint target, MediaUnitCallback callback);

    /**
     * 订阅指定场域上的输出端口的数据流。
     *
     * @param sn
     * @param commField
     * @param endpoint
     * @param offerSDP
     * @param callback
     * @return
     */
    public MultipointCommStateCode subscribe(Long sn, CommField commField, CommFieldEndpoint endpoint,
            String offerSDP, MediaUnitCallback callback);

    /**
     * 向指定目标添加 ICE candidate 数据。
     *
     * @param commField
     * @param endpoint
     * @param target
     * @param candidate
     * @return
     */
    public MultipointCommStateCode addCandidate(CommField commField, CommFieldEndpoint endpoint,
            CommFieldEndpoint target, JSONObject candidate);

    /**
     * 向指定 SN 标记的会话添加 ICE candidate 数据。
     *
     * @param sn
     * @param commField
     * @param endpoint
     * @param candidate
     * @return
     */
    public MultipointCommStateCode addCandidate(Long sn, CommField commField, CommFieldEndpoint endpoint,
            JSONObject candidate);

    /**
     * 移除指定的终端的所有数据流。
     *
     * @param commField
     * @param endpoint
     * @return
     */
    public MultipointCommStateCode removeEndpoint(CommField commField, CommFieldEndpoint endpoint);

    /**
     * 释放通讯域的数据通道。
     *
     * @param commField
     * @return
     */
    public MultipointCommStateCode release(CommField commField);

    /**
     * 销毁当前媒体单元。
     */
    public void destroy();

    /**
     * 返回所有 MediaLobby 实例。
     *
     * @return
     */
    public Collection<? extends MediaLobby> getAllLobbies();
}
