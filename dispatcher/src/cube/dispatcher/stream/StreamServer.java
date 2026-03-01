/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.stream;

import cell.core.net.Message;
import cell.core.net.MessageHandler;
import cell.core.net.NonblockingAcceptor;
import cell.core.net.Session;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.Logger;

import java.nio.charset.StandardCharsets;

public class StreamServer {

    public final static byte[] sSeparator = new byte[] { 0x10, 0x17 };

    private int port;

    private NonblockingAcceptor acceptor;

    private StreamListener listener;

    public StreamServer() {
        this.port = 7171;
    }

    public void start(int port) {
        this.port = port;
        this.acceptor = new NonblockingAcceptor();
        this.acceptor.setHandler(new StreamDataHandler());
        this.acceptor.setMaxConnectNum(1000);
        this.acceptor.setWorkerNum(16);
        (new Thread() {
            @Override
            public void run() {
                if (acceptor.bind(port)) {
                    Logger.i(StreamServer.class, "Stream server bind @ " + port);
                }
                else {
                    Logger.i(StreamServer.class, "Stream server bind ERROR @ " + port);
                }
            }
        }).start();
    }

    public void stop() {
        if (null != this.acceptor) {
            this.acceptor.unbind();
            this.acceptor = null;
        }
    }

    public int getPort() {
        return this.port;
    }

    public void setListener(StreamListener listener) {
        this.listener = listener;
    }

    public void removeListener() {
        this.listener = null;
    }


    private class StreamDataHandler implements MessageHandler {

        @Override
        public void sessionCreated(Session session) {
            // Nothing
        }

        @Override
        public void sessionDestroyed(Session session) {
            // Nothing
        }

        @Override
        public void sessionOpened(Session session) {
            Logger.d(this.getClass(), "#sessionOpened - endpoint: " + session.getEndpoint().toString());
        }

        @Override
        public void sessionClosed(Session session) {
            Logger.d(this.getClass(), "#sessionClosed - endpoint: " + session.getEndpoint().toString());
        }

        @Override
        public void messageReceived(Session session, Message message) {
            // 数据格式：TYPE+NAME+INDEX+TIMESTAMP+STREAM
            byte[] payload = message.getPayload();
            byte[] type = null;
            byte[] name = null;
            byte[] index = null;
            byte[] timestamp = null;
            byte[] data = null;
            FlexibleByteBuffer buf = new FlexibleByteBuffer();
            for (int i = 0; i < payload.length; ++i) {
                byte cb = payload[i];
                byte nb = (i + 1 < payload.length) ? payload[i + 1] : 0;
                if (cb == sSeparator[0] && nb == sSeparator[1]) {
                    if (null == type) {
                        buf.flip();
                        type = new byte[buf.limit()];
                        System.arraycopy(buf.array(), 0, type, 0, type.length);
                        buf.clear();
                    }
                    else if (null == name) {
                        buf.flip();
                        name = new byte[buf.limit()];
                        System.arraycopy(buf.array(), 0, name, 0, name.length);
                        buf.clear();
                    }
                    else if (null == index) {
                        buf.flip();
                        index = new byte[buf.limit()];
                        System.arraycopy(buf.array(), 0, index, 0, index.length);
                        buf.clear();
                    }
                    else if (null == timestamp) {
                        buf.flip();
                        timestamp = new byte[buf.limit()];
                        System.arraycopy(buf.array(), 0, timestamp, 0, timestamp.length);
                        buf.clear();
                    }

                    // 更新 i
                    i += 1;
                    continue;
                }

                buf.put(cb);
            }

            buf.flip();
            if (buf.limit() > 0) {
                data = new byte[buf.limit()];
                System.arraycopy(buf.array(), 0, data, 0, data.length);
            }

            if (null != type && null != name && null != index && null != data) {
                String strType = new String(type, StandardCharsets.UTF_8);
                String strName = new String(name, StandardCharsets.UTF_8);
                String strIndex = new String(index);
                String strTimestamp = new String(timestamp);
                Stream stream = new Stream(strType, strName,
                        Integer.parseInt(strIndex), Long.parseLong(strTimestamp), data);
                if (null != listener) {
                    listener.onStream(new Track(acceptor, session), stream);
                }
            }
            else {
                Logger.w(this.getClass(), "#messageReceived - Stream data format error");
            }
        }

        @Override
        public void messageSent(Session session, Message message) {
            // Nothing
        }

        @Override
        public void errorOccurred(int code, Session session) {
            // Nothing
        }
    }
}
