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
 *
 * <p><b>契约约定</b>：{@link SQLUtils#spellInsert(String, StorageField[])} 与
 * {@link SQLUtils#spellUpdate(String, StorageField[], Conditional[])} 在
 * <b>不存在有效赋值字段</b>时会返回 {@code null}（而不是拼出一条非法 SQL）。
 * 因此本类的写入方法必须对该返回值判空，并按原有的「记录日志 + 返回 false」语义处理。</p>
 *
 * <p>另外，{@code open()} 失败时 {@code connection} 会保持为 {@code null}，
 * 所有执行方法都必须据此显式失败，而不是抛出 {@link NullPointerException}。</p>
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

    /**
     * 判断底层连接是否就绪。
     *
     * @return 连接可用返回 {@code true} 。
     */
    private boolean isReady() {
        if (null == this.connection) {
            Logger.e(this.getClass(), "#isReady - The SQLite connection is not ready");
            return false;
        }

        return true;
    }

    @Override
    public boolean exist(String table) {
        if (!this.isReady()) {
            return false;
        }

        Statement statement = null;

        synchronized (this.connection) {
            try {
                statement = this.connection.createStatement();
                statement.setQueryTimeout(10);
                statement.executeQuery("SELECT * FROM " + SQLUtils.correctTableName(table) + " LIMIT 1");
            } catch (SQLException e) {
                return false;
            } finally {
                if (null != statement) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        // 释放失败不影响主流程，仅记录
                        Logger.d(SQLiteStorage.class, "#exist - Failed to close 'statement': " + e.getMessage());
                    }
                }
            }

            return true;
        }
    }

    @Override
    public boolean executeCreate(String table, StorageField[] fields) {
        if (!this.isReady()) {
            return false;
        }

        if (null == fields || 0 == fields.length) {
            Logger.e(this.getClass(), "#executeCreate - Empty fields, table: " + table);
            return false;
        }

        // 按 SQLite 方言修正字段。StorageField 不可变，这里构造修正后的副本，
        // 不改写调用方持有的对象（同一份字段数组还会被 executeQuery 使用）。
        StorageField[] fixedFields = new StorageField[fields.length];
        for (int i = 0; i < fields.length; ++i) {
            fixedFields[i] = this.fixBigintAndAutoIncrement(fields[i]);
        }

        // 拼写 SQL 语句
        String sql = SQLUtils.spellCreateTable(table, fixedFields);

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
                        // 释放失败不影响主流程，仅记录
                        Logger.d(SQLiteStorage.class, "#executeCreate - Failed to close 'statement': " + e.getMessage());
                    }
                }
            }
        }

        return true;
    }

    /**
     * 按 SQLite 方言修正字段：{@code BIGINT} 改为 {@code INTEGER} ，
     * {@code AUTO_INCREMENT} 改为 {@code AUTOINCREMENT} 。
     *
     * <p>入参不会被修改。无需修正时原样返回入参，否则返回修正后的新实例。</p>
     *
     * @param field 待修正的字段。
     * @return 返回可直接用于拼装 SQL 的字段。
     */
    private StorageField fixBigintAndAutoIncrement(StorageField field) {
        LiteralBase literal = field.getLiteralBase();
        if (literal == LiteralBase.LONG) {
            // SQLite 使用 INTEGER 存储 8 字节整数
            literal = LiteralBase.INT;
        }

        // getConstraints() 返回内部数组的副本，可直接就地修正
        Constraint[] constraints = field.getConstraints();
        boolean changed = (literal != field.getLiteralBase());
        if (null != constraints) {
            for (int i = 0; i < constraints.length; ++i) {
                if (constraints[i] == Constraint.AUTO_INCREMENT) {
                    constraints[i] = Constraint.AUTOINCREMENT;
                    changed = true;
                }
            }
        }

        if (!changed) {
            return field;
        }

        return new StorageField(field.getTableName(), field.getName(), literal, field.getValue(), constraints);
    }

    @Override
    public boolean executeInsert(String table, StorageField[] fields) {
        if (!this.isReady()) {
            return false;
        }

        // 拼写 SQL 语句
        String sql = SQLUtils.spellInsert(table, fields);
        if (null == sql) {
            // 所有字段值均为空，不存在合法的 INSERT 语句
            return false;
        }

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
                        // 释放失败不影响主流程，仅记录
                        Logger.d(SQLiteStorage.class, "#executeInsert - Failed to close 'statement': " + e.getMessage());
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean executeInsert(String table, List<StorageField[]> fieldsList) {
        if (!this.isReady()) {
            return false;
        }

        boolean success = true;

        synchronized (this.connection) {
            for (StorageField[] fields : fieldsList) {
                // 拼写 SQL 语句
                String sql = SQLUtils.spellInsert(table, fields);
                if (null == sql) {
                    // 所有字段值均为空，跳过该条记录
                    success = false;
                    continue;
                }

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
                            // 释放失败不影响主流程，仅记录
                            Logger.d(SQLiteStorage.class, "#executeInsert - Failed to close 'statement': " + e.getMessage());
                        }
                    }
                }
            }
        }

        return success;
    }

    @Override
    public boolean executeUpdate(String table, StorageField[] fields, Conditional[] conditionals) {
        if (!this.isReady()) {
            return false;
        }

        // 拼写 SQL 语句
        String sql = SQLUtils.spellUpdate(table, fields, conditionals);
        if (null == sql) {
            // 没有有效的赋值字段
            return false;
        }

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
                        // 释放失败不影响主流程，仅记录
                        Logger.d(SQLiteStorage.class, "#executeUpdate - Failed to close 'statement': " + e.getMessage());
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean executeDelete(String table, Conditional[] conditionals) {
        if (!this.isReady()) {
            return false;
        }

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
                        // 释放失败不影响主流程，仅记录
                        Logger.d(SQLiteStorage.class, "#executeDelete - Failed to close 'statement': " + e.getMessage());
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

        if (!this.isReady()) {
            return result;
        }

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
                        // 释放失败不影响主流程，仅记录
                        Logger.d(SQLiteStorage.class, "#executeQuery - Failed to close 'statement': " + e.getMessage());
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<StorageField[]> executeQuery(String[] tables, StorageField[] fields, Conditional[] conditionals) {
        ArrayList<StorageField[]> result = new ArrayList<>();

        if (!this.isReady()) {
            return result;
        }

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
                        // 释放失败不影响主流程，仅记录
                        Logger.d(SQLiteStorage.class, "#executeQuery - Failed to close 'statement': " + e.getMessage());
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<StorageField[]> executeQuery(String sql) {
        ArrayList<StorageField[]> result = new ArrayList<>();

        if (!this.isReady()) {
            return result;
        }

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
                    // 释放失败不影响主流程，仅记录
                    Logger.d(SQLiteStorage.class, "#executeQuery - Failed to close 'statement': " + e.getMessage());
                }
            }
        }

        return result;
    }

    @Override
    public boolean execute(String sql) {
        if (!this.isReady()) {
            return false;
        }

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
                    // 释放失败不影响主流程，仅记录
                    Logger.d(SQLiteStorage.class, "#execute - Failed to close 'statement': " + e.getMessage());
                }
            }
        }

        return false;
    }
}
