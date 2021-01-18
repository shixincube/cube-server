/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cube.common.action.FileProcessorAction;
import cube.core.Kernel;

import java.util.concurrent.ExecutorService;

/**
 * 文件处理服务的 Cellet 服务。
 */
public class FileProcessorServiceCellet extends Cellet {

    private FileProcessorService service = null;

    private ExecutorService executor = null;

    public FileProcessorServiceCellet() {
        super(FileProcessorService.NAME);
    }

    public FileProcessorService getService() {
        return this.service;
    }

    @Override
    public boolean install() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(16);

        this.service = new FileProcessorService(this.executor);

        Kernel kernel = (Kernel) this.nucleus.getParameter("kernel");
        kernel.installModule(this.getName(), this.service);

        return true;
    }

    @Override
    public void uninstall() {
        Kernel kernel = (Kernel) this.nucleus.getParameter("kernel");
        kernel.uninstallModule(this.getName());

        this.executor.shutdown();
    }

    @Override
    public void onListened(TalkContext talkContext, Primitive primitive) {
        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (FileProcessorAction.DetectObject.name.equals(action)) {
        }
    }
}
