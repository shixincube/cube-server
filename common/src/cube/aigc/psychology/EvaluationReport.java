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
import java.util.Comparator;
import java.util.List;

/**
 * 评估报告。
 */
public class EvaluationReport {

    public final static String UNIT = ModelConfig.BAIZE_UNIT;

    private Attribute attribute;

    private List<Representation> representationList;

    private int topN;

    public EvaluationReport(Attribute attribute, List<EvaluationFeature> resultList) {
        this.attribute = attribute;
        this.representationList = new ArrayList<>();
        this.topN = 5;
        this.build(resultList);
    }

    public void setTopN(int value) {
        this.topN = value;
    }

    public boolean isEmpty() {
        return this.representationList.isEmpty();
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    private void build(List<EvaluationFeature> resultList) {
        for (EvaluationFeature result : resultList) {
            Representation representation = this.getRepresentation(result.comment);
            if (null == representation) {
                KnowledgeStrategy interpretation = Resource.getInstance().getCommentInterpretation(result.comment);
                if (null == interpretation) {
                    // 没有对应的释义
                    Logger.e(this.getClass(), "#build - Can NOT find comment interpretation: " + result.comment.word);
                    continue;
                }

                representation = new Representation(interpretation);
                this.representationList.add(representation);
            }

            if (result.score == Score.High) {
                representation.positiveCorrelation += 1;
            }
            else if (result.score == Score.Low) {
                representation.negativeCorrelation += 1;
            }
        }
    }

    public Representation getRepresentation(Comment comment) {
        for (Representation score : this.representationList) {
            if (score.knowledgeStrategy.getComment() == comment) {
                return score;
            }
        }
        return null;
    }

    public List<Representation> getRepresentationList() {
        return this.representationList;
    }

    public List<Representation> getRepresentationListOrderByCorrelation() {
        this.representationList.sort(new Comparator<Representation>() {
            @Override
            public int compare(Representation representation1, Representation representation2) {
                int score1 = representation1.positiveCorrelation + representation1.negativeCorrelation;
                int score2 = representation2.positiveCorrelation + representation2.negativeCorrelation;
                return score2 - score1;
            }
        });

        List<Representation> result = new ArrayList<>(this.topN);
        for (int i = 0, len = Math.min(this.topN, this.representationList.size()); i < len; ++i) {
            result.add(this.representationList.get(i));
        }
        return result;
    }

    public class Representation {

        public KnowledgeStrategy knowledgeStrategy;

        public int positiveCorrelation = 0;

        public int negativeCorrelation = 0;

        public Representation(KnowledgeStrategy knowledgeStrategy) {
            this.knowledgeStrategy = knowledgeStrategy;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Representation) {
                Representation other = (Representation) obj;
                if (other.knowledgeStrategy.getComment() == this.knowledgeStrategy.getComment()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.knowledgeStrategy.getComment().hashCode();
        }
    }
}
