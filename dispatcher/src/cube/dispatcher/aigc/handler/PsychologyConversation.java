/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.aigc.handler;

import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;
import cube.dispatcher.aigc.Manager;
import cube.util.FileLabels;
import cube.util.TextUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 心理学领域对话。
 */
public class PsychologyConversation extends ContextHandler {

    public PsychologyConversation() {
        super("/aigc/psychology/converse");
        setHandler(new Handler());
    }

    private class Handler extends AIGCHandler {

        public Handler() {
            super();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) {
            String token = this.getApiToken(request);
            if (null == token) {
                this.respond(response, HttpStatus.NOT_ACCEPTABLE_406, this.makeError(HttpStatus.NOT_ACCEPTABLE_406));
                this.complete();
                return;
            }

            if (!Manager.getInstance().checkToken(token, this.getDevice(request))) {
                this.respond(response, HttpStatus.UNAUTHORIZED_401, this.makeError(HttpStatus.UNAUTHORIZED_401));
                this.complete();
                return;
            }

            try {
                JSONObject requestData = this.readBodyAsJSONObject(request);
                String channelCode = requestData.has("channel") ?
                        requestData.getString("channel") : requestData.getString("channelCode");
                String query = requestData.getString("query");
                JSONArray relations = requestData.has("relations") ?
                        requestData.getJSONArray("relations") : null;
                JSONObject context = requestData.has("context") ?
                        requestData.getJSONObject("context") : null;
                JSONObject relation = requestData.has("relation") ?
                        requestData.getJSONObject("relation") : null;

                JSONObject result = null;
                if (null != relations) {
                    result = Manager.getInstance().executePsychologyConversation(token, channelCode, relations, query);
                }
                else {
                    result = Manager.getInstance().executePsychologyConversation(token, channelCode,
                            context, relation, query);
                }
                if (null != result) {
                    if (result.has("queryFileLabels")) {
                        JSONArray array = result.getJSONArray("queryFileLabels");
                        for (int i = 0; i < array.length(); ++i) {
                            FileLabels.reviseFileLabel(array.getJSONObject(i), token,
                                    Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                                    Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                        }
                    }
                    if (result.has("answerFileLabels")) {
                        JSONArray array = result.getJSONArray("answerFileLabels");
                        for (int i = 0; i < array.length(); ++i) {
                            FileLabels.reviseFileLabel(array.getJSONObject(i), token,
                                    Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                                    Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                        }
                    }
                    if (result.has("context")) {
                        if (result.getJSONObject("context").has("resources")) {
                            JSONArray resources = result.getJSONObject("context").getJSONArray("resources");
                            for (int i = 0; i < resources.length(); ++i) {
                                JSONObject resJson = resources.getJSONObject(i);
                                JSONObject payload = resJson.getJSONObject("payload");
                                try {
                                    FileLabels.reviseFileLabel(payload.getJSONArray("attachments")
                                                    .getJSONObject(0).getJSONObject("fileLabel"),
                                            token,
                                            Manager.getInstance().getPerformer().getExternalHttpEndpoint(),
                                            Manager.getInstance().getPerformer().getExternalHttpsEndpoint());
                                } catch (Exception e) {
                                    // Nothing
                                }
                            }
                        }
                    }
                }

                String stream = request.getParameter("stream");
                if (null == stream) {
                    // 一般模式
                    if (null != result) {
                        this.respondOk(response, result);
                    }
                    else {
                        this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                    }
                    this.complete();
                }
                else {
                    // 流模式
                    if (null != result && result.has("answer")) {
                        String answer = result.getString("answer");

                        AnswerInputStream content = new AnswerInputStream(result, answer);
                        response.setContentLength(content.getContentLength());
                        // Async output
                        AsyncContext async = request.startAsync();
                        ServletOutputStream output = async.getResponse().getOutputStream();
                        StandardDataStream dataStream = new StandardDataStream(content, async, output);
                        async.addListener(new AsyncListener() {
                            @Override
                            public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
                                Logger.d(this.getClass(), "onStartAsync");
                            }

                            @Override
                            public void onComplete(AsyncEvent asyncEvent) throws IOException {
                                Logger.d(this.getClass(), "onComplete: " + content.getContentLength() + "/"
                                        + dataStream.contentLength + " - "
                                        + (dataStream.contentLength == content.getContentLength()));
                            }

                            @Override
                            public void onTimeout(AsyncEvent asyncEvent) throws IOException {
                                Logger.d(this.getClass(), "onTimeout");
                            }

                            @Override
                            public void onError(AsyncEvent asyncEvent) throws IOException {
                                Logger.d(this.getClass(), "onError");
                            }
                        });

                        // 设置数据写入监听器
                        output.setWriteListener(dataStream);
                        response.setStatus(HttpStatus.OK_200);
                        this.complete();
                    }
                    else {
                        this.respond(response, HttpStatus.BAD_REQUEST_400, this.makeError(HttpStatus.BAD_REQUEST_400));
                        this.complete();
                    }
                }
            } catch (Exception e) {
                this.respond(response, HttpStatus.FORBIDDEN_403, this.makeError(HttpStatus.FORBIDDEN_403));
                this.complete();
            }
        }
    }

