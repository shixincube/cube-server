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

package cube.aigc.psychology;

import org.json.JSONObject;

/**
 * 词描述。
 */
public class CommentInterpretation {

    private Comment comment;

    private String interpretation;

    private String advise;

    private String remark;

    public CommentInterpretation(JSONObject json) {
        this.comment = Comment.parse(json.getString("comment"));
        if (json.has("interpretation")) {
            this.interpretation = json.getString("interpretation");
        }
        if (json.has("advise")) {
            this.advise = json.getString("advise");
        }
        if (json.has("remark")) {
            this.remark = json.getString("remark");
        }
    }

    public Comment getComment() {
        return this.comment;
    }

    public String getInterpretation() {
        return this.interpretation;
    }

    public String getAdvise() {
        return this.advise;
    }

    public String getRemark() {
        return this.remark;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CommentInterpretation) {
            CommentInterpretation other = (CommentInterpretation) obj;
            if (other.comment == this.comment) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.comment.hashCode();
    }
}