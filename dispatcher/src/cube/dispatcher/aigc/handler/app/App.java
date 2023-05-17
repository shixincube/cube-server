/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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

package cube.dispatcher.aigc.handler.app;

import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.ConversationRequest;
import cube.aigc.ConversationResponse;
import cube.aigc.ModelConfig;
import cube.util.HttpClientFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.WWWAuthenticationProtocolHandler;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public final class App {

    /**
     * Key: 令牌
     */
    private Map<String, ModelConfig> modelConfigMap;

    /**
     * Key: 令牌, Value: Channel Code
     */
    private Map<String, String> tokenChannelMap;

    public final static App instance = new App();

    private App() {
        this.modelConfigMap = new ConcurrentHashMap<>();
        this.tokenChannelMap = new ConcurrentHashMap<>();
    }

    public final static App getInstance() {
        return App.instance;
    }

    public void start() {
    }

    public void stop() {
    }

    public String openChannel(String token, ModelConfig config) {
        this.modelConfigMap.put(token, config);

        String channel = this.tokenChannelMap.get(token);

        if (null == channel) {
            HttpClient client = HttpClientFactory.getInstance().borrowHttpClient();

            String url = config.getChannelURL() + token;

            JSONObject data = new JSONObject();
            data.put("participant", token);

            try {
                client.getProtocolHandlers().remove(WWWAuthenticationProtocolHandler.NAME);

                StringContentProvider provider = new StringContentProvider(data.toString());
                ContentResponse response = client.POST(url).content(provider).send();
                if (response.getStatus() == HttpStatus.OK_200) {
                    JSONObject responseData = new JSONObject(response.getContentAsString());
                    channel = responseData.getString("code");

                    // 更新频道码
                    this.tokenChannelMap.put(token, channel);
                }
                else {
                    Logger.w(this.getClass(), "#openChannel - status code: " + response.getStatus());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                HttpClientFactory.getInstance().returnHttpClient(client);
            }
        }

        return channel;
    }

    public List<ConversationResponse> requestConversation(String token, ConversationRequest request) {
        ModelConfig config = this.modelConfigMap.get(token);
        if (null == config) {
            Logger.w(this.getClass(), "#requestModel - Not find model config for token: " + token);
            return null;
        }

        String convId = this.tokenChannelMap.get(token);
        if (null == convId) {
            Logger.w(this.getClass(), "#requestModel - Not find channel for token: " + token);
            return null;
        }

//        String url = config.getApiURL() + token + "/";
//        HttpClient client = HttpClientFactory.getInstance().borrowHttpClient();
//        client.POST(url);

        String content = "来自 App 测试：" + request.prompt + " : " + Utils.randomString(4) + " - 这是一串字符串";
        return this.splitResponse(convId, request, content);
    }

    private List<ConversationResponse> splitResponse(String conversationId, ConversationRequest request, String content) {
        List<ConversationResponse> result = new ArrayList<>();

        // 先拆为字符数组
        List<String> list = Helper.splitContent(content);

        // 父 ID
        String pid = request.options.parentMessageId;
        if (null == pid) {
            pid = "";
        }

        for (int i = 0; i < list.size(); ++i) {
            String text = list.get(i);

            String id = UUID.randomUUID().toString();
            ConversationResponse response = new ConversationResponse(id, conversationId, text, pid);
            result.add(response);

            pid = id;
        }

        return result;
    }
}
