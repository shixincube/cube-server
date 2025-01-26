/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.tool;

import cell.api.*;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkError;
import cell.util.log.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 服务检测工具。
 */
public final class Detector {

    private Detector() {
    }

    public static boolean detectCellServer(String host, int port) {
        String address = host;
        if (host.equals("0.0.0.0")) {
            address = "127.0.0.1";
        }
        final String serverAddress = address;

        AtomicBoolean connected = new AtomicBoolean(false);

        NucleusConfig config = new NucleusConfig(NucleusDevice.DESKTOP);
        Nucleus nucleus = new Nucleus(config);

        nucleus.getTalkService().setListener("AuthService", new TalkListener() {
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
                Logger.d(Detector.class, "#onContacted - " + serverAddress);
                connected.set(true);

                synchronized (serverAddress) {
                    serverAddress.notify();
                }
            }

            @Override
            public void onQuitted(Speakable speakable) {
                Logger.d(Detector.class, "#onQuitted - " + serverAddress);

                synchronized (serverAddress) {
                    serverAddress.notify();
                }
            }

            @Override
            public void onFailed(Speakable speakable, TalkError talkError) {
                Logger.d(Detector.class, "#detectCellServer - " + serverAddress + " - #onFailed: " + talkError.getErrorCode());
                synchronized (serverAddress) {
                    serverAddress.notify();
                }
            }
        });

        // 尝试连接
        nucleus.getTalkService().call(address, port);

        synchronized (serverAddress) {
            try {
                serverAddress.wait(10000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        nucleus.getTalkService().hangup(address, port, false);

        synchronized (serverAddress) {
            try {
                serverAddress.wait(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        nucleus.destroy();

        return connected.get();
    }
}
