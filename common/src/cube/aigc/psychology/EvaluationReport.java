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
import cube.aigc.psychology.composition.Tendency;

import java.util.*;

/**
 * 评估报告。
 */
public class EvaluationReport {

    public final static String UNIT = ModelConfig.BAIZE_UNIT;

    private Attribute attribute;

    private List<Representation> representationList;

    private int representationTopN;

    public EvaluationReport(Attribute attribute, EvaluationFeature evaluationFeature) {
        this(attribute, Collections.singletonList((evaluationFeature)));
    }

    public EvaluationReport(Attribute attribute, List<EvaluationFeature> evaluationFeatureList) {
        this.attribute = attribute;
        this.representationList = new ArrayList<>();
        this.representationTopN = 5;
        this.build(evaluationFeatureList);
    }

    public void setTopN(int value) {
        this.representationTopN = value;
    }

    public boolean isEmpty() {
        return this.representationList.isEmpty();
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    private void build(List<EvaluationFeature> resultList) {
        for (EvaluationFeature result : resultList) {
            for (EvaluationFeature.Feature feature : result.getFeatures()) {
                Representation representation = this.getRepresentation(feature.comment);
                if (null == representation) {
                    KnowledgeStrategy interpretation = Resource.getInstance().getCommentInterpretation(feature.comment);
                    if (null == interpretation) {
                        // 没有对应的释义
                        Logger.e(this.getClass(), "#build - Can NOT find comment interpretation: " + feature.comment.word);
                        continue;
                    }

                    representation = new Representation(interpretation);
                    this.representationList.add(representation);
                }

                if (feature.tendency == Tendency.Positive) {
                    representation.positiveCorrelation += 1;
                }
                else if (feature.tendency == Tendency.Negative) {
                    representation.negativeCorrelation += 1;
                }
            }
        }
    }

    public Representation getRepresentation(Comment comment) {
        for (Representation representation : this.representationList) {
            if (representation.knowledgeStrategy.getComment() == comment) {
                return representation;
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

        List<Representation> result = new ArrayList<>(this.representationTopN);
        for (int i = 0, len = Math.min(this.representationTopN, this.representationList.size()); i < len; ++i) {
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
