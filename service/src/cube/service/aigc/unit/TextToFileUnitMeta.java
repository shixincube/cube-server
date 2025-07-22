/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.unit;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.log.Logger;
import cube.aigc.Consts;
import cube.aigc.ModelConfig;
import cube.common.Packet;
import cube.common.action.FileProcessorAction;
import cube.common.entity.AIGCChannel;
import cube.common.entity.AIGCUnit;
import cube.common.entity.FileLabel;
import cube.common.entity.GeneratingRecord;
import cube.common.state.AIGCStateCode;
import cube.service.aigc.AIGCService;
import cube.service.aigc.listener.TextToFileListener;
import cube.util.FileType;
import cube.util.FileUtils;
import cube.util.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TextToFileUnitMeta extends UnitMeta {

    private final long sn;

    private AIGCChannel channel;

    private String text;

    private List<FileLabel> sources;

    private TextToFileListener listener;

    public TextToFileUnitMeta(AIGCService service, AIGCUnit unit, AIGCChannel channel, String text,
                              List<FileLabel> sources, TextToFileListener listener) {
        super(service, unit);
        this.sn = Utils.generateSerialNumber();
        this.channel = channel;
        this.text = text;
        this.sources = new ArrayList<>(sources);
        this.listener = listener;
    }

    @Override
    public void process() {
        this.service.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                listener.onProcessing(channel);
            }
        });

        int promptLimit = ModelConfig.getPromptLengthLimit(this.unit.getCapability().getName());

        List<FileLabel> queryFiles = new ArrayList<>();
        StringBuilder answerBuf = new StringBuilder("已处理文件");
        long processedSize = 0;
        long generatingSize = 0;

        FileType fileType = FileType.UNKNOWN;
        JSONArray files = new JSONArray();
        for (FileLabel fileLabel : this.sources) {
            fileType = fileLabel.getFileType();
            if (fileType == FileType.XLSX || fileType == FileType.XLS) {
                files.put(fileLabel.toJSON());
                queryFiles.add(fileLabel);

                answerBuf.append("“**").append(fileLabel.getFileName()).append("**”，");
                processedSize += fileLabel.getFileSize();
            }
        }
        answerBuf.delete(answerBuf.length() - 1, answerBuf.length());
        answerBuf.append("。");

        ActionDialect dialect = null;
        if (fileType == FileType.XLSX || fileType == FileType.XLS) {
            JSONObject data = new JSONObject();
            data.put("files", files);
            Packet request = new Packet(FileProcessorAction.ReadExcel.name, data);
            dialect = this.service.getCellet().transmit(this.unit.getContext(), request.toDialect(), 60 * 1000, this.sn);
            if (null == dialect) {
                Logger.w(this.getClass(), "File processor service error");
                // 回调错误
                this.listener.onFailed(this.channel, AIGCStateCode.UnitError);
                return;
            }
        }
        else {
            Logger.w(this.getClass(), "Unsupported file type： " + fileType.getPreferredExtension());
            // 回调错误
            this.listener.onFailed(this.channel, AIGCStateCode.IllegalOperation);
            return;
        }

        Packet response = new Packet(dialect);
        JSONObject payload = Packet.extractDataPayload(response);
        JSONArray fileResult = payload.getJSONArray("result");
        if (fileResult.length() == 0) {
            Logger.w(this.getClass(), "File error");
            // 回调错误
            this.listener.onFailed(this.channel, AIGCStateCode.FileError);
            return;
        }

        String notice = null;
        GeneratingRecord result = new GeneratingRecord(ModelConfig.BAIZE_UNIT, this.text, answerBuf.toString());
        result.queryFileLabels = queryFiles;

        for (int i = 0; i < fileResult.length(); ++i) {
            JSONObject fileData = fileResult.getJSONObject(i);
            JSONObject fileLabelJson = fileData.getJSONObject("fileLabel");
            FileLabel fileLabel = new FileLabel(fileLabelJson);
            JSONArray sheets = fileData.getJSONArray("sheets");
            for (int n = 0; n < sheets.length(); ++n) {
                JSONObject sheetJson = sheets.getJSONObject(n);
                String name = sheetJson.getString("name");
                String content = sheetJson.getString("content");

                if (content.length() > promptLimit) {
                    Logger.w(this.getClass(), "#process - Content length exceeded the limit: " +
                            content.length() + "/" + promptLimit);
                    continue;
                }

                StringBuilder prompt = new StringBuilder();
                prompt.append("已知以下表格数据：\n\n");
                prompt.append("表格名称：").append(name).append("\n\n");
                prompt.append("表格数据内容：\n\n").append(content);
                prompt.append("\n\n");
                prompt.append(String.format(Consts.PROMPT_SUFFIX_FORMAT, this.text));

                GeneratingRecord generating = this.service.syncGenerateText(ModelConfig.BAIZE_X_UNIT, prompt.toString(),
                        null, null, null);
                if (null == generating) {
                    Logger.w(this.getClass(), "#process - Generating failed: " + fileLabel.getFileCode());
                    continue;
                }

                String answer = generating.answer;

                String tmpNotice = this.extractNotice(answer);
                String csv = TextUtils.extractMarkdownTable(answer);

                if (null != tmpNotice) {
                    notice = tmpNotice;
                }

                if (null != csv) {
                    generatingSize += csv.length();

                    // 文件码
                    String tmpFileCode = FileUtils.makeFileCode(fileLabel.getFileCode(), channel.getAuthToken().getDomain(), name);
                    Path path = Paths.get(this.service.workingPath.getAbsolutePath(), tmpFileCode + ".csv");
                    try {
                        // 写入文件
                        Files.write(path, csv.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        Logger.e(this.getClass(), "#process - File write failed: " + path.toString(), e);
                    }

                    FileLabel newFileLabel = this.service.saveFile(this.channel.getAuthToken(),
                            tmpFileCode, path.toFile(), name + ".csv", true);
                    if (null != newFileLabel) {
                        result.addAnswerFileLabel(newFileLabel);
                    }
                    else {
                        Logger.e(this.getClass(), "#process - Save file failed: " + path.toString());
                    }
                }
                else {
                    Logger.e(this.getClass(), "#process - Extract markdown table failed: " + fileLabel.getFileCode());
                }
            }
        }

        if (null != result.answerFileLabels) {
            answerBuf.append("处理的数据大小合计 ").append(FileUtils.scaleFileSize(processedSize)).append(" 。");
            answerBuf.append("生成").append(result.answerFileLabels.size()).append("个文件，合计 ");
            answerBuf.append(FileUtils.scaleFileSize(generatingSize)).append(" 。\n");
        }
        else {
            answerBuf.append("处理的数据大小合计 ").append(FileUtils.scaleFileSize(processedSize)).append(" 。");
            answerBuf.append("但是未能正确读取数据，建议检查一下待处理文件。");
        }

        if (null != notice) {
            answerBuf.append("\n").append(notice);
        }

        result.answer = answerBuf.toString();
        listener.onCompleted(result);
    }

    private String extractNotice(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.length() < 2) {
                continue;
            }

            if (line.contains("注意")) {
                return line;
            }
        }
        return null;
    }
}
