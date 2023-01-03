/*
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 Cube Team.
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
