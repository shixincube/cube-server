/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.filestorage;

import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.CachedQueueExecutor;
import cube.common.action.FileStorageAction;
import cube.core.AbstractCellet;
import cube.core.Kernel;
import cube.service.filestorage.task.*;

import java.util.concurrent.ExecutorService;

/**
 * 文件存储器 Cellet 服务单元。
 */
public class FileStorageServiceCellet extends AbstractCellet {

    private ExecutorService executor = null;

    public FileStorageServiceCellet() {
        super(FileStorageService.NAME);
    }

    @Override
    public boolean install() {
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(16);

        Kernel kernel = (Kernel) this.nucleus.getParameter("kernel");
        kernel.installModule(this.getName(), new FileStorageService(this, this.executor));

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
        super.onListened(talkContext, primitive);

        ActionDialect dialect = DialectFactory.getInstance().createActionDialect(primitive);
        String action = dialect.getName();

        if (FileStorageAction.GetFile.name.equals(action)) {
            this.executor.execute(new GetFileTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.PutFile.name.equals(action)) {
            this.executor.execute(new PutFileTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.ListFileLabels.name.equals(action)) {
            this.executor.execute(new ListFileLabelsTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.GetRoot.name.equals(action)) {
            this.executor.execute(new GetRootDirectoryTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.ListDirs.name.equals(action)) {
            this.executor.execute(new ListDirectoriesTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.ListFiles.name.equals(action)) {
            this.executor.execute(new ListFilesTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.FindFile.name.equals(action)) {
            this.executor.execute(new FindFileTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.NewDir.name.equals(action)) {
            this.executor.execute(new NewDirectoryTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.DeleteDir.name.equals(action)) {
            this.executor.execute(new DeleteDirectoryTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.RenameDir.name.equals(action)) {
            this.executor.execute(new RenameDirectoryTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.InsertFile.name.equals(action)) {
            this.executor.execute(new InsertFileTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.MoveFile.name.equals(action)) {
            this.executor.execute(new MoveFileTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.RenameFile.name.equals(action)) {
            this.executor.execute(new RenameFileTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.DeleteFile.name.equals(action)) {
            this.executor.execute(new DeleteFileTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.ListTrash.name.equals(action)) {
            this.executor.execute(new ListTrashTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.EraseTrash.name.equals(action)) {
            this.executor.execute(new EraseTrashTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.EmptyTrash.name.equals(action)) {
            this.executor.execute(new EmptyTrashTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.RestoreTrash.name.equals(action)) {
            this.executor.execute(new RestoreTrashTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.SearchFile.name.equals(action)) {
            this.executor.execute(new SearchFileTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.GetSharingTag.name.equals(action)) {
            this.executor.execute(new GetSharingTagTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.CreateSharingTag.name.equals(action)) {
            this.executor.execute(new CreateSharingTagTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.GetSharingReport.name.equals(action)) {
            this.executor.execute(new GetSharingReportTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.ListSharingTags.name.equals(action)) {
            this.executor.execute(new ListSharingTagsTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.Trace.name.equals(action)) {
            this.executor.execute(new TraceTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.ListSharingTraces.name.equals(action)) {
            this.executor.execute(new ListSharingTracesTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.CancelSharingTag.name.equals(action)) {
            this.executor.execute(new CancelSharingTagTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
        else if (FileStorageAction.DeleteSharingTag.name.equals(action)) {
            this.executor.execute(new DeleteSharingTagTask(this, talkContext, primitive,
                    this.markResponseTime(action)));
        }
    }

    @Override
    public void onListened(TalkContext talkContext, PrimitiveInputStream stream) {
        this.executor.execute(new WriteFileTask(this, talkContext, stream));
    }
}
