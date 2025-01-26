/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.storage;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.core.AbstractStorage;
import cube.core.Conditional;
import cube.core.Constraint;
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
        super(name, StorageType.SQLite);
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
            this.fixBigintAndAutoIncrement(field);
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

    private void fixBigintAndAutoIncrement(StorageField field) {
        if (field.getLiteralBase() == LiteralBase.LONG) {
            field.resetLiteralBase(LiteralBase.INT);
        }

        Constraint[] constraints = field.getConstraints();
        if (null != constraints) {
            for (int i = 0; i < constraints.length; ++i) {
                Constraint constraint = constraints[i];
                if (constraint == Constraint.AUTO_INCREMENT) {
                    constraints[i] = Constraint.AUTOINCREMENT;
                }
            }
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
                    if (null == fields) {
                        StorageField[] row = StorageFields.scanResultSet(rs);
                        result.add(row);
                    }
                    else {
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
                    if (null == fields) {
                        StorageField[] row = StorageFields.scanResultSet(rs);
                        result.add(row);
                    }
                    else {
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
    public List<StorageField[]> executeQuery(String sql) {
        ArrayList<StorageField[]> result = new ArrayList<>();

        Statement statement = null;

        try {
            statement = this.connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                StorageField[] row = StorageFields.scanResultSet(rs);
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

        return result;
    }

    @Override
    public boolean execute(String sql) {
        Statement statement = null;

        try {
            statement = this.connection.createStatement();
            return statement.execute(sql);
        } catch (SQLException e) {
            Logger.w(this.getClass(), "#execute - SQL: " + sql, e);
        } finally {
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }
        }

        return false;
    }
}
