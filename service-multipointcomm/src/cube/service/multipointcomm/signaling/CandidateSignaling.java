/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm.signaling;

import cube.common.action.MultipointCommAction;
import cube.common.entity.CommField;
import cube.common.entity.CommFieldEndpoint;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Candidate 信令。
 */
public class CandidateSignaling extends Signaling {

    private JSONObject candidate;

    private List<JSONObject> candidateList;

    public CandidateSignaling(CommField field, CommFieldEndpoint endpoint) {
        super(MultipointCommAction.Candidate.name, field, endpoint.getContact(), endpoint.getDevice());
    }

    public CandidateSignaling(CommField field, Contact contact, Device device) {
        super(MultipointCommAction.Candidate.name, field, contact, device);
    }

    public CandidateSignaling(JSONObject json) {
        super(json);

        if (json.has("candidate")) {
            this.candidate = json.getJSONObject("candidate");
        }

        if (json.has("candidates")) {
            JSONArray array = json.getJSONArray("candidates");
            this.candidateList = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); ++i) {
                JSONObject candidate = array.getJSONObject(i);
                this.candidateList.add(candidate);
            }
        }
    }

    public void setCandidate(JSONObject candidate) {
        this.candidate = candidate;
    }

    public JSONObject getCandidate() {
        return this.candidate;
    }

    public void setCandidateList(List<JSONObject> candidates) {
        if (null == this.candidateList) {
            this.candidateList = new ArrayList<>();
        }

        this.candidateList.addAll(candidates);
    }

    /**
     *
     * @return
     */
    public int numCandidates() {
        int num = (null != this.candidate) ? 1 : 0;
        return (num + ((null != this.candidateList) ? this.candidateList.size() : 0));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        if (null != this.candidate) {
            json.put("candidate", this.candidate);
        }

        if (null != this.candidateList) {
            JSONArray array = new JSONArray();
            for (JSONObject candidate : this.candidateList) {
                array.put(candidate);
            }

            json.put("candidates", array);
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
