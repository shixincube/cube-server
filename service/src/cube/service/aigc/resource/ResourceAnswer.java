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

package cube.service.aigc.resource;

import cube.aigc.Consts;
import cube.common.entity.ComplexContext;
import cube.common.entity.ComplexResource;
import cube.util.TextUtils;

/**
 * 资源内容回答。
 */
public class ResourceAnswer {

    private ComplexContext complexContext;

    public ResourceAnswer(ComplexContext complexContext) {
        this.complexContext = complexContext;
    }

    public String answer() {
        String result = "";

        if (this.complexContext.numResources() == 1) {
            ComplexResource resource = this.complexContext.getResource();
            if (ComplexResource.TYPE_PAGE.equals(resource.metaType)) {
                result = Consts.formatUrlPageAnswer(TextUtils.extractDomain(resource.url),
                        resource.title, resource.content.length());
            }
            else if (ComplexResource.TYPE_IMAGE.equals(resource.metaType)) {
                result = Consts.formatUrlImageAnswer(TextUtils.extractDomain(resource.url),
                        resource.format, resource.width, resource.height,
                        resource.size);
            }
            else if (ComplexResource.TYPE_PLAIN.equals(resource.metaType)) {
                result = Consts.formatUrlPlainAnswer(TextUtils.extractDomain(resource.url),
                        resource.numWords, resource.size);
            }
            else if (ComplexResource.TYPE_VIDEO.equals(resource.metaType)) {
                result = Consts.formatUrlVideoAnswer(TextUtils.extractDomain(resource.url),
                        resource.size);
            }
            else if (ComplexResource.TYPE_AUDIO.equals(resource.metaType)) {
                result = Consts.formatUrlAudioAnswer(TextUtils.extractDomain(resource.url),
                        resource.size);
            }
            else if (ComplexResource.TYPE_OTHER.equals(resource.metaType)) {
                result = Consts.formatUrlOtherAnswer(TextUtils.extractDomain(resource.url),
                        resource.size);
            }
            else {
                result = Consts.formatUrlFailureAnswer(TextUtils.extractDomain(resource.url),
                        resource.content);
            }
        }
        else {
            result = "TODO";
        }

        return result;
    }
}
