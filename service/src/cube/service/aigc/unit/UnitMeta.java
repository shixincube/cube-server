/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.entity.RetrieveReRankResult;
import cube.service.aigc.AIGCService;
import cube.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class UnitMeta {

    protected final AIGCService service;

    public final AIGCUnit unit;

    public UnitMeta(AIGCService service, AIGCUnit unit) {
        this.service = service;
        this.unit = unit;
    }

    protected List<RetrieveReRankResult> analyseFiles(List<FileLabel> fileLabels, String query) {
        List<RetrieveReRankResult> result = new ArrayList<>();
        if (fileLabels.isEmpty()) {
            return result;
        }

        for (FileLabel fileLabel : fileLabels) {
            RetrieveReRankResult retrieveReRankResult = this.service.syncRetrieveReRank(fileLabel, query);
            if (null == retrieveReRankResult) {
                continue;
            }

            result.add(retrieveReRankResult);
        }
        return result;
    }

//        protected List<String> readFileContent(List<FileLabel> fileLabels) {
//            List<String> result = new ArrayList<>();
//
//            AbstractModule fileStorage = getKernel().getModule("FileStorage");
//            for (FileLabel fileLabel : fileLabels) {
//                if (fileLabel.getFileType() == FileType.TEXT
//                        || fileLabel.getFileType() == FileType.TXT
//                        || fileLabel.getFileType() == FileType.MD
//                        || fileLabel.getFileType() == FileType.LOG) {
//                    String fullpath = fileStorage.notify(new LoadFile(fileLabel.getDomain().getName(), fileLabel.getFileCode()));
//                    if (null == fullpath) {
//                        Logger.w(this.getClass(), "#readFileContent - Load file error: " + fileLabel.getFileCode());
//                        continue;
//                    }
//
//                    try {
//                        List<String> lines = Files.readAllLines(Paths.get(fullpath));
//                        for (String text : lines) {
//                            if (text.trim().length() < 3) {
//                                continue;
//                            }
//                            result.add(text);
//                        }
//                    } catch (Exception e) {
//                        Logger.w(this.getClass(), "#readFileContent - Read file error: " + fullpath);
//                    }
//                }
//                else {
//                    Logger.w(this.getClass(), "#readFileContent - File type error: " + fileLabel.getFileType().getMimeType());
//                }
//            }
//
//            return result;
//        }

    /**
     * 计算文本的 Token 列表。
     *
     * @param text
     * @return
     */
    public List<String> calcTokens(String text) {
        List<String> tokens = this.service.getTokenizer().sentenceProcess(text);
        tokens.removeIf(s -> !TextUtils.isChineseWord(s) && !TextUtils.isWord(s));
        return tokens;
    }

    public abstract void process();
}
