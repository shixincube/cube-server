/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.storage;

import java.sql.Connection;

public interface ConnectionHandler {

    void handle(Connection connection);
}
