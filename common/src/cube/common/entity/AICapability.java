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

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONObject;

/**
 * AI 能力集。
 */
public class AICapability implements JSONable {

    public final static String MultimodalTask = "Multimodal";

    public final static String NaturalLanguageProcessingTask = "NaturalLanguageProcessing";

    public final static String ComputerVisionTask = "ComputerVision";

    public final static String AudioProcessingTask = "AudioProcessing";

    private String name;

    private String task;

    private String subtask;

    private String description;

    public AICapability(String name, String task, String subtask, String description) {
        this.name = name;
        this.task = task;
        this.subtask = subtask;
        this.description = description;
    }

    public AICapability(JSONObject json) {
        this.name = json.getString("name");
        this.task = json.getString("task");
        this.subtask = json.getString("subtask");
        this.description = json.getString("description");
    }

    public String getName() {
        return this.name;
    }

    public String getTask() {
        return this.task;
    }

    public String getSubtask() {
        return this.subtask;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("task", this.task);
        json.put("subtask", this.subtask);
        json.put("description", this.description);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }


    public class Multimodal {

        public final static String TextToImage = "TextToImage";

        public final static String ImageToText = "ImageToText";

        public final static String TextToVideo = "TextToVideo";

        public final static String VisualQuestionAnswering = "VisualQuestionAnswering";

        public final static String DocumentQuestionAnswering = "DocumentQuestionAnswering";

        public final static String FeatureExtraction = "FeatureExtraction";

        private Multimodal() {
        }
    }


    public class NaturalLanguageProcessing {

        public final static String TextClassification = "TextClassification";

        public final static String TokenClassification = "TokenClassification";

        public final static String TableQuestionAnswering = "TableQuestionAnswering";

        public final static String QuestionAnswering = "QuestionAnswering";

        public final static String ZeroShotClassification = "ZeroShotClassification";

        public final static String Translation = "Translation";

        public final static String Summarization = "Summarization";

        public final static String Conversational = "Conversational";

        public final static String ImprovedConversational = "ImprovedConversational";

        public final static String TextGeneration = "TextGeneration";

        public final static String Text2TextGeneration = "Text2TextGeneration";

        public final static String FillMask = "FillMask";

        public final static String SentenceSimilarity = "SentenceSimilarity";

        public final static String SentimentAnalysis = "SentimentAnalysis";

        private NaturalLanguageProcessing() {
        }
    }


    public class ComputerVision {

        public final static String DepthEstimation = "DepthEstimation";

        public final static String ImageClassification = "ImageClassification";

        public final static String ObjectDetection = "ObjectDetection";

        public final static String ImageSegmentation = "ImageSegmentation";

        public final static String ImageToImage = "ImageToImage";

        public final static String UnconditionalImageGeneration = "UnconditionalImageGeneration";

        public final static String VideoClassification = "VideoClassification";

        public final static String ZeroShotImageClassification = "ZeroShotImageClassification";

        private ComputerVision() {
        }
    }


    public class AudioProcessing {

        public final static String TextToSpeech = "TextToSpeech";

        public final static String AutomaticSpeechRecognition = "AutomaticSpeechRecognition";

        public final static String AudioToAudio = "AudioToAudio";

        public final static String AudioClassification = "AudioClassification";

        public final static String VoiceActivityDetection = "VoiceActivityDetection";

        private AudioProcessing() {
        }
    }
}
