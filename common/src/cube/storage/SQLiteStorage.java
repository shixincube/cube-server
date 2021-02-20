/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.storage;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.core.AbstractStorage;
import cube.core.Conditional;
import cube.core.StorageField;
import cube.util.SQLUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 SQLite 的存储器。
 */
public class SQLiteStorage extends AbstractStorage {

    public final static String CONFIG_FILE = "file";

    private Connection connection = null;

    public SQLiteStorage(String name) {
        super(name);
    }

    @Override
    public void open() {
        if (null != this.connection) {
            return;
        }

        JSONObject config = this.getConfig();
        String file = null;
        try {
            file = config.getString(CONFIG_FILE);
        } catch (JSONException e) {
            Logger.e(this.getClass(), "Open SQLite Storage", e);
            return;
        }

        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + file);
        } catch (SQLException e) {
            Logger.e(this.getClass(), "Open SQLite Storage", e);
        }
    }

    @Override
    public void close() {
        if (null == this.connection) {
            return;
        }

        try {
            this.connection.close();
        } catch (SQLException e) {
            Logger.e(this.getClass(), "Close SQLite Storage", e);
        }

        this.connection = null;
    }

    @Override
    public void configure(JSONObject config) {
        super.configure(config);
    }

    @Override
    public boolean exist(String table) {
        Statement statement = null;

        synchronized (this.connection) {
            try {
                statement = this.connection.createStatement();
                statement.setQueryTimeout(10);
                statement.executeQuery("SELECT * FROM " + table + " LIMIT 1");
            } catch (SQLException e) {
                return false;
            } finally {
                if (null != statement) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }

            return true;
        }
    }

    @Override
    public boolean executeCreate(String table, StorageField[] fields) {
        for (StorageField field : fields) {
            this.fixBigint(field);
        }

        // 拼写 SQL 语句
        String sql = SQLUtils.spellCreateTable(table, fields);

        Statement statement = null;

        synchronized (this.connection) {
            try {
                statement = this.connection.createStatement();
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                Logger.e(this.getClass(), "SQL: " + sql, e);
                return false;
            } finally {
                if (null != statement) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }

        return true;
    }

    private void fixBigint(StorageField field) {
        if (field.getLiteralBase() == LiteralBase.LONG) {
            field.resetLiteralBase(LiteralBase.INT);
        }
    }

    @Override
    public boolean executeInsert(String table, StorageField[] fields) {
        // 拼写 SQL 语句
        String sql = SQLUtils.spellInsert(table, fields);

        Statement statement = null;

        synchronized (this.connection) {
            try {
                statement = this.connection.createStatement();
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                Logger.e(this.getClass(), "SQL: " + sql, e);
                return false;
            } finally {
                if (null != statement) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean executeInsert(String table, List<StorageField[]> fieldsList) {
        boolean success = true;

        synchronized (this.connection) {
            for (StorageField[] fields : fieldsList) {
                // 拼写 SQL 语句
                String sql = SQLUtils.spellInsert(table, fields);

                Statement statement = null;
                try {
                    statement = this.connection.createStatement();
                    statement.executeUpdate(sql);
                } catch (SQLException e) {
                    Logger.e(this.getClass(), "SQL: " + sql, e);
                    success = false;
                    continue;
                } finally {
                    if (null != statement) {
                        try {
                            statement.close();
                        } catch (SQLException e) {
                        }
                    }
                }
            }
        }

        return success;
    }

    @Override
    public boolean executeUpdate(String table, StorageField[] fields, Conditional[] conditionals) {
        // 拼写 SQL 语句
        String sql = SQLUtils.spellUpdate(table, fields, conditionals);

        Statement statement = null;

        synchronized (this.connection) {
            try {
                statement = this.connection.createStatement();
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                Logger.e(this.getClass(), "SQL: " + sql, e);
                return false;
            } finally {
                if (null != statement) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean executeDelete(String table, Conditional[] conditionals) {
        // 拼写 SQL 语句
        String sql = SQLUtils.spellDelete(table, conditionals);

        Statement statement = null;

        synchronized (this.connection) {
            try {
                statement = this.connection.createStatement();
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                Logger.e(this.getClass(), "SQL: " + sql, e);
                return false;
            } finally {
                if (null != statement) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<StorageField[]> executeQuery(String table, StorageField[] fields) {
        return this.executeQuery(table, fields, null);
    }

    @Override
    public List<StorageField[]> executeQuery(String table, StorageField[] fields, Conditional[] conditionals) {
        ArrayList<StorageField[]> result = new ArrayList<>();

        // 拼写 SQL 语句
        String sql = SQLUtils.spellSelect(table, fields, conditionals);

        Statement statement = null;

        synchronized (this.connection) {
            try {
                statement = this.connection.createStatement();
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    StorageField[] row = new StorageField[fields.length];

                    for (int i = 0; i < fields.length; ++i) {
                        StorageField sf = fields[i];
                        LiteralBase literal = sf.getLiteralBase();
                        if (literal == LiteralBase.STRING) {
                            String value = rs.getString(sf.getName());
                            row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                        } else if (literal == LiteralBase.LONG) {
                            long value = rs.getLong(sf.getName());
                            row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                        } else if (literal == LiteralBase.INT) {
                            int value = rs.getInt(sf.getName());
                            row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                        } else if (literal == LiteralBase.BOOL) {
                            boolean value = rs.getBoolean(sf.getName());
                            row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                        }
                    }

                    result.add(row);
                }
            } catch (SQLException e) {
                Logger.d(this.getClass(), e.getMessage());
            } finally {
                if (null != statement) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<StorageField[]> executeQuery(String[] tables, StorageField[] fields, Conditional[] conditionals) {
        ArrayList<StorageField[]> result = new ArrayList<>();

        // 拼写 SQL 语句
        String sql = SQLUtils.spellSelect(tables, fields, conditionals);

        Statement statement = null;

        synchronized (this.connection) {
            try {
                statement = this.connection.createStatement();
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    StorageField[] row = new StorageField[fields.length];

                    for (int i = 0; i < fields.length; ++i) {
                        StorageField sf = fields[i];
                        LiteralBase literal = sf.getLiteralBase();

                        if (literal == LiteralBase.STRING) {
                            String value = rs.getString(sf.getName());
                            row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                        } else if (literal == LiteralBase.LONG) {
                            long value = rs.getLong(sf.getName());
                            row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                        } else if (literal == LiteralBase.INT) {
                            int value = rs.getInt(sf.getName());
                            row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                        } else if (literal == LiteralBase.BOOL) {
                            boolean value = rs.getBoolean(sf.getName());
                            row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                        }
                    }

                    result.add(row);
                }
            } catch (SQLException e) {
                Logger.d(this.getClass(), e.getMessage());
            } finally {
                if (null != statement) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }

        return result;
    }
}
