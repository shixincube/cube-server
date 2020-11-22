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

import java.util.ArrayList;
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

    private List<OutboundCalling> outboundCallingList;

    private List<InboundCalling> inboundCallingList;

    private long defaultTimeout = 42L * 1000L;

    private long offerTimeout = 45L;

    public CommField(Long id, String domainName, Contact founder) {
        super(id, domainName);

        this.founder = founder;

        this.fieldEndpoints = new ConcurrentHashMap<>();
        this.outboundCallingList = new Vector<>();
        this.inboundCallingList = new Vector<>();
    }

    public CommField(JSONObject json) {
        super();

        this.fieldEndpoints = new ConcurrentHashMap<>();
        this.outboundCallingList = new Vector<>();
        this.inboundCallingList = new Vector<>();

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
     * 查询指定联系人是否正在进行外呼。
     *
     * @param contact
     * @return
     */
    public boolean hasOutboundCall(Contact contact) {
        for (OutboundCalling oc : this.outboundCallingList) {
            if (oc.proposer.equals(contact)) {

                // 判断超时
                if (System.currentTimeMillis() - oc.timestamp > this.defaultTimeout) {
                    this.outboundCallingList.remove(oc);
                    return false;
                }

                return true;
            }
        }

        return false;
    }

    public void markOutboundCall(Contact proposer, Contact target) {
        OutboundCalling oc = new OutboundCalling(proposer, target);
        if (this.outboundCallingList.contains(oc)) {
            return;
        }

        this.outboundCallingList.add(oc);
    }

    public List<Contact> getOutboundCallTargets() {
        List<Contact> result = new ArrayList<>();
        for (OutboundCalling oc : this.outboundCallingList) {
            result.add(oc.target);
        }
        return result;
    }

    public void clearOutboundCall() {
        this.outboundCallingList.clear();
    }

    public void markInboundCall(Contact proposer, Contact target) {
        InboundCalling ic = new InboundCalling(proposer, target);
        if (this.inboundCallingList.contains(ic)) {
            return;
        }

        this.inboundCallingList.add(ic);
    }

    public Contact getInboundCallProposer() {
        if (this.inboundCallingList.isEmpty()) {
            return null;
        }

        return this.inboundCallingList.get(0).proposer;
    }

    public void clearInboundCall() {
        this.inboundCallingList.clear();
    }

    public void addEndpoint(CommFieldEndpoint endpoint) {
        this.fieldEndpoints.put(endpoint.getId(), endpoint);
    }

    public void removeEndpoint(CommFieldEndpoint endpoint) {
        this.fieldEndpoints.remove(endpoint.getId());
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
        }, this.offerTimeout, TimeUnit.SECONDS);

        this.offerFutureMap.put(endpoint.getId(), future);
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


    protected class OutboundCalling {

        protected Contact proposer;

        protected Contact target;

        protected long timestamp;

        public OutboundCalling(Contact proposer, Contact target) {
            this.proposer = proposer;
            this.target = target;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object object) {
            if (null != object && object instanceof OutboundCalling) {
                OutboundCalling other = (OutboundCalling) object;
                if (other.proposer.equals(this.proposer) && other.target.equals(this.target)) {
                    return true;
                }
            }

            return false;
        }
    }

    protected class InboundCalling {

        protected Contact proposer;

        protected Contact target;

        protected long timestamp;

        public InboundCalling(Contact proposer, Contact target) {
            this.proposer = proposer;
            this.target = target;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object object) {
            if (null != object && object instanceof OutboundCalling) {
                OutboundCalling other = (OutboundCalling) object;
                if (other.proposer.equals(this.proposer) && other.target.equals(this.target)) {
                    return true;
                }
            }

            return false;
        }
    }
}
