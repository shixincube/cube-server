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

package cube.common.entity;

import cell.util.json.JSONArray;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.Domain;
import cube.common.UniqueKey;
import cube.common.state.MultipointCommStateCode;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 多方通信场域。
 */
public class CommField extends Entity {

    private Contact founder;

    private ConcurrentHashMap<Long, CommFieldEndpoint> fieldEndpoints;

    private ConcurrentHashMap<Long, ScheduledFuture<?>> offerFutureMap;

    private List<BoundCalling> boundCallingList;

    private long defaultTimeout = 40L * 1000L;

    private long offerTimeout = 45L * 1000L;

    /**
     *
     * @param id
     * @param domainName
     * @param founder
     */
    public CommField(Long id, String domainName, Contact founder) {
        super(id, domainName);

        this.founder = founder;

        this.fieldEndpoints = new ConcurrentHashMap<>();
        this.boundCallingList = new Vector<>();
    }

    public CommField(JSONObject json) {
        super();

        this.fieldEndpoints = new ConcurrentHashMap<>();
        this.boundCallingList = new Vector<>();

        try {
            this.id = json.getLong("id");
            this.domain = new Domain(json.getString("domain"));
            this.founder = new Contact(json.getJSONObject("founder"), this.domain);

            if (json.has("endpoints")) {
                JSONArray endpoints = json.getJSONArray("endpoints");

                for (int i = 0, size = endpoints.length(); i < size; ++i) {
                    JSONObject cfeJson = endpoints.getJSONObject(i);
                    CommFieldEndpoint cfe = new CommFieldEndpoint(cfeJson);
                    this.fieldEndpoints.put(cfe.getId(), cfe);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.uniqueKey = UniqueKey.make(this.id, this.domain);
    }

    public boolean isPrivate() {
        return (this.id.longValue() == this.founder.getId().longValue());
    }

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
        for (BoundCalling calling : this.boundCallingList) {
            if (calling.caller.equals(contact)) {
                return true;
            }
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
        for (BoundCalling calling : this.boundCallingList) {
            if (calling.callee.equals(contact)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 标记 Calling
     *
     * @param caller
     * @param callee
     */
    public void markSingleCalling(Contact caller, Contact callee) {
        this.boundCallingList.clear();

        BoundCalling calling = new BoundCalling(caller, callee);
        this.boundCallingList.add(calling);
    }

    public boolean isCalling(Contact contact) {
        long now = System.currentTimeMillis();
        for (BoundCalling calling : this.boundCallingList) {
            if (now - calling.timestamp > this.defaultTimeout) {
                continue;
            }

            if (calling.caller.equals(contact)) {
                if (calling.callerState == MultipointCommStateCode.CallBye) {
                    continue;
                }
                else {
                    return true;
                }
            }

            if (calling.callee.equals(contact)) {
                if (calling.calleeState == MultipointCommStateCode.CallBye) {
                    continue;
                }
                else {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 仅用于私域下。
     *
     * @return
     */
    public Contact getCaller() {
        if (this.boundCallingList.isEmpty()) {
            return null;
        }

        return this.boundCallingList.get(0).caller;
    }

    /**
     * 仅用于私域下。
     *
     * @return
     */
    public MultipointCommStateCode getCallerState() {
        if (this.boundCallingList.isEmpty()) {
            return MultipointCommStateCode.Unknown;
        }

        return this.boundCallingList.get(0).callerState;
    }

    public void updateCallerState(MultipointCommStateCode state) {
        if (this.boundCallingList.isEmpty()) {
            return;
        }

        this.boundCallingList.get(0).callerState = state;
    }

    /**
     * 仅用于私域下。
     *
     * @return
     */
    public Contact getCallee() {
        if (this.boundCallingList.isEmpty()) {
            return null;
        }

        return this.boundCallingList.get(0).callee;
    }

    /**
     * 仅用于私域下。
     *
     * @return
     */
    public MultipointCommStateCode getCalleeState() {
        if (this.boundCallingList.isEmpty()) {
            return MultipointCommStateCode.Unknown;
        }

        return this.boundCallingList.get(0).calleeState;
    }

    public void updateCalleeState(MultipointCommStateCode state) {
        if (this.boundCallingList.isEmpty()) {
            return;
        }

        this.boundCallingList.get(0).calleeState = state;
    }

    public void clearCalling() {
        this.boundCallingList.clear();
    }

    public void addEndpoint(CommFieldEndpoint endpoint) {
        this.fieldEndpoints.put(endpoint.getId(), endpoint);
    }

    public void removeEndpoint(CommFieldEndpoint endpoint) {
        this.fieldEndpoints.remove(endpoint.getId());
    }

    public void clearEndpoints() {
        this.fieldEndpoints.clear();
    }

    public void clearEndpoint(CommFieldEndpoint endpoint) {
        Contact contact = endpoint.getContact();

        Iterator<BoundCalling> iter = this.boundCallingList.iterator();
        while (iter.hasNext()) {
            BoundCalling calling = iter.next();
            if (calling.caller.equals(contact) || calling.callee.equals(contact)) {
                iter.remove();
            }
        }

        this.fieldEndpoints.remove(endpoint.getId());
    }

    /**
     * 仅用于私域下。
     *
     * @param contact
     * @return
     */
    public CommFieldEndpoint getEndpoint(Contact contact) {
        for (CommFieldEndpoint endpoint : this.fieldEndpoints.values()) {
            if (endpoint.getContact().equals(contact)) {
                return endpoint;
            }
        }

        return null;
    }

    public CommFieldEndpoint getEndpoint(Contact contact, Device device) {
        for (CommFieldEndpoint endpoint : this.fieldEndpoints.values()) {
            if (endpoint.getContact().equals(contact) && endpoint.getDevice().equals(device)) {
                return endpoint;
            }
        }

        return null;
    }

    public void traceOffer(ScheduledExecutorService scheduledExecutor, CommFieldEndpoint endpoint,
                           Runnable timeoutCallback) {
        if (null == this.offerFutureMap) {
            this.offerFutureMap = new ConcurrentHashMap<>();
        }

        ScheduledFuture<?> future = scheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                offerFutureMap.remove(endpoint.getId());
                timeoutCallback.run();
            }
        }, this.offerTimeout, TimeUnit.MILLISECONDS);

        this.offerFutureMap.put(endpoint.getId(), future);
    }

    public void stopTrace(CommFieldEndpoint endpoint) {
        if (null == this.offerFutureMap) {
            return;
        }

        ScheduledFuture<?> future = this.offerFutureMap.remove(endpoint.getId());
        if (null != future) {
            future.cancel(true);
        }
    }

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
        try {
            json.put("id", this.id);
            json.put("domain", this.domain.getName());
            json.put("founder", this.founder.toBasicJSON());

            JSONArray array = new JSONArray();
            for (CommFieldEndpoint cfe : this.fieldEndpoints.values()) {
                array.put(cfe.toJSON());
            }
            json.put("endpoints", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("domain", this.domain.getName());
            json.put("founder", this.founder.toBasicJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }


    protected class BoundCalling {

        protected Contact caller;

        protected MultipointCommStateCode callerState = MultipointCommStateCode.Ok;

        protected Contact callee;

        protected MultipointCommStateCode calleeState = MultipointCommStateCode.Ok;

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
