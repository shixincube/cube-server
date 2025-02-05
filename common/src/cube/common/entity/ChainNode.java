/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 链条节点。
 */
public class ChainNode extends Entity {

    /**
     * 操作事件。
     */
    private String event;

    /**
     * 操作人。
     */
    private AbstractContact who;

    /**
     * 被操作的实体。
     */
    private FileLabel what;

    /**
     * 操作时间。
     */
    private long when;

    /**
     * 做了什么操作方式。
     */
    private TransmissionMethod method;

    /**
     * 追踪轨道。
     */
    private List<String> tracks;

    /**
     * 相邻前向节点。
     */
    private ChainNode previous;

    /**
     * 相邻后向节点。
     */
    private ChainNode next;

    /**
     * 子节点。
     */
    private List<ChainNode> children;

    /**
     * 数量。
     */
    private int total;

    /**
     * 访问追踪。
     */
    private VisitTrace visitTrace;

    public ChainNode(Long id, String domain, String event, AbstractContact who, FileLabel what, long when) {
        super(id, domain);
        this.event = event;
        this.who = who;
        this.what = what;
        this.when = when;
        this.tracks = new ArrayList<>();
    }

    public ChainNode(long id, String domain, ChainNode previous, ChainNode next) {
        super(id, domain);
        this.previous = previous;
        this.next = next;
    }

    public ChainNode(long id, String domain, String event) {
        super(id, domain);
        this.event = event;

    }

    public ChainNode(VisitTrace visitTrace) {
        super();
        this.visitTrace = visitTrace;
        this.event = visitTrace.event;
        this.total = 1;
    }

    public String getEvent() {
        return this.event;
    }

    public AbstractContact getWho() {
        return this.who;
    }

    public FileLabel getWhat() {
        return this.what;
    }

    public long getWhen() {
        return this.when;
    }

    public void addTrack(String track) {
        if (this.tracks.contains(track)) {
            return;
        }

        this.tracks.add(track);
    }

    public List<String> getTracks() {
        return this.tracks;
    }

    public TransmissionMethod getMethod() {
        return this.method;
    }

    public void setMethod(TransmissionMethod method) {
        this.method = method;
    }

    public void addChildren(List<ChainNode> list) {
        if (null == this.children) {
            this.children = new ArrayList<>();
        }

        this.children.addAll(list);
    }

    public void addChild(ChainNode node) {
        if (null == this.children) {
            this.children = new ArrayList<>();
        }

        this.children.add(node);
    }

    public List<ChainNode> getChildren() {
        if (null == this.children) {
            this.children = new ArrayList<>();
        }

        return this.children;
    }

    public int incrementTotal() {
        return ++this.total;
    }

    public VisitTrace getVisitTrace() {
        return this.visitTrace;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ChainNode) {
            ChainNode other = (ChainNode) o;
            if (other.id.equals(this.id)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("event", this.event);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("event", this.event);
        json.put("total", this.total);

        if (null != this.visitTrace) {
            json.put("visitTrace", this.visitTrace.toMiniJSON());
        }

        JSONArray array = new JSONArray();
        if (null != this.children) {
            for (ChainNode child : this.children) {
                array.put(child.toCompactJSON());
            }
        }
        json.put("children", array);

        return json;
    }
}
