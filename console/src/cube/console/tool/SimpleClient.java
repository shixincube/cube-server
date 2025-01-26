/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.console.tool;

import cell.api.Nucleus;
import cell.api.NucleusConfig;
import cell.api.NucleusDevice;
import cube.auth.AuthToken;

/**
 * 简单客户端。
 */
public class SimpleClient {

    private AuthToken authToken;

    private Nucleus nucleus;

    public SimpleClient(AuthToken authToken) {
        this.authToken = authToken;
    }

    public void start(String address, int port) {
        NucleusConfig config = new NucleusConfig(NucleusDevice.DESKTOP);
        this.nucleus = new Nucleus(config);

        this.nucleus.getTalkService().call(address, port);
    }

    public void stop() {

    }

    public void signIn() {

    }
}
