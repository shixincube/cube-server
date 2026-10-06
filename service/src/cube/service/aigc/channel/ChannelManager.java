/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.channel;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.auth.AuthToken;
import cube.common.Language;
import cube.common.entity.AIGCChannel;
import cube.service.aigc.AIGCCellet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 频道管理器。
 *
 * <p>频道（{@link AIGCChannel}）是一次会话的上下文：它承载参与方、语言、处理状态与最近应答记录。
 * 本类集中管理频道的<b>创建、查询、保活与回收</b>，是频道表的唯一持有者。</p>
 *
 * <p><b>凭证索引</b>：除频道码索引外，本类还维护「访问令牌 → 频道码」索引
 * （{@link #channelTokenMap}），使按凭证查询频道从 O(n) 全表扫描降为一次哈希查找。
 * 同一凭证若存在多个频道（历史遗留的频道泄漏行为所致），索引指向最近创建的那个，
 * 即当前实际在用的频道；过期清理时用 {@code remove(key, value)} 双参数形式删除，
 * 避免误删同一凭证下后创建的频道映射。</p>
 *
 * <p><b>与会话无关的边界</b>：本类不关心频道里跑的是什么任务。中断正在执行的单元元任务
 * 通过 {@link AIGCCellet#interrupt(long)} 发出，单元侧负责回告失败，从而保证调用方
 * 一定收到响应（既不返回结果也不返回失败的分支会在调用方造成永久等待）。</p>
 */
public class ChannelManager {

    /**
     * 最大频道数量。
     */
    public static final int MAX_CHANNEL = 10000;

    /**
     * 频道空闲超时：30 分钟。
     */
    public static final long CHANNEL_TIMEOUT = 30 * 60 * 1000;

    /**
     * 频道处理状态的兜底复位周期：5 分钟。
     *
     * <p>用于回收「处理标志因异常路径未被清除」的频道，避免该频道永久拒绝新请求。</p>
     */
    private static final long PROCESSING_RESET_PERIOD = 5 * 60 * 1000;

    /**
     * 频道表。Key 是频道码。
     */
    private final ConcurrentHashMap<String, AIGCChannel> channelMap;

    /**
     * 访问令牌 → 频道码 索引。
     */
    private final ConcurrentHashMap<String, String> channelTokenMap;

    /**
     * 用于中断频道上正在执行的单元元任务。
     */
    private final AIGCCellet cellet;

    /**
     * 访问令牌解析器：把令牌码解析为 {@link AuthToken}，解析失败返回 {@code null}。
     */
    private final Function<String, AuthToken> tokenResolver;

    /**
     * 构造频道管理器。
     *
     * @param cellet        单元容器，用于中断频道上的执行中任务。
     * @param tokenResolver 访问令牌解析器。
     */
    public ChannelManager(AIGCCellet cellet, Function<String, AuthToken> tokenResolver) {
        this.cellet = cellet;
        this.tokenResolver = tokenResolver;
        this.channelMap = new ConcurrentHashMap<>();
        this.channelTokenMap = new ConcurrentHashMap<>();
    }

    /**
     * 按频道码获取频道。
     *
     * @param channelCode 频道码。
     * @return 不存在返回 {@code null}。
     */
    public AIGCChannel get(String channelCode) {
        // ConcurrentHashMap 不接受 null 键查询
        if (null == channelCode) {
            return null;
        }
        return this.channelMap.get(channelCode);
    }

    /**
     * 通过访问令牌获取对应的频道。
     *
     * @param tokenCode 访问令牌码。
     * @return 不存在返回 {@code null}。
     */
    public AIGCChannel getByToken(String tokenCode) {
        // ConcurrentHashMap 不接受 null 键查询
        if (null == tokenCode) {
            return null;
        }
        // 走凭证索引，避免对 channelMap 做全表扫描（上限 10000 时是热路径成本）
        String channelCode = this.channelTokenMap.get(tokenCode);
        return (null != channelCode) ? this.channelMap.get(channelCode) : null;
    }

    /**
     * 通过访问令牌获取频道，必要时创建。
     *
     * @param authToken      访问令牌。
     * @param createIfAbsent 为 {@code true} 时在频道不存在的情况下创建并登记频道。
     * @return 返回频道，未找到且不创建时返回 {@code null}。
     */
    public AIGCChannel get(AuthToken authToken, boolean createIfAbsent) {
        AIGCChannel channel = this.getByToken(authToken.getCode());
        if (null != channel) {
            return channel;
        }

        if (createIfAbsent) {
            channel = new AIGCChannel(authToken, "User-" + authToken.getContactId());
            this.index(channel);
            return channel;
        }

        return null;
    }

    /**
     * 获取当前所有频道的快照。
     *
     * @return 返回频道列表。
     */
    public List<AIGCChannel> getAll() {
        return new ArrayList<>(this.channelMap.values());
    }

    /**
     * 返回当前频道数量。
     *
     * @return 返回数量。
     */
    public int size() {
        return this.channelMap.size();
    }

    /**
     * 创建频道。
     *
     * @param token       访问令牌码。
     * @param participant 参与方名称。
     * @param channelCode 频道码。
     * @param language    语言。
     * @return 令牌无效时返回 {@code null}。
     */
    public AIGCChannel create(String token, String participant, String channelCode, Language language) {
        AuthToken authToken = this.tokenResolver.apply(token);
        if (null == authToken) {
            return null;
        }

        return this.create(authToken, participant, channelCode, language);
    }

    /**
     * 创建频道。
     *
     * @param authToken   访问令牌。
     * @param participant 参与方名称。
     * @param channelCode 频道码。
     * @param language    语言。
     * @return 返回新创建的频道。
     */
    public AIGCChannel create(AuthToken authToken, String participant, String channelCode, Language language) {
        // 频道码为键，ConcurrentHashMap 不接受 null 键：为空时代生成随机码
        String code = (null == channelCode || channelCode.isEmpty()) ? Utils.randomString(16) : channelCode;
        AIGCChannel channel = new AIGCChannel(authToken, participant, code, language);
        this.index(channel);
        return channel;
    }

    /**
     * 申请频道。
     *
     * <p>拒绝条件：频道数达到上限、参与方名称命中敏感词、或<b>同一凭证的最新频道仍在处理中</b>。</p>
     *
     * @param token       访问令牌码。
     * @param participant 参与方名称。
     * @return 被拒绝时返回 {@code null}。
     */
    public AIGCChannel request(String token, String participant) {
        // null 参与方会让 checkParticipantName 抛 NPE，null 令牌无法解析，一并拒绝
        if (null == token || null == participant) {
            Logger.w(ChannelManager.class, "#request - Token or participant is NULL");
            return null;
        }

        if (this.channelMap.size() >= MAX_CHANNEL) {
            Logger.w(ChannelManager.class, "#request - Channel num overflow: " + MAX_CHANNEL);
            return null;
        }

        if (!checkParticipantName(participant)) {
            Logger.w(ChannelManager.class, "#request - Participant is sensitive word: " + participant);
            return null;
        }

        // 走凭证索引判断「当前频道是否还在工作状态」，避免全表扫描
        AIGCChannel processing = this.getByToken(token);
        if (null != processing && processing.isProcessing()) {
            Logger.w(ChannelManager.class, "#request - Channel is processing: " + processing.getCode());
            return null;
        }

        AuthToken authToken = this.tokenResolver.apply(token);

        AIGCChannel channel = new AIGCChannel(authToken, participant);
        this.index(channel);
        return channel;
    }

    /**
     * 停止频道正在进行的操作。
     *
     * @param channelCode 指定频道码。
     * @return 返回频道，不存在时返回 {@code null}。
     */
    public AIGCChannel stopProcessing(String channelCode) {
        AIGCChannel channel = this.get(channelCode);
        if (null == channel) {
            return null;
        }

        if (channel.isProcessing()) {
            // 说明：早期这里预留了「任务仍在队列中未被执行时直接出队」的分支（原 TODO XJW），
            // 但那样的处理会让调用方永远收不到应答（既不返回结果也不返回失败）。
            // 现在文本生成统一走任务执行器的生成队列，仍然保持「一律按 sn 中断」，
            // 由单元侧回告失败，从而保证调用方一定得到响应。
            this.cellet.interrupt(channel.getLastUnitMetaSn());
        }

        return channel;
    }

    /**
     * 保活频道：刷新频道活跃时间戳，使其免于空闲超时回收。
     *
     * <p>注意参数语义与历史实现保持一致：这里按<b>频道码</b>查找频道。</p>
     *
     * @param channelCode 频道码。
     * @return 频道不存在返回 {@code false}。
     */
    public boolean keepAlive(String channelCode) {
        AIGCChannel channel = this.channelMap.get(channelCode);
        if (null == channel) {
            return false;
        }

        channel.setActiveTimestamp(System.currentTimeMillis());
        return true;
    }

    /**
     * 周期维护：复位长期未变化的处理标志，并回收空闲超时的频道。
     *
     * @param now 当前时刻。
     */
    public void onTick(long now) {
        Iterator<AIGCChannel> iter = this.channelMap.values().iterator();
        while (iter.hasNext()) {
            AIGCChannel channel = iter.next();
            if (now - channel.getProcessingTimestamp() >= PROCESSING_RESET_PERIOD) {
                // 重置状态
                channel.setProcessing(false);
            }

            if (now - channel.getActiveTimestamp() >= CHANNEL_TIMEOUT) {
                iter.remove();

                // 同步清理凭证索引。仅当索引仍指向本频道时才删除，
                // 避免误删同一凭证下后创建的频道映射。
                if (null != channel.getAuthToken()) {
                    this.channelTokenMap.remove(channel.getAuthToken().getCode(), channel.getCode());
                }
            }
        }
    }

    /**
     * 清空频道索引。
     */
    public void clear() {
        this.channelTokenMap.clear();
    }

    /**
     * 登记频道并维护「访问令牌 → 频道码」索引。
     *
     * @param channel 频道。
     */
    private void index(AIGCChannel channel) {
        this.channelMap.put(channel.getCode(), channel);
        // 令牌码同为哈希键，null / 空串一律跳过索引（频道本身仍可按码访问）
        if (null != channel.getAuthToken() && null != channel.getAuthToken().getCode()) {
            this.channelTokenMap.put(channel.getAuthToken().getCode(), channel.getCode());
        }
    }

    /**
     * 校验参与方名称。
     *
     * @param name 参与方名称。
     * @return 允许使用返回 {@code true}。
     */
    private static boolean checkParticipantName(String name) {
        if (name.equalsIgnoreCase("AIGC") || name.equalsIgnoreCase("Cube") ||
                name.equalsIgnoreCase("Baize") || name.contains("白泽")) {
            return false;
        } else {
            return true;
        }
    }
}
