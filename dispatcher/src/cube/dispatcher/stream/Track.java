/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher.stream;

import cell.core.net.NonblockingAcceptor;
import cell.core.net.Session;

public class Track {

    private NonblockingAcceptor acceptor;

    private Session session;

    public Track(NonblockingAcceptor acceptor, Session session) {
        this.acceptor = acceptor;
        this.session = session;
    }
}
