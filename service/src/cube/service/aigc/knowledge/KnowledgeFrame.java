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

package cube.service.aigc.knowledge;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 知识库框架。
 */
public class KnowledgeFrame {

    public final static String DefaultName = "document";

    public final static String DefaultDescription = "文档库";

    /**
     * Key: 联系人 ID 。
     */
    private ConcurrentHashMap<Long, Frame> frameMap;

    public KnowledgeFrame() {
        this.frameMap = new ConcurrentHashMap<>();
    }

    public KnowledgeBase getKnowledgeBase(long contactId) {
        return this.getKnowledgeBase(contactId, DefaultName);
    }

    public KnowledgeBase getKnowledgeBase(long contactId, String baseName) {
        Frame frame = this.frameMap.get(contactId);
        if (null == frame) {
            return null;
        }

        return frame.getKnowledgeBase(baseName);
    }

    public void putKnowledgeBase(KnowledgeBase knowledgeBase) {
        Frame frame = this.frameMap.get(knowledgeBase.getAuthToken().getContactId());
        if (null == frame) {
            frame = new Frame(knowledgeBase.getAuthToken().getContactId());
            this.frameMap.put(knowledgeBase.getAuthToken().getContactId(), frame);
        }
        frame.putKnowledgeBase(knowledgeBase);
    }

    private class Frame {

        private final long contactId;

        /**
         * Key: 知识库名。
         */
        private ConcurrentHashMap<String, KnowledgeBase> knowledgeMap;

        private Frame(long contactId) {
            this.contactId = contactId;
            this.knowledgeMap = new ConcurrentHashMap<>();
        }

        public KnowledgeBase getKnowledgeBase(String name) {
            return this.knowledgeMap.get(name);
        }

        public void putKnowledgeBase(KnowledgeBase knowledgeBase) {
            this.knowledgeMap.put(knowledgeBase.getName(), knowledgeBase);
        }
    }
}
