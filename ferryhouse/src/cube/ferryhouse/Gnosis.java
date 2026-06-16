/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2026 Ambrose Xu.
 */

package cube.ferryhouse;

import cell.core.net.Endpoint;
import cell.core.talk.dialect.ActionDialect;
import cell.util.log.Logger;
import cube.ferry.FerryAction;
import cube.ferry.FerryStateCode;
import cube.ferry.GnosisAgent;
import cube.util.HttpClientFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

public class Gnosis {

    private Endpoint endpoint;

    public Gnosis(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public JSONObject getVitalSigns(boolean mock) {
        if (!mock) {
            return requestGet("api/v1/vital-signs");
        }
        else {
            return new JSONObject("{\n" +
                    "    \"buffer_status\": {\n" +
                    "        \"breathing_capacity\": 300,\n" +
                    "        \"breathing_samples\": 0,\n" +
                    "        \"heartbeat_capacity\": 150,\n" +
                    "        \"heartbeat_samples\": 0\n" +
                    "    },\n" +
                    "    \"source\": \"esp32\",\n" +
                    "    \"tick\": 13462,\n" +
                    "    \"vital_signs\": {\n" +
                    "        \"breathing_confidence\": 0.23272526912521513,\n" +
                    "        \"breathing_rate_bpm\": 9.375,\n" +
                    "        \"heart_rate_bpm\": 84.54142141447271,\n" +
                    "        \"heartbeat_confidence\": 0.5282932747349278,\n" +
                    "        \"signal_quality\": 0.5\n" +
                    "    }\n" +
                    "}\n");
        }
    }

    public JSONObject getPersons(boolean mock) {
        if (!mock) {
            return requestGet("api/v1/pose/current");
        }
        else {
            return new JSONObject("{\n" +
                    "    \"persons\": [\n" +
                    "        {\n" +
                    "            \"bbox\": {\n" +
                    "                \"height\": 1.0,\n" +
                    "                \"width\": 0.6,\n" +
                    "                \"x\": 318.37260203642,\n" +
                    "                \"y\": 226.24498703900505\n" +
                    "            },\n" +
                    "            \"confidence\": 0.9,\n" +
                    "            \"id\": 2980,\n" +
                    "            \"keypoints\": [\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"nose\",\n" +
                    "                    \"x\": 318.43780517578125,\n" +
                    "                    \"y\": 153.6667938232422,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_eye\",\n" +
                    "                    \"x\": 312.8466491699219,\n" +
                    "                    \"y\": 143.41131591796875,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_eye\",\n" +
                    "                    \"x\": 323.4551086425781,\n" +
                    "                    \"y\": 144.59815979003906,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_ear\",\n" +
                    "                    \"x\": 300.8844299316406,\n" +
                    "                    \"y\": 148.93411254882812,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_ear\",\n" +
                    "                    \"x\": 335.19000244140625,\n" +
                    "                    \"y\": 149.4102783203125,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_shoulder\",\n" +
                    "                    \"x\": 291.3099670410156,\n" +
                    "                    \"y\": 184.3454132080078,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_shoulder\",\n" +
                    "                    \"x\": 345.7322692871094,\n" +
                    "                    \"y\": 182.92137145996094,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_elbow\",\n" +
                    "                    \"x\": 272.2490234375,\n" +
                    "                    \"y\": 220.09344482421875,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_elbow\",\n" +
                    "                    \"x\": 365.1173400878906,\n" +
                    "                    \"y\": 213.62828063964844,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_wrist\",\n" +
                    "                    \"x\": 270.8162841796875,\n" +
                    "                    \"y\": 254.5254669189453,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_wrist\",\n" +
                    "                    \"x\": 367.8811340332031,\n" +
                    "                    \"y\": 248.50796508789062,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_hip\",\n" +
                    "                    \"x\": 300.03839111328125,\n" +
                    "                    \"y\": 251.34637451171875,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_hip\",\n" +
                    "                    \"x\": 337.8976745605469,\n" +
                    "                    \"y\": 252.19871520996094,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_knee\",\n" +
                    "                    \"x\": 299.2127380371094,\n" +
                    "                    \"y\": 297.3845520019531,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_knee\",\n" +
                    "                    \"x\": 338.8644104003906,\n" +
                    "                    \"y\": 305.54595947265625,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_ankle\",\n" +
                    "                    \"x\": 293.368408203125,\n" +
                    "                    \"y\": 347.4258728027344,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_ankle\",\n" +
                    "                    \"x\": 344.1325988769531,\n" +
                    "                    \"y\": 356.720703125,\n" +
                    "                    \"z\": -0.24959996342658997\n" +
                    "                }\n" +
                    "            ],\n" +
                    "            \"zone\": \"tracked\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"bbox\": {\n" +
                    "                \"height\": 1.0,\n" +
                    "                \"width\": 0.6,\n" +
                    "                \"x\": 320.7999217313879,\n" +
                    "                \"y\": 226.24498703900505\n" +
                    "            },\n" +
                    "            \"confidence\": 0.9,\n" +
                    "            \"id\": 2984,\n" +
                    "            \"keypoints\": [\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"nose\",\n" +
                    "                    \"x\": 320.844482421875,\n" +
                    "                    \"y\": 153.6667938232422,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_eye\",\n" +
                    "                    \"x\": 315.2533264160156,\n" +
                    "                    \"y\": 143.41131591796875,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_eye\",\n" +
                    "                    \"x\": 325.8617858886719,\n" +
                    "                    \"y\": 144.59815979003906,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_ear\",\n" +
                    "                    \"x\": 303.2911071777344,\n" +
                    "                    \"y\": 148.93411254882812,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_ear\",\n" +
                    "                    \"x\": 337.5966796875,\n" +
                    "                    \"y\": 149.4102783203125,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_shoulder\",\n" +
                    "                    \"x\": 286.2583312988281,\n" +
                    "                    \"y\": 179.8704376220703,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_shoulder\",\n" +
                    "                    \"x\": 355.59722900390625,\n" +
                    "                    \"y\": 178.44639587402344,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_elbow\",\n" +
                    "                    \"x\": 274.65570068359375,\n" +
                    "                    \"y\": 217.4713134765625,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_elbow\",\n" +
                    "                    \"x\": 367.5240173339844,\n" +
                    "                    \"y\": 216.25039672851562,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_wrist\",\n" +
                    "                    \"x\": 273.067138671875,\n" +
                    "                    \"y\": 252.2033233642578,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_wrist\",\n" +
                    "                    \"x\": 370.6517028808594,\n" +
                    "                    \"y\": 250.8301239013672,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_hip\",\n" +
                    "                    \"x\": 294.9867858886719,\n" +
                    "                    \"y\": 255.8213653564453,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_hip\",\n" +
                    "                    \"x\": 347.76263427734375,\n" +
                    "                    \"y\": 256.6736755371094,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_knee\",\n" +
                    "                    \"x\": 301.6194152832031,\n" +
                    "                    \"y\": 300.6622009277344,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_knee\",\n" +
                    "                    \"x\": 341.2710876464844,\n" +
                    "                    \"y\": 302.2682800292969,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"left_ankle\",\n" +
                    "                    \"x\": 296.1709899902344,\n" +
                    "                    \"y\": 351.0035095214844,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"confidence\": 0.0,\n" +
                    "                    \"name\": \"right_ankle\",\n" +
                    "                    \"x\": 346.2862548828125,\n" +
                    "                    \"y\": 353.1430969238281,\n" +
                    "                    \"z\": -0.208467498421669\n" +
                    "                }\n" +
                    "            ],\n" +
                    "            \"zone\": \"tracked\"\n" +
                    "        }\n" +
                    "    ],\n" +
                    "    \"source\": \"esp32:offline\",\n" +
                    "    \"timestamp\": 1780316021.378,\n" +
                    "    \"total_persons\": 2\n" +
                    "}\n");
        }
    }

    public ActionDialect auto(ActionDialect actionDialect) {
        int sn = actionDialect.getParamAsInt("sn");

        ActionDialect result = new ActionDialect(FerryAction.GnosisAgentAck.name);
        result.addParam("sn", sn);

        GnosisAgent agent = new GnosisAgent(actionDialect.getParamAsJson("agent"));
        JSONObject data = null;
        if (agent.name.equalsIgnoreCase(GnosisAgent.VitalSigns)) {
            data = this.getVitalSigns(agent.mock);
        }
        else if (agent.name.equalsIgnoreCase(GnosisAgent.Persons)) {
            data = this.getPersons(agent.mock);
        }
        else {
            Logger.d(this.getClass(), "#auto - nonsupport: " + agent.name);
        }

        if (null != data) {
            result.addParam("code", FerryStateCode.Ok.code);
            result.addParam("result", data);
        }
        else {
            result.addParam("code", FerryStateCode.Failure.code);
        }

        return result;
    }

    private JSONObject requestGet(String urlPath) {
        JSONObject result = null;

        HttpClient client = HttpClientFactory.getInstance().borrowHttpClient();
        try {
            ContentResponse response = client.GET("http://" + this.endpoint.getHost() + ":" + this.endpoint.getPort() +
                    "/" + urlPath);
            if (response.getStatus() == HttpStatus.OK_200) {
                result = new JSONObject(response.getContentAsString());
            }
        } catch (Exception e) {
            Logger.w(this.getClass(), "", e);
        } finally {
            HttpClientFactory.getInstance().returnHttpClient(client);
        }

        return result;
    }
}
