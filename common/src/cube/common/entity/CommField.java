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

package cube.common.entity;

import cube.common.Domain;
import cube.common.UniqueKey;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 多方通讯场域。
 */
public class CommField extends Entity {

    /**
     * 默认超时时间。
     */
    private long defaultTimeout = 40L * 1000L;

    /**
     * 定时器超时时间。
     */
    private long timerTimeout = 45L * 1000L;

    /**
     * 名称，便于调试。
     */
    private final String name;

    /**
     * 创建人。
     */
    private Contact founder;

    /**
     * 媒体约束。
     */
    private MediaConstraint mediaConstraint;

    /**
     * 场域包含的终端节点。
     */
    private ConcurrentHashMap<Long, CommFieldEndpoint> endpoints;

    /**
     * 超时任务的 ScheduledFuture 映射。
     */
    private ConcurrentHashMap<Long, ScheduledFuture<?>> timeoutFutureMap;

    /**
     * 关联的群组。
     */
    private Group group;

    /**
     * 当前场域内的通话关系。仅用于私域。
     */
    private BoundCalling boundCalling;

    /**
     * 构造函数。
     *
     * @param id
     * @param domainName
     * @param founder
     */
    public CommField(Long id, String domainName, Contact founder) {
        super(id, domainName);

        this.name = founder.getName() + "#" + id;
        this.founder = founder;
        this.mediaConstraint = new MediaConstraint();
        this.endpoints = new ConcurrentHashMap<>();
    }

    /**
     * 构造函数。
     *
     * @param json
     */
    public CommField(JSONObject json) {
        super();

        this.endpoints = new ConcurrentHashMap<>();

        this.id = json.getLong("id");
        this.domain = new Domain(json.getString("domain"));
        this.name = json.getString("name");
        this.founder = new Contact(json.getJSONObject("founder"));
        this.mediaConstraint = new MediaConstraint((json.getJSONObject("mediaConstraint")));

        if (json.has("endpoints")) {
            JSONArray endpoints = json.getJSONArray("endpoints");

            for (int i = 0, size = endpoints.length(); i < size; ++i) {
                JSONObject cfeJson = endpoints.getJSONObject(i);
                CommFieldEndpoint cfe = new CommFieldEndpoint(cfeJson);
                this.endpoints.put(cfe.getId(), cfe);
            }
        }

        if (json.has("group")) {
            this.group = new Group(json.getJSONObject("group"));
        }

        if (json.has("caller") && json.has("callee")) {
            this.boundCalling = new BoundCalling(new Contact(json.getJSONObject("caller")),
                    new Contact(json.getJSONObject("callee")));
        }

        this.uniqueKey = UniqueKey.make(this.id, this.domain);
    }

    /**
     * 获取名称。
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * 是否是私有场域。
     *
     * @return
     */
    public boolean isPrivate() {
        return (this.id.longValue() == this.founder.getId().longValue());
    }

    /**
     * 返回创建人。
     *
     * @return
     */
    public Contact getFounder() {
        return this.founder;
    }

    /**
     * 判断指定联系人是否是主叫。
     *
     * @param contact
     * @return
     */
    public boolean isCaller(Contact contact) {
        if (null != this.boundCalling) {
            return this.boundCalling.caller.equals(contact);
        }

        return false;
    }

    /**
     * 判断指定联系人是否是被叫。
     *
     * @param contact
     * @return
     */
    public boolean isCallee(Contact contact) {
        if (null != this.boundCalling) {
            return this.boundCalling.callee.equals(contact);
        }

        return false;
    }

    /**
     * 复制私域数据。
     *
     * @param source
     */
    public void markSingleCalling(CommField source) {
        this.boundCalling = new BoundCalling(source.getCaller(), source.getCallee());
    }

    /**
     * 指定联系人是否正在场域内进行呼叫操作。
     *
     * @param contact
     * @return
     */
    public boolean isSingleCalling(Contact contact) {
        if (null == this.boundCalling) {
            return false;
        }

        if (this.boundCalling.caller.equals(contact)) {
            return true;
        }
        else if (this.boundCalling.callee.equals(contact)) {
            return true;
        }

        return false;
    }

    /**
     * 获取主叫
     * 仅用于私域下。
     *
     * @return
     */
    public Contact getCaller() {
        return (null != this.boundCalling) ? this.boundCalling.caller : null;
    }

    /**
     * 获取被叫。
     * 仅用于私域下。
     *
     * @return
     */
    public Contact getCallee() {
        return (null != this.boundCalling) ? this.boundCalling.callee : null;
    }

    /**
     * 清空所有呼叫状态。
     */
    public void clearCalling() {
        this.boundCalling = null;
    }

    /**
     * 获取群组。
     *
     * @return
     */
    public Group getGroup() {
        return this.group;
    }

    /**
     *
     * @return
     */
    public int numEndpoints() {
        return this.endpoints.size();
    }

