/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 Ambrose Xu.
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

package cube.service.aigc.resource;

import cell.util.log.Logger;
import cube.aigc.Consts;
import cube.aigc.ModelConfig;
import cube.common.entity.AICapability;
import cube.common.entity.AIGCUnit;
import cube.common.entity.Contact;
import cube.util.HttpClientFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.WWWAuthenticationProtocolHandler;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 代理访问。
 */
public final class Agent {

    private static Agent instance = null;

    private final String url;

    private AIGCUnit unit;

    public final static Agent getInstance() {
        return Agent.instance;
    }

    public final static Agent createInstance(String url) {
        Agent.instance = new Agent(url);
        return Agent.instance;
    }

    private Agent(String url) {
        this.url = url.endsWith("/") ? url : url + "/";

        Contact contact = new Contact(100000, "shixincube.com");

        List<String> subtasks = new ArrayList<>();
        subtasks.add(AICapability.NaturalLanguageProcessing.Conversational);
        AICapability capability = new AICapability(ModelConfig.BAIZE_UNIT,
                AICapability.NaturalLanguageProcessingTask, subtasks, "");

        this.unit = new AIGCUnit(contact, capability, null);
    }

    public String getUrl() {
        return this.url;
    }

    public AIGCUnit getUnit() {
        return this.unit;
    }

    public String generateText(String token, String channelCode, String content) {
        HttpClient client = HttpClientFactory.getInstance().borrowHttpClient();

        try {
            String chatUrl = this.url + "aigc/chat/" + token;

            JSONObject data = new JSONObject();
            data.put("code", channelCode);
            data.put("content", content);
            data.put("unit", this.unit.getCapability().getName());
            data.put("histories", 0);
            data.put("pattern", Consts.PATTERN_CHAT);
            data.put("recordable", false);

            client.getProtocolHandlers().remove(WWWAuthenticationProtocolHandler.NAME);

            StringContentProvider provider = new StringContentProvider(data.toString());
            ContentResponse response = client.POST(chatUrl).content(provider).send();

            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject responseData = new JSONObject(response.getContentAsString());
                if (responseData.has("content")) {
                    return responseData.getString("content");
                }
                else {
                    Logger.e(this.getClass(), "#generateText - Response data has no content");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            HttpClientFactory.getInstance().returnHttpClient(client);
        }

        return null;
    }
}
