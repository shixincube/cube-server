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

package cube.service.filestorage;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cube.common.action.FileStorageAction;
import cube.core.Kernel;
import cube.service.filestorage.task.*;

import java.util.concurrent.ExecutorService;

/**
 * 文件存储器 Cellet 服务单元。
 */
public class FileStorageServiceCellet extends Cellet {

    private ExecutorService executor = null;

    public FileStorageServiceCellet() {
        super(FileStorageService.NAME);
    }

    @Override
    public boolean install() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(16);

        Kernel kernel = (Kernel) this.nucleus.getParameter("kernel");
        kernel.installModule(this.getName(), new FileStorageService(this.executor));

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

        if (FileStorageAction.GetFile.name.equals(action)) {
            this.executor.execute(new GetFileTask(this, talkContext, primitive));
        }
        else if (FileStorageAction.PutFile.name.equals(action)) {
            this.executor.execute(new PutFileTask(this, talkContext, primitive));
        }
        else if (FileStorageAction.GetRoot.name.equals(action)) {
            this.executor.execute(new GetRootDirectoryTask(this, talkContext, primitive));
        }
        else if (FileStorageAction.ListDirs.name.equals(action)) {
            this.executor.execute(new ListDirectoriesTask(this, talkContext, primitive));
        }
        else if (FileStorageAction.ListFiles.name.equals(action)) {
            this.executor.execute(new ListFilesTask(this, talkContext, primitive));
        }
        else if (FileStorageAction.NewDir.name.equals(action)) {
            this.executor.execute(new NewDirectoryTask(this, talkContext, primitive));
        }
        else if (FileStorageAction.DeleteDir.name.equals(action)) {
            this.executor.execute(new DeleteDirectoryTask(this, talkContext, primitive));
        }
    }

    @Override
    public void onListened(TalkContext talkContext, PrimitiveInputStream stream) {
        this.executor.execute(new WriteFileTask(this, talkContext, stream));
    }
}
