package org.backmeup.keyserver.core.db.sql;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.backmeup.keyserver.core.config.Configuration;
import org.backmeup.keyserver.core.db.Database;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.backmeup.keyserver.model.KeyserverUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLDatabaseImpl implements Database {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLDatabaseImpl.class);
    protected static final Properties SQL_STATEMENTS = new Properties();
    protected static final String SQL_STMT_FILE = "backmeup-keyserver-sql.properties";

    static {
        try {
            Class.forName(Configuration.getProperty("backmeup.keyserver.db.driver_name"));
        } catch (java.lang.ClassNotFoundException e) {
            LOGGER.error("could not load database driver", e);
        }
        
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader.getResourceAsStream(SQL_STMT_FILE) != null) {
                SQL_STATEMENTS.load(loader.getResourceAsStream(SQL_STMT_FILE));
            } else {
                throw new IOException("unable to load properties file: " + SQL_STMT_FILE);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected static final String DB_TABLE = SQL_STATEMENTS.getProperty("backmeup.keyserver.db.table");

    protected Connection conn;
    protected boolean connected;
    protected PreparedStatement psPut;
    protected PreparedStatement psUpdateTTL;
    protected PreparedStatement psGet;
    protected PreparedStatement psGetWithVersion;
    protected PreparedStatement psSearchByKey;
    protected PreparedStatement psSearchByKeyWithExpired;
    
    protected static String getSQLStatement(String key) throws DatabaseException {
        String stmt = SQL_STATEMENTS.getProperty(key);
        if (stmt == null) {
            throw new DatabaseException("Statement "+key+" not found in "+SQL_STMT_FILE);
        }
        return MessageFormat.format(stmt, DB_TABLE);
    }

    @Override
    @SuppressWarnings("all")
    public void connect() throws DatabaseException {
        try {
            this.conn = DriverManager.getConnection(Configuration.getProperty("backmeup.keyserver.db.connection_string"));
            this.conn.setAutoCommit(false);
            if (!this.checkForTable()) {
                this.prepareTable();
            }

            this.psPut = this.conn.prepareStatement(getSQLStatement("backmeup.keyserver.db.sql.put"));
            this.psUpdateTTL = this.conn.prepareStatement(getSQLStatement("backmeup.keyserver.db.sql.updateTTL"));
            this.psGet = this.conn.prepareStatement(getSQLStatement("backmeup.keyserver.db.sql.get"));
            this.psGetWithVersion = this.conn.prepareStatement(getSQLStatement("backmeup.keyserver.db.sql.getWithVersion"));
            this.psSearchByKey = this.conn.prepareStatement(getSQLStatement("backmeup.keyserver.db.sql.searchByKey"));
            this.psSearchByKeyWithExpired = this.conn.prepareStatement(getSQLStatement("backmeup.keyserver.db.sql.searchByKeyWithExpired"));
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

        this.connected = true;
    }

    protected boolean checkForTable() throws SQLException {
        DatabaseMetaData dbm = this.conn.getMetaData();
        try (ResultSet tables = dbm.getTables(null, null, DB_TABLE, null)) {
            return tables.next();
        }
    }

    protected void prepareTable() throws SQLException, DatabaseException {
        try (Statement s = this.conn.createStatement()) {
            s.execute(getSQLStatement("backmeup.keyserver.db.sql.create"));
            this.conn.commit();
        } catch (SQLException e) {
            conn.rollback();
        }
    }

    @Override
    public void disconnect() throws DatabaseException {
        this.connected = false;

        try {
            if (this.conn != null) {
                if (this.psPut != null) {
                    this.psPut.close();
                }
                if (this.psGet != null) {
                    this.psGet.close();
                }
                if (this.psGetWithVersion != null) {
                    this.psGetWithVersion.close();
                }
                this.conn.commit();
                this.conn.close();
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /*
     * This method should only be called for testing purposes!
     */
    public void cleanup() throws DatabaseException {
        try (Statement s = conn.createStatement()) {
            s.execute("DROP TABLE " + DB_TABLE);
            conn.commit();
            this.prepareTable();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new DatabaseException(e1);
            }
            throw new DatabaseException(e);
        }
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public KeyserverEntry getEntry(String key) throws DatabaseException {
        KeyserverEntry entry = null;

        try {
            this.psGet.clearParameters();
            this.psGet.setString(1, key);
            this.psGet.setTimestamp(2, new Timestamp(KeyserverUtils.getActTime().getTimeInMillis()));
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

        try (ResultSet rs = this.psGet.executeQuery()) {
            if (rs.next()) {
                entry = createEntryFromResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

        return entry;
    }

    @Override
    public KeyserverEntry getEntry(String key, long version) throws DatabaseException {
        KeyserverEntry entry = null;
        try {
            this.psGetWithVersion.clearParameters();
            this.psGetWithVersion.setString(1, key);
            this.psGetWithVersion.setLong(2, version);
            this.psGetWithVersion.setTimestamp(3, new Timestamp(KeyserverUtils.getActTime().getTimeInMillis()));
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

        try (ResultSet rs = this.psGetWithVersion.executeQuery()) {
            if (rs.next()) {
                entry = createEntryFromResultSet(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

        return entry;
    }

    protected KeyserverEntry createEntryFromResultSet(ResultSet rs) throws SQLException {
        Calendar createdAt = KeyserverUtils.getActTime();
        createdAt.setTime(rs.getTimestamp("created_at"));
        Calendar lastModified = KeyserverUtils.getActTime();
        lastModified.setTime(rs.getTimestamp("last_modified"));

        Timestamp ttl = rs.getTimestamp("ttl");
        Calendar cttl = null;
        if (ttl != null) {
            cttl = KeyserverUtils.getActTime();
            cttl.setTime(ttl);
        }

        return new KeyserverEntry(rs.getString("ekey"), rs.getBytes("value"), rs.getInt("keyringId"), rs.getLong("version"), createdAt, lastModified,
                cttl);
    }

    @Override
    public void putEntry(KeyserverEntry entry) throws DatabaseException {
        try {
            this.psPut.clearParameters();
            this.psPut.setString(1, entry.getKey());
            this.psPut.setBytes(2, entry.getValue());
            this.psPut.setInt(3, entry.getKeyringId());
            this.psPut.setLong(4, entry.getVersion());
            this.psPut.setTimestamp(5, new Timestamp(entry.getCreatedAt().getTimeInMillis()));
            this.psPut.setTimestamp(6, new Timestamp(entry.getLastModified().getTimeInMillis()));
            Calendar c = entry.getTTL();
            if (c == null) {
                this.psPut.setNull(7, Types.TIMESTAMP);
            } else {
                this.psPut.setTimestamp(7, new Timestamp(c.getTimeInMillis()));
            }

            this.psPut.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new DatabaseException(e1);
            }
            throw new DatabaseException(e);
        }
    }

    @Override
    public void updateTTL(KeyserverEntry entry) throws DatabaseException {
        try {
            this.psUpdateTTL.clearParameters();

            Calendar c = entry.getTTL();
            if (c == null) {
                this.psUpdateTTL.setNull(1, Types.TIMESTAMP);
            } else {
                this.psUpdateTTL.setTimestamp(1, new Timestamp(c.getTimeInMillis()));
            }

            this.psUpdateTTL.setString(2, entry.getKey());
            this.psUpdateTTL.setLong(3, entry.getVersion());

            this.psUpdateTTL.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                throw new DatabaseException(e1);
            }
            throw new DatabaseException(e);
        }
    }
    
    @Override
    public List<KeyserverEntry> searchByKey(String key, boolean allVersions, boolean withExpired) throws DatabaseException {
        List<KeyserverEntry> entries = new LinkedList<>();
        PreparedStatement search = null;
        
        if (withExpired) {
            search = this.psSearchByKeyWithExpired;
        } else {
            search = this.psSearchByKey;
        }
        
        try {
            search.clearParameters();
            search.setString(1, key);
            if (!withExpired) {
                search.setTimestamp(2, new Timestamp(KeyserverUtils.getActTime().getTimeInMillis()));
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
         
        try (ResultSet rs = search.executeQuery()) {
            String lastKey = null;
            
            while (rs.next()) {
                KeyserverEntry entry = createEntryFromResultSet(rs);
                String actKey = entry.getKey();
                if (allVersions || !actKey.equals(lastKey)) {
                    entries.add(entry);
                    lastKey = actKey;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }

        return entries;
    }
}