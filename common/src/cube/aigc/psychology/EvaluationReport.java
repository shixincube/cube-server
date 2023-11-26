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

import cube.aigc.PromptChaining;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估报告。
 */
public class EvaluationReport {

    private List<ReportScore> reportScoreList;

    public EvaluationReport(List<Evaluation.Result> resultList) {
        this.reportScoreList = new ArrayList<>();
        this.build(resultList);
    }

    private void build(List<Evaluation.Result> resultList) {
        for (Evaluation.Result result : resultList) {
            CommentInterpretation interpretation = Resource.getInstance().getCommentInterpretation(result.comment);

        }
    }

    public ReportScore getReportScore(Comment comment) {
        for (ReportScore score : this.reportScoreList) {

        }

        return null;
    }

    public PromptChaining outputFamilyRelationships() {
        return null;
    }



    public class ReportScore {

        public CommentInterpretation comment;

        public int positive = 0;

        public int negative = 0;

        public ReportScore(CommentInterpretation comment) {
            this.comment = comment;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ReportScore) {
                ReportScore score = (ReportScore) obj;
                if (score.comment.getComment() == this.comment.getComment()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.comment.getComment().hashCode();
        }
    }
}
