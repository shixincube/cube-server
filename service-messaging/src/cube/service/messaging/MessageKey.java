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

package cube.service.messaging;

/**
 * 消息记录的主键。
 */
public class MessageKey {

    protected Long contactId;

    protected Long messageId;

    protected int hash = 0;

    protected MessageKey(Long contactId, Long messageId) {
        this.contactId = contactId;
        this.messageId = messageId;
        this.hash = contactId.hashCode() * 3 + messageId.hashCode() * 7;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof MessageKey) {
            MessageKey other = (MessageKey) object;
            if (other.contactId.equals(this.contactId) && other.messageId.equals(this.messageId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }
}
