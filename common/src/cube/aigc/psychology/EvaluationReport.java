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

package cube.aigc.psychology;

import cell.util.log.Logger;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.composition.Score;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估报告。
 */
public class EvaluationReport {

    public final static String UNIT = ModelConfig.BAIZE_UNIT;

    private ReportAttribute reportAttribute;

    private List<Representation> representationList;

    public EvaluationReport(ReportAttribute reportAttribute, List<EvaluationFeature> resultList) {
        this.reportAttribute = reportAttribute;
        this.representationList = new ArrayList<>();
        this.build(resultList);
    }

    public ReportAttribute getAuthor() {
        return this.reportAttribute;
    }

    private void build(List<EvaluationFeature> resultList) {
        for (EvaluationFeature result : resultList) {
            Representation representation = this.getRepresentation(result.comment);
            if (null == representation) {
                CommentInterpretation interpretation = Resource.getInstance().getCommentInterpretation(result.comment);
                if (null == interpretation) {
                    // 没有对应的释义
                    Logger.e(this.getClass(), "#build - Can NOT find comment interpretation: " + result.comment.word);
                    continue;
                }

                representation = new Representation(interpretation);
                this.representationList.add(representation);
            }

            if (result.score == Score.High) {
                representation.positive += 1;
            }
            else {
                representation.negative += 1;
            }
        }
    }

    public Representation getRepresentation(Comment comment) {
        for (Representation score : this.representationList) {
            if (score.interpretation.getComment() == comment) {
                return score;
            }
        }
        return null;
    }

    public List<Representation> getRepresentationList() {
        return this.representationList;
    }

    public class Representation {

        public CommentInterpretation interpretation;

        public int positive = 0;

        public int negative = 0;

        public Representation(CommentInterpretation interpretation) {
            this.interpretation = interpretation;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Representation) {
                Representation other = (Representation) obj;
                if (other.interpretation.getComment() == this.interpretation.getComment()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.interpretation.getComment().hashCode();
        }
    }
}
