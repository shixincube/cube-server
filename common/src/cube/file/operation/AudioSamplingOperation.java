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

package cube.file.operation;

import cube.file.AudioOperation;
import cube.util.FileType;
import org.json.JSONObject;

/**
 * 音频文件重新采样。
 */
public class AudioSamplingOperation extends AudioOperation {

    public final static String Operation = "AudioSampling";

    private final static String KEY_OUTPUT_TYPE = "outputType";

    private final static String KEY_CHANNEL = "channel";

    private final static String KEY_RATE = "rate";

    public FileType outputType;

    public int channel = 1;

    public int rate = 16000;

    public AudioSamplingOperation(int channel, int rate) {
        super();
        this.outputType = FileType.WAV;
        this.channel = channel;
        this.rate = rate;
    }

    public AudioSamplingOperation(int channel, int rate, FileType outputType) {
        super();
        this.outputType = outputType;
        this.channel = channel;
        this.rate = rate;
    }

    public AudioSamplingOperation(JSONObject json) {
        super();
        this.outputType = FileType.matchExtension(json.getString(KEY_OUTPUT_TYPE));
        this.channel = json.getInt(KEY_CHANNEL);
        this.rate = json.getInt(KEY_RATE);
    }

    @Override
    public String getOperation() {
        return AudioSamplingOperation.Operation;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put(KEY_OUTPUT_TYPE, this.outputType.getPreferredExtension());
        json.put(KEY_CHANNEL, this.channel);
        json.put(KEY_RATE, this.rate);
        return json;
    }
}
