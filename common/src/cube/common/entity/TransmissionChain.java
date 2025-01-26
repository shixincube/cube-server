/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 传输链。
 * 传输链上可以同时追踪不同 SHA1 码的文件。
 * 文件允许被修改，修改之后仍然在同一条链上追踪。
 */
public class TransmissionChain extends Entity {

    private String traceCode;

    private List<ChainNode> nodeList;

    public TransmissionChain(String traceCode) {
        super();
        this.traceCode = traceCode;
        this.nodeList = new LinkedList<>();
    }

    public void addNode(ChainNode node) {
        this.nodeList.add(node);
    }

    @Override
    public JSONObject toJSON() {
        return this.toCompactJSON();
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = new JSONObject();
        json.put("traceCode", this.traceCode);

        JSONArray nodes = new JSONArray();
        for (ChainNode node : this.nodeList) {
            nodes.put(node.toCompactJSON());
        }
        json.put("nodes", nodes);

        return json;
    }
}
