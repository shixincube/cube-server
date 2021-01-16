/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.service.fileprocessor;

import cell.api.Speakable;
import cell.api.TalkListener;
import cell.api.TalkService;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkError;

import java.io.File;

/**
 * 计算机视觉系统连接器。
 */
public class CVConnector implements TalkListener {

    private String celletName;

    private TalkService talkService;

    private String host;

    private int port;

    public CVConnector(String celletName, TalkService talkService) {
        this.celletName = celletName;
        this.talkService = talkService;
    }

    public void start(String host, int port) {
        this.host = host;
        this.port = port;

        this.talkService.setListener(this.celletName, this);
        this.talkService.call(host, port);
    }

    public void stop() {
        this.talkService.hangup(this.host, this.port, false);
        this.talkService.removeListener(this.celletName);
    }

    public void detectObjects(File file, String fileCode, CVCallback callback) {

//        PrimitiveOutputStream stream = this.talkService.speakStream(this.celletName, fileLabel.getFileCode());
    }

    @Override
    public void onListened(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onListened(Speakable speakable, String cellet, PrimitiveInputStream primitiveInputStream) {

    }

    @Override
    public void onSpoke(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onAck(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onSpeakTimeout(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onContacted(Speakable speakable) {

    }

    @Override
    public void onQuitted(Speakable speakable) {

    }

    @Override
    public void onFailed(Speakable speakable, TalkError talkError) {

    }
}