    private final class AnswerInputStream extends InputStream {

        private List<byte[]> content;

        private int index;
        private int position;

        protected volatile boolean jump = false;

        public AnswerInputStream(JSONObject responseJson, String text) {
            this.index = 0;
            this.position = 0;
            this.content = new ArrayList<>();
            // 将文本随机拆分为列表
            List<String> list = TextUtils.randomSplitString(text);
            for (String line : list) {
                responseJson.put("answer", line);
                StringBuffer buf = new StringBuffer();
                buf.append(responseJson.toString()).append("\n");
                this.content.add(buf.toString().getBytes(StandardCharsets.UTF_8));
                buf = null;
            }
        }

        public int getContentLength() {
            int length = 0;
            for (byte[] bytes : this.content) {
                length += bytes.length;
            }
            return length;
        }

        public boolean peekJump() {
            byte[] bytes = this.content.get(this.index);
            if (this.position >= bytes.length) {
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public int read() throws IOException {
            if (this.index >= this.content.size()) {
                return -1;
            }

            byte[] bytes = this.content.get(this.index);
            if (this.position >= bytes.length) {
                this.jump = true;

                this.position = 0;
                ++this.index;
                if (this.index >= this.content.size()) {
                    return -1;
                }
                bytes = this.content.get(this.index);
            }
            else {
                this.jump = false;
            }

            byte b = bytes[this.position];
            ++this.position;
            return b & 0xFF;
        }
    }

    /**
     * 标准数据流输出。
     */
    private final class StandardDataStream implements WriteListener {

        private final AnswerInputStream content;
        private final AsyncContext async;
        private final ServletOutputStream output;

        protected long contentLength = 0;

        private StandardDataStream(AnswerInputStream content, AsyncContext async, ServletOutputStream output) {
            this.content = content;
            this.async = async;
            this.output = output;
        }

        @Override
        public void onWritePossible() throws IOException {
            FlexibleByteBuffer buffer = new FlexibleByteBuffer();

            // 输出流是否就绪
            while (this.output.isReady()) {
                int b = this.content.read();
                if (b == -1) {
                    buffer.flip();
                    if (buffer.limit() > 0) {
                        this.contentLength += buffer.limit();
                        this.output.write(buffer.array(), 0, buffer.limit());
                    }
                    break;
                }

                buffer.put((byte)b);

                if (this.content.peekJump()) {
                    buffer.flip();
                    this.contentLength += buffer.limit();
                    this.output.write(buffer.array(), 0, buffer.limit());
                    buffer.clear();
                }
            }

            this.async.complete();
            try {
                this.content.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            Logger.w(this.getClass(), "Async Error", throwable);
            this.async.complete();

            try {
                this.content.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
