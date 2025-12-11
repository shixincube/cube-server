/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.stream;

import cell.core.net.Message;
import cell.core.net.NonblockingAcceptor;
import cell.core.net.Session;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Track {

    private NonblockingAcceptor acceptor;

    private Session session;

    public Track(NonblockingAcceptor acceptor, Session session) {
        this.acceptor = acceptor;
        this.session = session;
    }

    public void write(StreamType streamType, JSONObject data) {
        // 数据格式：TYPE+DATA(Sink)
        byte[] type = streamType.name.getBytes(StandardCharsets.UTF_8);
        byte[] sink = data.toString().getBytes(StandardCharsets.UTF_8);

        byte[] buf = new byte[type.length + StreamServer.sSeparator.length + sink.length];
        System.arraycopy(type, 0, buf, 0, type.length);
        System.arraycopy(StreamServer.sSeparator, 0, buf, type.length, StreamServer.sSeparator.length);
        System.arraycopy(sink, 0, buf, type.length + StreamServer.sSeparator.length, sink.length);

        Message message = new Message(buf);
        // 禁用压缩
        message.disableCompression();
        try {
            this.acceptor.write(this.session, message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
