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

import java.util.concurrent.ConcurrentHashMap;

/**
 * 多方通信场域。
 */
public class CommField extends Entity {

    private Contact founder;

    private ConcurrentHashMap<Long, CommFieldEndpoint> fieldEndpoints;

    public CommField(Long id, String domainName, Contact founder) {
        super(id, domainName);

        this.founder = founder;

        this.fieldEndpoints = new ConcurrentHashMap<>();
    }

    public CommField(JSONObject json) {
        super();

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

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("domain", this.domain.getName());
            json.put("founder", this.founder.toCompactJSON());

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
            json.put("founder", this.founder.toCompactJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
