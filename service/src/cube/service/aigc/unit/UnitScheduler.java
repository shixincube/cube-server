/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cell.core.talk.TalkContext;
import cell.util.log.Logger;
import cube.common.entity.AICapability;
import cube.common.entity.AIGCUnit;
import cube.common.entity.Contact;
import cube.service.aigc.event.EventCenter;
import cube.service.aigc.resource.Relay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单元调度器。
 *
 * <p>负责 AI 单元的<b>注册</b>（{@link #setup}、{@link #teardown}）与<b>选点</b>
 * （{@link #select}、{@link #selectIdle}、{@link #selectBySubtask}），以及单元的周期性维护
 * （{@link #onTick(long)}）。</p>
 *
 * <p>单元（{@link AIGCUnit}）是模型推理的执行载体：一个「联系人 + 能力」对应一个单元，
 * 单元自己维护运行状态与最近执行时间戳。本类只负责回答「这次请求该用哪个单元」，
 * 不参与任务的入队与执行——那是 {@link AIGCTaskExecutor} 的职责。</p>
 *
 * <p>选点方法保持 {@code synchronized}：选点与并发派发之间存在「先选到空闲单元、后入队」
 * 的时间窗，串行化选点可以让多个派发方看到一致的单元占用视图。</p>
 */
public class UnitScheduler {

    /**
     * 单元表。
     * Key 是单元查询键（{@link AIGCUnit#makeQueryKey(Contact, AICapability)}）。
     */
    private final Map<String, AIGCUnit> unitMap = new ConcurrentHashMap<>();

    /**
     * 单元权重。
     * Key 是 Contact Id。
     */
    private final Map<Long, Double> unitWeightMap = new ConcurrentHashMap<>();

    /**
     * 上一次重置单元运行状态的时刻。
     */
    private long lastResetUnitTime = 0;

    /**
     * 单元运行状态重置周期：10 分钟。
     *
     * <p>单元的运行标志带有「注册后一段时间内视为运行中」的语义，长期存活的服务里可能出现
     * 标志残留，因此周期性统一复位。</p>
     */
    private static final long RESET_RUNNING_PERIOD = 10 * 60 * 1000;

    /**
     * 单元选取顺序：按「最近执行时间戳」从低到高，即优先选择最久没有被执行过的单元。
     *
     * <p>使用 {@link Long#compare} 而非差值强转 int，避免时间戳差超过 int 表示范围时排序反转。</p>
     */
    private static final Comparator<AIGCUnit> IDLEST_FIRST = new Comparator<AIGCUnit>() {
        @Override
        public int compare(AIGCUnit u1, AIGCUnit u2) {
            return Long.compare(u1.getLastRunningTimestamp(), u2.getLastRunningTimestamp());
        }
    };

    public UnitScheduler() {
    }

    /**
     * 设置指定联系人的单元权重。
     *
     * @param contactId 联系人 ID。
     * @param weight    权重。
     */
    public void setWeight(long contactId, double weight) {
        this.unitWeightMap.put(contactId, weight);
    }

    /**
     * 用中继（Relay）提供的单元填充单元表。仅用于中继模式的本地测试。
     *
     * @param relay 中继实例。
     */
    public void fillFromRelay(Relay relay) {
        relay.fillUnits(this.unitMap);
    }

    /**
     * 注册单元。已存在的单元仅更新其通信上下文。
     *
     * @param contact      联系人。
     * @param capabilities 能力列表。
     * @param context      通信上下文。
     * @return 返回本次涉及的单元列表。
     */
    public List<AIGCUnit> setup(Contact contact, List<AICapability> capabilities, TalkContext context) {
        List<AIGCUnit> result = new ArrayList<>(capabilities.size());

        for (AICapability capability : capabilities) {
            String key = AIGCUnit.makeQueryKey(contact, capability);
            AIGCUnit unit = this.unitMap.get(key);
            if (null != unit) {
                unit.setContext(context);
            }
            else {
                unit = new AIGCUnit(contact, capability, context);
                this.unitMap.put(key, unit);
            }

            Double weight = this.unitWeightMap.get(contact.getId());
            if (null != weight) {
                unit.setWeight(weight);
                Logger.d(this.getClass(), "#setup - Modify unit \"" +
                        unit.getCapability().getName() + "\" weight : " + weight);
            }

            result.add(unit);
        }

        return result;
    }

    /**
     * 注销指定联系人的所有单元。
     *
     * @param contact 联系人。
     * @return 返回被注销的单元列表。
     */
    public List<AIGCUnit> teardown(Contact contact) {
        List<AIGCUnit> result = new ArrayList<>();

        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getContact().getId().equals(contact.getId())) {
                result.add(unit);
                iter.remove();
                // 从事件中心移除
                EventCenter.getInstance().removeUnitMeta(unit);
            }
        }

        return result;
    }

    /**
     * 获取当前所有单元的快照。
     *
     * @return 返回单元列表。
     */
    public List<AIGCUnit> getAll() {
        List<AIGCUnit> list = new ArrayList<>(this.unitMap.size());
        list.addAll(this.unitMap.values());
        return list;
    }

    /**
     * 统计指定能力名下的有效单元数量。
     *
     * @param unitName 单元能力名。
     * @return 返回数量。
     */
    public int numUnitsByName(String unitName) {
        int num = 0;
        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().getName().equalsIgnoreCase(unitName)
                    && unit.getContext().isValid()) {
                ++num;
            }
        }
        return num;
    }

    /**
     * 判断是否存在指定能力名的单元，不区分单元是否有效。
     *
     * @param unitName 单元能力名。
     * @return 存在返回 {@code true}。
     */
    public boolean hasUnit(String unitName) {
        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            if (iter.next().getCapability().getName().equals(unitName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 选择空闲的单元，如果没有空闲单元返回 <code>null</code> 值。
     *
     * @param unitName 单元能力名。
     * @return 找不到空闲单元时返回 {@code null}。
     */
    public synchronized AIGCUnit selectIdle(String unitName) {
        ArrayList<AIGCUnit> candidates = new ArrayList<>();

        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().getName().equalsIgnoreCase(unitName)
                    && unit.getContext().isValid()
                    && !unit.isRunning()) {
                // 检查是否正在处理流
                MultimodalUnitMeta meta = EventCenter.getInstance().searchMultimodalUnitMeta(unit);
                if (null != meta) {
                    // 正在处理流
                    continue;
                }
                candidates.add(unit);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        AIGCUnit unit = selectIdlest(candidates);

        Logger.d(this.getClass(), "#selectIdle - Unit: " + unitName + "@"
                + unit.getContact().getId());
        return unit;
    }

    /**
     * 按能力名选择单元。优先返回空闲单元，没有空闲单元时返回最久未执行的单元。
     *
     * @param unitName 单元能力名。
     * @return 无可用单元时返回 {@code null}。
     */
    public synchronized AIGCUnit select(String unitName) {
        AIGCUnit idleUnit = this.selectIdle(unitName);
        if (null != idleUnit) {
            return idleUnit;
        }

        ArrayList<AIGCUnit> candidates = new ArrayList<>();

        // 选择所有可用节点
        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().getName().equalsIgnoreCase(unitName) &&
                    unit.getContext().isValid()) {
                candidates.add(unit);
            }
        }

        // 无候选节点
        if (candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() == 1) {
            Logger.d(this.getClass(), "#select - Unit: " + unitName + "@"
                    + candidates.get(0).getContact().getId());
            return candidates.get(0);
        }

        AIGCUnit unit = selectIdlest(candidates);

        Logger.d(this.getClass(), "#select - Unit: " + unitName + "@"
                + unit.getContact().getId());
        return unit;
    }

    /**
     * 按能力名与联系人 ID 选择单元。
     *
     * <p>联系人 ID 在 10 位以内（≤ 9999999999）时优先在「权重 &gt; 5.0」的单元内选择，
     * 该规则用于把高优先级用户调度到专用单元；没有符合条件的单元时退化为一般选择。</p>
     *
     * @param unitName 单元能力名。
     * @param cid      联系人 ID。
     * @return 无可用单元时返回 {@code null}。
     */
    public synchronized AIGCUnit select(String unitName, long cid) {
        if (cid > 9999999999L) {
            // 10位以上ID进行一般选择
            return this.select(unitName);
        }

        ArrayList<AIGCUnit> candidates = new ArrayList<>();

        // 选择所有权重大于5.0的可用节点
        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().getName().equals(unitName) &&
                    unit.getContext().isValid() &&
                    unit.getWeight() > 5.0) {
                candidates.add(unit);
            }
        }

        // 无候选节点
        if (candidates.isEmpty()) {
            // 进行一般选择
            return this.select(unitName);
        }

        if (candidates.size() == 1) {
            Logger.d(this.getClass(), "#select - Unit: " + unitName + "@"
                    + candidates.get(0).getContact().getId());
            return candidates.get(0);
        }

        AIGCUnit unit = selectIdlest(candidates);

        Logger.d(this.getClass(), "#select - Unit: " + unitName + "@"
                + unit.getContact().getId());
        return unit;
    }

    /**
     * 按子任务名选择单元。
     *
     * @param subtask 子任务名。
     * @return 无可用单元时返回 {@code null}。
     */
    public AIGCUnit selectBySubtask(String subtask) {
        ArrayList<AIGCUnit> candidates = new ArrayList<>();

        Iterator<AIGCUnit> iter = this.unitMap.values().iterator();
        while (iter.hasNext()) {
            AIGCUnit unit = iter.next();
            if (unit.getCapability().containsSubtask(subtask) && unit.getContext().isValid()) {
                candidates.add(unit);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        int num = candidates.size();
        if (num == 1) {
            Logger.d(this.getClass(), "#selectBySubtask - Unit: " +
                    candidates.get(0).getCapability().getName() + "@" + candidates.get(0).getContact().getId());
            return candidates.get(0);
        }

        AIGCUnit unit = selectIdlest(candidates);

        Logger.d(this.getClass(), "#selectBySubtask - Unit: " +
                unit.getCapability().getName() + "@" + unit.getContact().getId());
        return unit;
    }

    /**
     * 周期维护：清理通信上下文已失效的单元，并周期性重置单元运行标志。
     *
     * @param now 当前时刻。
     */
    public void onTick(long now) {
        Iterator<AIGCUnit> unitIter = this.unitMap.values().iterator();
        while (unitIter.hasNext()) {
            AIGCUnit unit = unitIter.next();
            if (null != unit.getContext() && !unit.getContext().isValid()) {
                // 已失效
                unitIter.remove();
                EventCenter.getInstance().removeUnitMeta(unit);
            }
        }

        if (now - this.lastResetUnitTime > RESET_RUNNING_PERIOD) {
            this.lastResetUnitTime = now;
            unitIter = this.unitMap.values().iterator();
            while (unitIter.hasNext()) {
                unitIter.next().resetRunning();
            }
        }
    }

    /**
     * 从候选单元中选出本轮要使用的单元。
     *
     * <p>选择规则（原先在四处选点方法里各写一遍）：先按最久未执行排序并记住首个，
     * 然后剔除正在运行的单元；若剔除后仍有候选则取其中最早的，全部在忙时回退到首个——
     * 也就是「全部单元都在运行」时仍然派发，把并发降级留给上层处理。</p>
     *
     * @param candidates 候选单元，非空。方法内部会按选取顺序重排。
     * @return 返回选中的单元。
     */
    private static AIGCUnit selectIdlest(List<AIGCUnit> candidates) {
        candidates.sort(IDLEST_FIRST);

        // 先取一次，选择最久没有执行的
        AIGCUnit idlest = candidates.get(0);

        Iterator<AIGCUnit> iter = candidates.iterator();
        while (iter.hasNext()) {
            if (iter.next().isRunning()) {
                // 把正在运行的单元从候选列表里删除
                iter.remove();
            }
        }

        // 全部在忙时回退到最久未执行的那个
        return candidates.isEmpty() ? idlest : candidates.get(0);
    }
}
