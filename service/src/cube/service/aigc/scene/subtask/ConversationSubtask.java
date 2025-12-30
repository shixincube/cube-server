/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene.subtask;

import cell.util.log.Logger;
import cube.aigc.Consts;
import cube.aigc.ModelConfig;
import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.Resource;
import cube.aigc.psychology.composition.ConversationContext;
import cube.aigc.psychology.composition.ConversationRelation;
import cube.aigc.psychology.composition.Subtask;
import cube.common.Language;
import cube.common.entity.*;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.GenerateTextListener;
import cube.util.FileType;
import cube.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class ConversationSubtask {

    protected final static String CORPUS = "conversation";

    protected final static String CORPUS_PROMPT = "prompt";

    private final static String JUMP_POLISH = "润色";

    public final Subtask name;

    protected AIGCService service;

    protected AIGCChannel channel;

    protected String query;

    protected ComplexContext context;

    protected ConversationRelation relation;

    protected ConversationContext convCtx;

    protected GenerateTextListener listener;

    public ConversationSubtask(Subtask name, AIGCService service, AIGCChannel channel, String query,
                               ComplexContext context, ConversationRelation relation, ConversationContext convCtx,
                               GenerateTextListener listener) {
        this.name = name;
        this.service = service;
        this.channel = channel;
        this.query = query;
        this.context = context;
        this.relation = relation;
        this.convCtx = convCtx;
        this.listener = listener;
    }

    protected FileLabel checkFileLabel(FileLabel fileLabel) {
        if (null == fileLabel) {
            return null;
        }
        FileLabel local = this.service.getFile(fileLabel.getDomain().getName(), fileLabel.getFileCode());
        if (null == local) {
            return null;
        }
        return local;
    }

    protected String polish(String text) {
        AIGCUnit unit = this.service.selectIdleUnitByName(ModelConfig.BAIZE_X_UNIT);
        if (null == unit) {
            unit = this.service.selectIdleUnitByName(ModelConfig.BAIZE_UNIT);
            if (null == unit) {
                unit = this.service.selectIdleUnitByName(ModelConfig.BAIZE_NEXT_UNIT);
                if (null == unit) {
                    Logger.d(this.getClass(), "#polish - Can NOT find idle unit");
                    return text;
                }
            }
        }

        String prompt = String.format(Resource.getInstance().getCorpus(CORPUS_PROMPT, "FORMAT_POLISH"), text);
        GeneratingRecord result = this.service.syncGenerateText(unit, prompt, null, null, null);
        if (null == result) {
            return text;
        }
        int pos = result.answer.indexOf("\n");
        if (pos > 0) {
            String substring = result.answer.substring(0, pos);
            if (substring.contains(JUMP_POLISH)) {
                result.answer = result.answer.substring(pos + 1);
            }
        }
        while (result.answer.startsWith("\n")) {
            result.answer = result.answer.substring(1);
        }
        return result.answer;
    }

    public String fastPolish(String text) {
        AIGCUnit unit = this.service.selectIdleUnitByName(ModelConfig.BAIZE_UNIT);
        if (null == unit) {
            unit = this.service.selectIdleUnitByName(ModelConfig.BAIZE_X_UNIT);
            if (null == unit) {
                Logger.d(this.getClass(), "#fastPolish - Can NOT find unit");
                return text;
            }
        }

        String prompt = String.format(Resource.getInstance().getCorpus(CORPUS_PROMPT, "FORMAT_POLISH"), text);
        GeneratingRecord result = this.service.syncGenerateText(unit, prompt, null, null, null);
        if (null == result) {
            return text;
        }
        int pos = result.answer.indexOf("\n");
        String substring = result.answer.substring(0, pos);
        if (substring.contains(JUMP_POLISH)) {
            result.answer = result.answer.substring(pos + 1);
        }
        while (result.answer.startsWith("\n")) {
            result.answer = result.answer.substring(1);
        }
        return result.answer;
    }

    public String infer(String prompt) {
        // 由于算力有限，根据提示词长度选择单元
        AIGCUnit unit = prompt.length() > 2000 ? this.service.selectIdleUnitByName(ModelConfig.BAIZE_X_UNIT) :
                this.service.selectIdleUnitByName(ModelConfig.BAIZE_NEXT_UNIT);
        if (null == unit) {
            unit = this.service.selectUnitByName(ModelConfig.BAIZE_X_UNIT);
            if (null == unit) {
                Logger.w(this.getClass(), "#infer - Can NOT find unit");
                return null;
            }
        }

        GeneratingRecord result = this.service.syncGenerateText(unit, prompt, null, null, null);
        if (null == result) {
            Logger.w(this.getClass(), "#infer - syncGenerateText failed");
            return null;
        }
        return result.answer;
    }

    protected Attribute extractAttribute(String query) {
        int age = 0;
        String gender = "";

        List<String> words = this.service.segmentation(query);
        int ageIndex = -1;
        int genderIndex = -1;
        for (int i = 0; i < words.size(); ++i) {
            String word = words.get(i);
            if (Consts.contains(word, Consts.AGE_SYNONYMS)) {
                ageIndex = i;
            }
            else if (Consts.contains(word, Consts.GENDER_SYNONYMS)) {
                genderIndex = i;
            }
        }

        if (ageIndex >= 0) {
            int start = Math.max(0, ageIndex - 2);
            for (int i = start; i < words.size(); ++i) {
                String word = words.get(i);
                word = word.trim();
                if (TextUtils.isNumeric(word)) {
                    try {
                        age = Integer.parseInt(word);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        else {
            for (int i = words.size() - 1; i >= 0; --i) {
                String word = words.get(i);
                word = word.trim();
                if (TextUtils.isNumeric(word)) {
                    try {
                        int value = Integer.parseInt(word);
                        if (value >= Attribute.MIN_AGE && value <= Attribute.MAX_AGE) {
                            age = value;
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (genderIndex >= 0) {
            int start = Math.max(0, genderIndex - 2);
            for (int i = start; i < words.size(); ++i) {
                String word = words.get(i);
                word = word.trim();
                if (word.equals("男") || word.equals("男性") || word.equalsIgnoreCase("male")) {
                    gender = "male";
                    break;
                }
                else if (word.equals("女") || word.equals("女性") || word.equalsIgnoreCase("female")) {
                    gender = "female";
                    break;
                }
            }
        }
        else {
            for (String word : words) {
                if (word.equals("男") || word.equals("男性") || word.equalsIgnoreCase("male")) {
                    gender = "male";
                    break;
                }
                else if (word.equals("女") || word.equals("女性") || word.equalsIgnoreCase("female")) {
                    gender = "female";
                    break;
                }
            }
        }

        return new Attribute(gender, age, Language.Chinese, false);
    }

    private List<FileLabel> checkFileLabels(List<FileLabel> fileLabels) {
        if (null == fileLabels || fileLabels.isEmpty()) {
            return null;
        }

        List<FileLabel> result = new ArrayList<>();
        for (FileLabel fileLabel : fileLabels) {
            FileLabel local = this.service.getFile(fileLabel.getDomain().getName(), fileLabel.getFileCode());
            if (null != local) {
                // 判断文件类型
                if (local.getFileType() == FileType.JPEG ||
                        local.getFileType() == FileType.PNG ||
                        local.getFileType() == FileType.BMP) {
                    result.add(local);
                }
            }
        }

        return (result.isEmpty()) ? null : result;
    }

    protected String filterSecondPerson(String text) {
        String result = text.replaceAll("各位", "您");
        return result.replaceAll("受测人", "您");
    }

    protected String filterFirstPerson(String text) {
        return text.replaceAll("我们", "我");
    }

    public abstract AIGCStateCode execute(Subtask roundSubtask);
}
