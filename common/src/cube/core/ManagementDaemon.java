/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.core;

import cell.api.Servable;
import cell.core.talk.BaseServer;
import cell.core.talk.TalkContext;
import cell.util.CachedQueueExecutor;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 管理器守护线程。
 * 不使用系统的定时器机制，而使用线程自旋方式，让整个任务始终持有时间片。
 */
public class ManagementDaemon extends Thread {

    private Kernel kernel;

    private boolean spinning = true;

    private final long spinningSleep = 60 * 1000;

    private long lastCheckTime = System.currentTimeMillis();

    private ExecutorService executor;

    public ManagementDaemon(Kernel kernel) {
        setName("ManagementDaemon");
        setDaemon(true);
        this.kernel = kernel;
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(4);
    }

    @Override
    public void run() {
        while (this.spinning) {
            try {
                Thread.sleep(this.spinningSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!this.spinning) {
                break;
            }

            List<AbstractModule> list = this.kernel.getModules();
            for (int i = 0, size = list.size(); i < size; ++i) {
                final AbstractModule module = list.get(i);
                this.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        module.onTick(module, kernel);
                    }
                });
            }

            if (System.currentTimeMillis() - this.lastCheckTime > 10 * 60 * 1000) {
                this.lastCheckTime = System.currentTimeMillis();

                try {
                    for (Servable server : this.kernel.getNucleus().getTalkService().getServers()) {
                        List<TalkContext> contextList = server.getAllContext();
                        for (TalkContext context : contextList) {
                            if (!server.isActive(context)) {
                                // 非活跃
                                BaseServer baseServer = (BaseServer) server;
                                baseServer.hangup(context.getSession(), true);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public final void terminate() {
        this.spinning = false;
        if (null != this.executor && !this.executor.isShutdown()) {
            this.executor.shutdown();
        }
    }
}
