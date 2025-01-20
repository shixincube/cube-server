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

import cube.aigc.Consts;
import cube.aigc.ModelConfig;
import cube.auth.AuthToken;
import cube.common.entity.*;
import cube.service.aigc.AIGCService;
import cube.util.TextUtils;

/**
 * 资源内容回答。
 */
public class ResourceAnswer {

    private ComplexContext complexContext;

    public ResourceAnswer(ComplexContext complexContext) {
        this.complexContext = complexContext;
    }

    public String answer(String addition) {
        String result = "";

        if (this.complexContext.numResources() == 1) {
            ComplexResource res = this.complexContext.getResource();
            if (res instanceof HyperlinkResource) {
                HyperlinkResource resource = (HyperlinkResource) res;
                if (HyperlinkResource.TYPE_PAGE.equals(resource.metaType)) {
                    result = Consts.formatUrlPageAnswer(resource.url, TextUtils.extractDomain(resource.url),
                            resource.title, resource.content.length());
                }
                else if (HyperlinkResource.TYPE_IMAGE.equals(resource.metaType)) {
                    result = Consts.formatUrlImageAnswer(TextUtils.extractDomain(resource.url),
                            resource.format, resource.width, resource.height,
                            resource.size);
                }
                else if (HyperlinkResource.TYPE_PLAIN.equals(resource.metaType)) {
                    result = Consts.formatUrlPlainAnswer(TextUtils.extractDomain(resource.url),
                            resource.numWords, resource.size);
                }
                else if (HyperlinkResource.TYPE_VIDEO.equals(resource.metaType)) {
                    result = Consts.formatUrlVideoAnswer(TextUtils.extractDomain(resource.url),
                            resource.size);
                }
                else if (HyperlinkResource.TYPE_AUDIO.equals(resource.metaType)) {
                    result = Consts.formatUrlAudioAnswer(TextUtils.extractDomain(resource.url),
                            resource.size);
                }
                else if (HyperlinkResource.TYPE_OTHER.equals(resource.metaType)) {
                    result = Consts.formatUrlOtherAnswer(TextUtils.extractDomain(resource.url),
                            resource.size);
                }
                else {
                    result = Consts.formatUrlFailureAnswer(TextUtils.extractDomain(resource.url),
                            resource.url);
                }
            }
            else if (res instanceof ChartResource) {
                ChartResource resource = (ChartResource) res;
                result = Consts.formatChartAnswer(resource.getTitle());
            }
            else if (res instanceof AttachmentResource) {
                result = Consts.ANSWER_SILENT;
            }
            else {
                result = Consts.ANSWER_NO_CONTENT;
            }
        }
        else {
            StringBuilder buf = new StringBuilder();

            if (this.complexContext.getResources().get(0) instanceof HyperlinkResource) {
                buf.append(Consts.formatUrlSomeAnswer(this.complexContext.numResources()));
                buf.append("\n");
                for (ComplexResource res : this.complexContext.getResources()) {
                    HyperlinkResource resource = (HyperlinkResource) res;
                    if (HyperlinkResource.TYPE_PAGE.equals(resource.metaType)) {
                        buf.append("* ");
                        buf.append(Consts.formatUrlSomePageAnswer(resource.url));
                        buf.append("\n");
                    }
                    else if (HyperlinkResource.TYPE_IMAGE.equals(resource.metaType)) {
                        buf.append("* ");
                        buf.append(Consts.formatUrlSomeImageAnswer(resource.url));
                        buf.append("\n");
                    }
                    else if (HyperlinkResource.TYPE_PLAIN.equals(resource.metaType)) {
                        buf.append("* ");
                        buf.append(Consts.formatUrlSomePlainAnswer(resource.url));
                        buf.append("\n");
                    }
                    else if (HyperlinkResource.TYPE_VIDEO.equals(resource.metaType)) {
                        buf.append("* ");
                        buf.append(Consts.formatUrlSomeVideoAnswer(resource.url));
                        buf.append("\n");
                    }
                    else if (HyperlinkResource.TYPE_AUDIO.equals(resource.metaType)) {
                        buf.append("* ");
                        buf.append(Consts.formatUrlSomeAudioAnswer(resource.url));
                        buf.append("\n");
                    }
                    else if (HyperlinkResource.TYPE_OTHER.equals(resource.metaType)) {
                        buf.append("* ");
                        buf.append(Consts.formatUrlSomeOtherAnswer(resource.url, resource.mimeType));
                        buf.append("\n");
                    }
                    else {
                        buf.append("* ");
                        buf.append(Consts.formatUrlSomeFailureAnswer(resource.url));
                        buf.append("\n");
                    }
                }
            }
            else if (this.complexContext.getResources().get(0) instanceof ChartResource) {
                buf.append(Consts.formatChartSomeAnswer(this.complexContext.numResources()));
                buf.append("\n");
                for (ComplexResource res : this.complexContext.getResources()) {
                    ChartResource resource = (ChartResource) res;
                    buf.append("* ");
                    buf.append(Consts.formatChartSomeOneAnswer(resource.getChart().desc));
                    buf.append("\n");
                }
            }
            else if (this.complexContext.getResources().get(0) instanceof AttachmentResource) {
                buf.append(Consts.ANSWER_SILENT);
            }
            else {
                buf.append(Consts.ANSWER_NO_CONTENT);
            }

            result = buf.toString();
        }

        if (null != addition && addition.length() > 1) {
            result = result + "\n\n" + addition;
        }

        return result;
    }

    public String extractContent(AIGCService service, AuthToken authToken) {
        StringBuilder result = new StringBuilder();

        for (ComplexResource res : this.complexContext.getResources()) {
            HyperlinkResource resource = (HyperlinkResource) res;
            if (HyperlinkResource.TYPE_PAGE.equals(resource.metaType)) {
                try {
                    StringBuilder content = new StringBuilder();

                    String[] lines = resource.content.split("\n");
                    int offset = lines.length > 80 ? 10 : 0;
                    for (int i = offset, len = lines.length - offset; i < len; ++i) {
                        content.append(lines[i]).append("\n");
                        if (content.length() >= ModelConfig.BAIZE_CONTEXT_LIMIT - 100) {
                            break;
                        }
                    }

                    if (content.length() > 10) {
                        String prompt = Consts.formatExtractContent(content.toString(), resource.title);
                        String response = service.syncGenerateText(ModelConfig.CHAT_UNIT, prompt,
                                new GeneratingOption(), null, null);
                        if (null != response) {
                            result.append(response).append("\n");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return result.toString();
    }

    public String ask(String query) {
        StringBuilder data = new StringBuilder();

        // TODO XJW 从模组里读取相关数据和问答

//        for (ComplexResource res : this.complexContext.getResources()) {
//            if (res instanceof ChartResource) {
//                ChartResource chart = (ChartResource) res;
//                String plainText = chart.makeDataPlainString();
//                data.append(plainText);
//            }
//        }

        return Consts.formatQuestion(data.toString(), query);
    }
}
