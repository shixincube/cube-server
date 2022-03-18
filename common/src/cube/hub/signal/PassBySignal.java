/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.hub.signal;

import cube.hub.SignalBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 透传信令。
 */
public class PassBySignal extends Signal {

    public final static String NAME = "PassBy";

    private boolean broadcast = false;

    private List<Long> destinations;

    private List<Signal> signals;

    public PassBySignal() {
        super(NAME);
    }

    public PassBySignal(boolean broadcast) {
        super(NAME);
        this.broadcast = broadcast;
    }

    public PassBySignal(JSONObject json) {
        super(json);
        this.broadcast = json.getBoolean("broadcast");

        if (json.has("signals")) {
            this.signals = new ArrayList<>();

            JSONArray array = json.getJSONArray("signals");
            for (int i = 0; i < array.length(); ++i) {
                Signal signal = SignalBuilder.build(array.getJSONObject(i));
                if (null != signal) {
                    this.signals.add(signal);
                }
            }
        }

        if (json.has("destinations")) {
            this.destinations = new ArrayList<>();

            JSONArray array = json.getJSONArray("destinations");
            for (int i = 0; i < array.length(); ++i) {
                this.destinations.add(array.getLong(i));
            }
        }
    }

    public boolean isBroadcast() {
        return this.broadcast;
    }

    public void addDestination(Long pretenderId) {
        if (null == this.destinations) {
            this.destinations = new ArrayList<>();
            this.destinations.add(pretenderId);
        }
        else {
            if (!this.destinations.contains(pretenderId)) {
                this.destinations.add(pretenderId);
            }
        }

        this.broadcast = false;
    }

    public List<Long> getDestinations() {
        return this.destinations;
    }

    public void addSignal(Signal signal) {
        if (null == this.signals) {
            this.signals = new ArrayList<>();
        }

        this.signals.add(signal);
    }

    public void removeSignal(Signal signal) {
        if (null != this.signals) {
            this.signals.remove(signal);
        }
    }

    public List<Signal> getSignals() {
        return this.signals;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("broadcast", this.broadcast);

        if (null != this.destinations) {
            JSONArray array = new JSONArray();
            for (Long id : this.destinations) {
                array.put(id.longValue());
            }
            json.put("destinations", array);
        }

        if (null != this.signals) {
            JSONArray array = new JSONArray();
            for (Signal signal : this.signals) {
                array.put(signal.toJSON());
            }
            json.put("signals", array);
        }

        return json;
    }
}