    /**
     * 添加终端节点。
     *
     * @param endpoint
     */
    public void addEndpoint(CommFieldEndpoint endpoint) {
        this.endpoints.put(endpoint.getId(), endpoint);
    }

    /**
     * 移除终端节点。
     *
     * @param endpoint
     */
    public void removeEndpoint(CommFieldEndpoint endpoint) {
        this.endpoints.remove(endpoint.getId());
    }

    /**
     * 清空所有终端节点。
     */
    public void clearEndpoints() {
        this.endpoints.clear();
    }

    /**
     * 获取所有终端节点。
     *
     * @return
     */
    public List<CommFieldEndpoint> getEndpoints() {
        return new ArrayList<>(this.endpoints.values());
    }

    /**
     * 获取联系人对应的场域节点。
     * 仅用于私域下。
     *
     * @param contact
     * @return
     */
    public CommFieldEndpoint getEndpoint(Contact contact) {
        for (CommFieldEndpoint endpoint : this.endpoints.values()) {
            if (endpoint.getContact().equals(contact)) {
                return endpoint;
            }
        }

        return null;
    }

    /**
     * 获取指定联系人及其设备的场域节点。
     *
     * @param contact
     * @param device
     * @return
     */
    public CommFieldEndpoint getEndpoint(Contact contact, Device device) {
        for (CommFieldEndpoint endpoint : this.endpoints.values()) {
            if (endpoint.getContact().equals(contact) && endpoint.getDevice().equals(device)) {
                return endpoint;
            }
        }

        return null;
    }

    /**
     * 追踪指定终端的呼叫。
     *
     * @param scheduledExecutor
     * @param endpoint
     * @param timeoutCallback
     */
    public void startTrace(ScheduledExecutorService scheduledExecutor, CommFieldEndpoint endpoint,
                           Runnable timeoutCallback) {
        if (null == this.timeoutFutureMap) {
            this.timeoutFutureMap = new ConcurrentHashMap<>();
        }

        ScheduledFuture<?> future = scheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                timeoutFutureMap.remove(endpoint.getId());
                timeoutCallback.run();
            }
        }, this.timerTimeout, TimeUnit.MILLISECONDS);

        this.timeoutFutureMap.put(endpoint.getId(), future);
    }

    /**
     * 停止超时追踪。
     *
     * @param endpoint
     */
    public void stopTrace(CommFieldEndpoint endpoint) {
        if (null == this.timeoutFutureMap) {
            return;
        }

        ScheduledFuture<?> future = this.timeoutFutureMap.remove(endpoint.getId());
        if (null != future) {
            future.cancel(true);
        }
    }

    public void clearTraces() {
        Iterator<ScheduledFuture<?>> iter = this.timeoutFutureMap.values().iterator();
        while (iter.hasNext()) {
            iter.next().cancel(true);
        }
        this.timeoutFutureMap.clear();
    }

    /**
     * 清除所有呼叫和终端。但是不能清除定时器。
     */
    public void clearAll() {
        this.clearCalling();
        this.clearEndpoints();
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof CommField) {
            CommField other = (CommField) object;
            if (other.getUniqueKey().equals(this.getUniqueKey())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.getUniqueKey().hashCode();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("domain", this.domain.getName());
        json.put("name", this.name);
        json.put("founder", this.founder.toCompactJSON());
        json.put("mediaConstraint", this.mediaConstraint.toJSON());

        JSONArray array = new JSONArray();
        for (CommFieldEndpoint cfe : this.endpoints.values()) {
            array.put(cfe.toJSON());
        }
        json.put("endpoints", array);

        if (null != this.group) {
            json.put("group", this.group.toCompactJSON());
        }

        if (null != this.boundCalling) {
            json.put("caller", this.boundCalling.caller.toBasicJSON());
            json.put("callee", this.boundCalling.callee.toBasicJSON());
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("domain", this.domain.getName());
        json.put("name", this.name);
        json.put("founder", this.founder.toBasicJSON());
        json.put("mediaConstraint", this.mediaConstraint.toJSON());

        if (null != this.group) {
            json.put("group", this.group.toCompactJSON());
        }

        if (null != this.boundCalling) {
            json.put("caller", this.boundCalling.caller.toBasicJSON());
            json.put("callee", this.boundCalling.callee.toBasicJSON());
        }

        return json;
    }


    /**
     * 呼叫记录。
     */
    protected class BoundCalling {

        /**
         * 主叫，用于 One 2 One
         */
        protected Contact caller;

        /**
         * 被叫，用于 One 2 One
         */
        protected Contact callee;

        protected long timestamp;

        public BoundCalling(Contact caller, Contact callee) {
            this.caller = caller;
            this.callee = callee;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object object) {
            if (null != object && object instanceof BoundCalling) {
                BoundCalling other = (BoundCalling) object;
                if (other.caller.equals(this.caller) && other.callee.equals(this.callee)) {
                    return true;
                }
            }

            return false;
        }
    }
}
