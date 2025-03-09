/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.common.entity;

import cube.common.JSONable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 能力集。
 */
public class AICapability implements JSONable {

    public final static String MultimodalTask = "Multimodal";

    public final static String NaturalLanguageProcessingTask = "NaturalLanguageProcessing";

    public final static String ComputerVisionTask = "ComputerVision";

    public final static String AudioProcessingTask = "AudioProcessing";

    public final static String DataProcessingTask = "DataProcessing";

    public final static String OtherProcessingTask = "OtherProcessing";

    private String name;

    private String task;

    private List<String> subtasks;

    private String description;

    public AICapability(String name, String task, List<String> subtasks, String description) {
        this.name = name;
        this.task = task;
        this.subtasks = subtasks;
        this.description = description;
    }

    public AICapability(JSONObject json) {
        this.name = json.getString("name");
        this.task = json.getString("task");

        this.subtasks = new ArrayList<>();
        if (json.has("subtask")) {
            this.subtasks.add(json.getString("subtask"));
        }
        else if (json.has("subtasks")) {
            JSONArray array = json.getJSONArray("subtasks");
            for (int i = 0; i < array.length(); ++i) {
                this.subtasks.add(array.getString(i));
            }
        }

        this.description = json.getString("description");
    }

    public String getName() {
        return this.name;
    }

    public String getTask() {
        return this.task;
    }

    public String getPrimarySubtask() {
        return this.subtasks.get(0);
    }

    public boolean containsSubtask(String subtask) {
        return this.subtasks.contains(subtask);
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("task", this.task);

        if (this.subtasks.size() == 1) {
            json.put("subtask", this.subtasks.get(0));
        }
        else {
            JSONArray array = new JSONArray();
            for (String subtask : this.subtasks) {
                array.put(subtask);
            }
            json.put("subtasks", array);
        }

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

        public final static String Segmentation = "Segmentation";

        public final static String TextClassification = "TextClassification";

        public final static String TokenClassification = "TokenClassification";

        public final static String TableQuestionAnswering = "TableQuestionAnswering";

        public final static String QuestionAnswering = "QuestionAnswering";

        public final static String ZeroShotClassification = "ZeroShotClassification";

        public final static String Translation = "Translation";

        public final static String Summarization = "Summarization";

        public final static String Conversational = "Conversational";

//        public final static String ImprovedConversational = "ImprovedConversational";

        public final static String TextGeneration = "TextGeneration";

        public final static String Text2TextGeneration = "Text2TextGeneration";

        public final static String FillMask = "FillMask";

        public final static String SentenceSimilarity = "SentenceSimilarity";

        public final static String SentimentAnalysis = "SentimentAnalysis";

        public final static String ExtractKeywords = "ExtractKeywords";

        public final static String MultiTask = "MultiTask";

        public final static String KnowledgeComprehension = "KnowledgeComprehension";

        public final static String SemanticSearch = "SemanticSearch";

        public final static String ParaphraseMining = "ParaphraseMining";

        public final static String RetrieveReRank = "RetrieveReRank";

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


    public class DataProcessing {

        public final static String ExtractURLContent = "ExtractURLContent";

        private DataProcessing() {
        }
    }


    public class OtherProcessing {

        public final static String Psychology = "Psychology";

        private OtherProcessing() {
        }
    }
}
