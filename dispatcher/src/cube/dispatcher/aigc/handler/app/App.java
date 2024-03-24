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

package cube.dispatcher.aigc.handler.app;

import cell.util.log.Logger;
import cube.aigc.Consts;
import cube.aigc.ConversationRequest;
import cube.aigc.ConversationResponse;
import cube.aigc.ModelConfig;
import cube.common.JSONable;
import cube.common.entity.*;
import cube.util.HttpClientFactory;
import cube.util.JSONUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.WWWAuthenticationProtocolHandler;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class App {

    public final static App instance = new App();

    /**
     * Key: 令牌
     */
    private Map<String, ModelConfig> modelConfigMap;

    /**
     * Key: 令牌, Value: Channel
     */
    private Map<String, ChannelInfo> tokenChannelMap;

    private final long channelTimeout = 25 * 60 * 1000;

    private long lastCheckTime = 0;

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

    public void onTick(long now) {
        // 删除超时的频道
        if (now - this.lastCheckTime >= this.channelTimeout) {
            this.lastCheckTime = now;

            Iterator<Map.Entry<String, ChannelInfo>> iter = this.tokenChannelMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, ChannelInfo> e = iter.next();
                ChannelInfo channel = e.getValue();
                if (now - channel.lastActivate > this.channelTimeout) {
                    iter.remove();
                }
            }
        }
    }

    public ModelConfig getModelConfig(String token) {
        return this.modelConfigMap.get(token);
    }

    public ChannelInfo getChannel(String token) {
        return this.tokenChannelMap.get(token);
    }

    public ChannelInfo openChannel(String token, ModelConfig config) {
        this.modelConfigMap.put(token, config);

        ChannelInfo channel = this.tokenChannelMap.get(token);
        if (null != channel && System.currentTimeMillis() - channel.lastActivate > this.channelTimeout) {
            // 超时
            this.tokenChannelMap.remove(token);
            channel = null;
        }

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
                    channel = new ChannelInfo(responseData.getString("code"));

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

    public ChannelInfo reopenChannel(String token, ModelConfig config) {
        this.tokenChannelMap.remove(token);
        return this.openChannel(token, config);
    }

    public ChannelInfo resetModel(String token, ModelConfig config) {
        this.modelConfigMap.put(token, config);
        return this.reopenChannel(token, config);
    }

    public List<ConversationResponse> requestConversation(String token, ConversationRequest request) {
        ChannelInfo channel = this.tokenChannelMap.get(token);
        if (null == channel) {
            Logger.w(this.getClass(), "#requestConversation - Not find channel for token: " + token);
            return null;
        }

        if (null == request.options.workPattern) {
            request.options.workPattern = Consts.PATTERN_CHAT;
        }
        if (null == request.options.categories) {
            request.options.categories = new JSONArray();
        }

        // 附件对象转记录对象
        JSONArray records = new JSONArray();
        if (null != request.options.attachments) {
            List<String> contentList = new ArrayList<>();
            List<FileLabel> fileLabelList = new ArrayList<>();

            for (int i = 0; i < request.options.attachments.length(); ++i) {
                JSONObject attachmentJson = request.options.attachments.getJSONObject(i);
                if (attachmentJson.has("fileLabel") && !attachmentJson.isNull("fileLabel")) {
                    FileLabel fileLabel = new FileLabel(attachmentJson.getJSONObject("fileLabel"));
                    fileLabelList.add(fileLabel);
                }

                if (attachmentJson.has("content") && !attachmentJson.isNull("content")) {
                    contentList.add(attachmentJson.getString("content"));
                }
            }

            if (!fileLabelList.isEmpty()) {
                GenerativeRecord record = new GenerativeRecord(fileLabelList);
                records.put(record.toJSON());
                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "#requestConversation - Use file - num: " + fileLabelList.size());
                }
            }
            if (!contentList.isEmpty()) {
                GenerativeRecord record = new GenerativeRecord(contentList.toArray(new String[0]));
                records.put(record.toJSON());
                if (Logger.isDebugLevel()) {
                    Logger.d(this.getClass(), "#requestConversation - Use text - num: " + contentList.size());
                }
            }
        }

        ModelConfig config = this.modelConfigMap.get(token);
        if (null == config) {
            Logger.w(this.getClass(), "#requestConversation - Not find model config for token: " + token);
            return null;
        }

        long now = System.currentTimeMillis();
        if (now - channel.lastActivate > this.channelTimeout) {
            channel = this.reopenChannel(token, config);
            if (null == channel) {
                Logger.w(this.getClass(), "#requestConversation - Reopen channel failed: " + token);
                return null;
            }
        }
        else {
            channel.lastActivate = now;
        }

        String convId = channel.code;

        // 序号
        long sn = 0;

        // 结果内容
        String content = null;
        // 内容的复合数据上下文
        ComplexContext context = null;
        // 是否结束
        boolean end = false;
        // 结果文件
        List<FileLabel> fileLabels = null;
        // 知识源
        List<KnowledgeSource> knowledgeSources = null;

        String url = config.getApiURL() + token;

        // 处理参数，将模型配置的默认参数代入
        JSONObject apiData = JSONUtils.clone(config.getParameter());
        apiData.put("code", convId);
        apiData.put("content", request.prompt);
        apiData.put("histories", request.usingContext ? 10 : 0);
        apiData.put("networking", request.usingNetwork);
        apiData.put("pattern", request.options.workPattern);
        apiData.put("categories", request.options.categories);
        apiData.put("temperature", request.temperature);
        apiData.put("topP", request.topP);
        apiData.put("searchTopK", request.searchTopK);
        apiData.put("searchFetchK", request.searchFetchK);
        if (records.length() > 0) {
            apiData.put("records", records);
        }

        HttpClient client = HttpClientFactory.getInstance().borrowHttpClient();
        try {
            client.getProtocolHandlers().remove(WWWAuthenticationProtocolHandler.NAME);

            StringContentProvider provider = new StringContentProvider(apiData.toString());
            ContentResponse response = client.POST(url).timeout(5, TimeUnit.MINUTES).content(provider).send();
            if (response.getStatus() == HttpStatus.OK_200) {
                JSONObject responseData = new JSONObject(response.getContentAsString());
                if (responseData.has("content")) {
                    end = true;
                    sn = responseData.getLong("sn");
                    content = responseData.getString("content");
                    if (responseData.has("fileLabels")) {
                        fileLabels = parseFileLabels(responseData.getJSONArray("fileLabels"));
                    }
                    if (responseData.has("knowledgeSources")) {
                        knowledgeSources = parseKnowledgeSources(responseData.getJSONArray("knowledgeSources"));
                    }
                }
                else if (responseData.has("response")) {
                    end = true;
                    sn = responseData.getLong("sn");
                    content = responseData.getJSONObject("response").getString("answer");
                    if (responseData.getJSONObject("response").has("fileLabels")) {
                        fileLabels = parseFileLabels(responseData.getJSONObject("response").getJSONArray("fileLabels"));
                    }
                }
                else if (responseData.has("lastMetaSn")) {
                    end = false;
                    // 返回正在处理状态，例如 Text to Image，此时返回 AIGCChannel info 格式
                    sn = responseData.getLong("lastMetaSn");
                    content = responseData.getString("message");
                }

                if (responseData.has("context")) {
                    context = new ComplexContext(responseData.getJSONObject("context"));
                }
            }
            else {
                Logger.w(this.getClass(), "#requestConversation - status code: " + response.getStatus());
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

        if (null == content) {
            Logger.w(this.getClass(), "#requestConversation - Model unit return error: " + token);
            return null;
        }

        if (end) {
            if (null != fileLabels) {
                return this.makeResponse(sn, convId, request, fileLabels);
            }
            else {
                List<ConversationResponse> list = this.splitResponse(sn, convId, request, content);
                // 处理知识源
                if (null != knowledgeSources && !knowledgeSources.isEmpty()) {
                    ConversationResponse source = this.makeResponse(sn, convId, knowledgeSources);
                    list.add(source);
                }
                // 仅在第一个元素携带上下文
                list.get(0).context = context;
                return list;
            }
        }
        else {
            return this.makeResponse(sn, convId, request, content, end);
        }
    }

    private List<FileLabel> parseFileLabels(JSONArray array) {
        List<FileLabel> list = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            list.add(new FileLabel(array.getJSONObject(i)));
        }
        return list;
    }

    private List<KnowledgeSource> parseKnowledgeSources(JSONArray array) {
        List<KnowledgeSource> list = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            list.add(new KnowledgeSource(array.getJSONObject(i)));
        }
        return list;
    }

    private List<ConversationResponse> makeResponse(long sn, String conversationId,
                                                    ConversationRequest request, List<FileLabel> fileLabels) {
        List<ConversationResponse> result = new ArrayList<>();

        // 父 ID
        String pid = request.options.parentMessageId;
        if (null == pid) {
            pid = "";
        }

        String id = UUID.randomUUID().toString();
        ConversationResponse response = new ConversationResponse(sn, id, conversationId, pid, fileLabels);
        result.add(response);

        return result;
    }

    private List<ConversationResponse> makeResponse(long sn, String conversationId,
                                                    ConversationRequest request, String content, boolean end) {
        List<ConversationResponse> result = new ArrayList<>();

        // 父 ID
        String pid = request.options.parentMessageId;
        if (null == pid) {
            pid = "";
        }

        String id = UUID.randomUUID().toString();
        ConversationResponse response = new ConversationResponse(sn, id, conversationId, content, pid, end);
        result.add(response);

        return result;
    }

    private List<ConversationResponse> splitResponse(long sn, String conversationId,
                                                     ConversationRequest request, String content) {
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
            ConversationResponse response = new ConversationResponse(sn, id, conversationId, text, pid);
            result.add(response);

            pid = id;
        }

        return result;
    }

    private ConversationResponse makeResponse(long sn, String conversationId, List<KnowledgeSource> sources) {
        StringBuilder text = new StringBuilder("\n");
        text.append("> 数据来源：\n");
        for (KnowledgeSource source : sources) {
            text.append("> ").append(source.toString()).append("\n");
        }

        String id = UUID.randomUUID().toString();
        ConversationResponse response = new ConversationResponse(sn, id, conversationId, text.toString(), "");
        return response;
    }


    public class ChannelInfo implements JSONable {

        public final String code;

        public long lastActivate;

        public ChannelInfo(String code) {
            this.code = code;
            this.lastActivate = System.currentTimeMillis();
        }

        @Override
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("code", this.code);
            return json;
        }

        @Override
        public JSONObject toCompactJSON() {
            return this.toJSON();
        }
    }
}
