/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.Page;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 复合会话内容。
 */
public class ComplexContext extends Entity {

    public enum Type {

        Simplex("simplex"),

        Complex("complex");

        public final String value;

        Type(String value) {
            this.value = value;
        }

        public static Type parse(String value) {
            if (value.equalsIgnoreCase(Simplex.value)) {
                return Simplex;
            }
            else {
                return Complex;
            }
        }
    }

    public final Type type;

    private boolean searchable = true;

    private List<ComplexResource> resources;

    private boolean inferable = false;

    private final AtomicBoolean inferring = new AtomicBoolean(false);

    private List<String> inferenceResult;

    private boolean networking = false;
    private boolean networkingInferEnd = false;
    private List<Page> networkingPages;
    private String networkingResult;

    public ComplexContext(Type type) {
        super(Utils.generateSerialNumber());
        this.type = type;
        this.resources = new ArrayList<>();
    }

    public ComplexContext(JSONObject json) {
        super(json);
        this.type = Type.parse(json.getString("type"));
        this.resources = new ArrayList<>();

        JSONArray array = json.getJSONArray("resources");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject data = array.getJSONObject(i);
            String subject = data.getString("subject");
            if (subject.equals(ComplexResource.Subject.Hyperlink.name())) {
                this.resources.add(new HyperlinkResource(data));
            }
            else if (subject.equals(ComplexResource.Subject.Chart.name())) {
                this.resources.add(new ChartResource(data));
            }
            else if (subject.equals(ComplexResource.Subject.Attachment.name())) {
                this.resources.add(new AttachmentResource(data));
            }
            else {
                Logger.e(this.getClass(), "Unknown complex context resource subject: " + subject);
            }
        }

        if (json.has("inferable")) {
            this.inferable = json.getBoolean("inferable");
        }
        if (json.has("inferring")) {
            this.inferring.set(json.getBoolean("inferring"));
        }

        if (json.has("inferenceResult")) {
            this.inferenceResult = new ArrayList<>();
            JSONArray list = json.getJSONArray("inferenceResult");
            for (int i = 0; i < list.length(); ++i) {
                this.inferenceResult.add(list.getString(i));
            }
        }

        if (json.has("searchable")) {
            this.searchable = json.getBoolean("searchable");
        }

        if (json.has("networking")) {
            this.networking = json.getBoolean("networking");
        }
        if (json.has("networkingInferEnd")) {
            this.networkingInferEnd = json.getBoolean("networkingInferEnd");
        }
        if (json.has("networkingPages")) {
            this.networkingPages = new ArrayList<>();
            JSONArray list = json.getJSONArray("networkingPages");
            for (int i = 0; i < list.length(); ++i) {
                this.networkingPages.add(new Page(list.getJSONObject(i)));
            }
        }
        if (json.has("networkingResult")) {
            this.networkingResult = json.getString("networkingResult");
        }
    }

    public boolean isSimplex() {
        return this.type == Type.Simplex;
    }

    public int numResources() {
        return this.resources.size();
    }

    public boolean hasResource(ComplexResource.Subject subject) {
        for (ComplexResource res : this.resources) {
            if (res.subject == subject) {
                return true;
            }
        }

        return false;
    }

    public ComplexResource getResource() {
        return this.resources.get(0);
    }

    public AttachmentResource getAttachmentResource() {
        for (ComplexResource res : this.resources) {
            if (res instanceof AttachmentResource) {
                return (AttachmentResource) res;
            }
        }
        return null;
    }

    public List<ComplexResource> getResources() {
        return this.resources;
    }

    public void addResource(ComplexResource resource) {
        this.resources.add(resource);
    }

    public void setSearchable(boolean value) {
        this.searchable = value;
    }

    public void setInferable(boolean value) {
        this.inferable = value;
    }

    public boolean isInferable() {
        return this.inferable;
    }

    public void setInferring(boolean value) {
        this.inferring.set(value);
    }

    public boolean isInferring(){
        return this.inferring.get();
    }

    public synchronized void addInferenceResult(String result) {
        if (null == this.inferenceResult) {
            this.inferenceResult = new ArrayList<>();
        }

        this.inferenceResult.add(result);
    }

    public List<String> getInferenceResult() {
        return this.inferenceResult;
    }

    public void setNetworking(boolean networking) {
        this.networking = networking;
    }

    public void fixNetworkingResult(List<Page> pages, String result) {
        this.networkingInferEnd = true;

        if (null != pages) {
            if (null == this.networkingPages) {
                this.networkingPages = new ArrayList<>();
            }
            this.networkingPages.addAll(pages);
        }
        if (null != result) {
            this.networkingResult = result;
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("type", this.type.value);

        JSONArray array = new JSONArray();
        for (ComplexResource resource : this.resources) {
            array.put(resource.toCompactJSON());
        }
        json.put("resources", array);

        json.put("inferable", this.inferable);
        json.put("inferring", this.inferring.get());

        if (null != this.inferenceResult) {
            JSONArray list = new JSONArray();
            for (String result : this.inferenceResult) {
                list.put(result);
            }
            json.put("inferenceResult", list);
        }

        json.put("searchable", this.searchable);

        json.put("networking", this.networking);
        json.put("networkingInferEnd", this.networkingInferEnd);

        if (null != this.networkingPages) {
            JSONArray list = new JSONArray();
            for (Page page : this.networkingPages) {
                list.put(page.toCompactJSON());
            }
            json.put("networkingPages", list);
        }

        if (null != this.networkingResult) {
            json.put("networkingResult", this.networkingResult);
        }

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        JSONObject json = this.toJSON();
        if (json.has("inferable")) {
            json.remove("inferable");
            json.remove("inferring");
        }
        if (json.has("inferenceResult")) {
            json.remove("inferenceResult");
        }
        if (json.has("networkingPages")) {
            json.remove("networkingPages");
        }
        return json;
    }
}
