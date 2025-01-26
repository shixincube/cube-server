/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.multipointcomm;

import org.kurento.client.Stats;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * 媒体会话的快照任务。
 */
public final class SnapshotDaemon implements Runnable {

    private ExecutorService executor;

    private List<MediaUnit> mediaUnits;

    private Timer timer;

    public SnapshotDaemon(ExecutorService executor, List<MediaUnit> mediaUnits) {
        this.executor = executor;
        this.mediaUnits = mediaUnits;
    }

    public void start() {
        if (null == this.timer) {
            this.timer = new Timer();
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SnapshotDaemon.this.run();
                }
            }, 30000, 1000);
        }
    }

    public void stop() {
        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < this.mediaUnits.size(); ++i) {
            MediaUnit mediaUnit = this.mediaUnits.get(i);
            Collection<? extends MediaLobby> lobbies = mediaUnit.getAllLobbies();
            if (lobbies.isEmpty()) {
                continue;
            }

            Iterator<? extends MediaLobby> iter = lobbies.iterator();
            while (iter.hasNext()) {
                MediaLobby lobby = iter.next();
                Collection<? extends MediaSession> sessions = lobby.getSessions();

                Iterator<? extends MediaSession> msiter = sessions.iterator();
                while (msiter.hasNext()) {
                    MediaSession session = msiter.next();

                    Map<String, Stats> stats = session.getOutgoingPeer().getStats();

                }
            }
        }
    }
}
