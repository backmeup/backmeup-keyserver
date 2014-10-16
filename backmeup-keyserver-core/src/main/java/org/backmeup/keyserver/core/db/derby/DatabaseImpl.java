package org.backmeup.keyserver.core.db.derby;

import java.io.ByteArrayInputStream;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.TimeZone;

import org.backmeup.keyserver.core.db.Database;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.model.KeyserverEntry;

public class DatabaseImpl implements Database {
	
	static {
		try {
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		} catch (java.lang.ClassNotFoundException e) {
			System.err.print("ClassNotFoundException: ");
			System.err.println(e.getMessage());
		}
	}
	
	private static final String DB_NAME = "keyserver";
	private static final String DB_TABLE = "ENTRY";
	private static final String DB_CREATE_SQL = "CREATE TABLE " + DB_TABLE
			+ "(id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY CONSTRAINT " + DB_TABLE + "_pk PRIMARY KEY, "
			+ "ekey VARCHAR(128) NOT NULL, "
			+ "value BLOB, " 
			+ "keyringId INT NOT NULL, "
			+ "version BIGINT NOT NULL DEFAULT 0, "
			+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
			+ "last_modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
			+ "ttl TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
			+ "CONSTRAINT " + DB_TABLE + "_ekey UNIQUE(ekey, version))";

	private Connection conn;
	private boolean connected;
	private PreparedStatement psPut;
	private PreparedStatement psUpdateTTL;
	private PreparedStatement psGet;
	private PreparedStatement psGetWithVersion;

	@Override
	public void connect() throws DatabaseException {		
		try {
			this.conn = DriverManager.getConnection("jdbc:derby:" + DB_NAME + ";create=true");
			if (!this.checkForTable()) {
				this.prepareTable();
			}
			
			this.psPut = this.conn.prepareStatement("INSERT INTO " + DB_TABLE + "(ekey, value, keyringId, version, created_at, last_modified, ttl) VALUES (?, ?, ?, ?, ?, ?, ?)");
			this.psUpdateTTL = this.conn.prepareStatement("UPDATE " + DB_TABLE + " SET ttl = ?  WHERE ekey = ? AND version = ?");
			this.psGet = this.conn.prepareStatement("SELECT * FROM " + DB_TABLE + " WHERE ekey = ? AND (ttl IS NULL OR ttl > CURRENT_TIMESTAMP) ORDER BY version DESC FETCH FIRST ROW ONLY");
			this.psGetWithVersion = this.conn.prepareStatement("SELECT * FROM " + DB_TABLE + " WHERE ekey = ? AND version = ? AND (ttl IS NULL OR ttl > CURRENT_TIMESTAMP)");
		} catch (SQLException e) {
			throw new DatabaseException(e);
		}
		
		this.connected = true;
	}

	protected boolean checkForTable() throws SQLException {
		DatabaseMetaData dbm = this.conn.getMetaData();
		try (ResultSet tables = dbm.getTables(null, null, DB_TABLE, null)) {
			boolean result = tables.next();
			return result;
		}
	}

	protected void prepareTable() throws SQLException {
		try (Statement s = conn.createStatement()) {
			s.execute(DB_CREATE_SQL);
		}
	}

	@Override
	public void disconnect() throws DatabaseException {
		this.connected = false;
		
		try {
			if(this.conn != null) {
				if (this.psPut != null) this.psPut.close();
				if (this.psGet != null) this.psGet.close();
				if (this.psGetWithVersion != null) this.psGetWithVersion.close();
				this.conn.close();
			}
		} catch (SQLException e) {
			throw new DatabaseException(e);
		}
		try {
			DriverManager.getConnection("jdbc:derby:;shutdown=true");
		} catch (SQLException e) {
			if (!e.getSQLState().equals("XJ015")) {
				throw new DatabaseException(e);
			}
		}
	}
	
	protected void dropTable() throws DatabaseException {
		try (Statement s = conn.createStatement()) {
			s.execute("DROP TABLE " + DB_TABLE);
		} catch (SQLException e) {
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
	
	private KeyserverEntry createEntryFromResultSet(ResultSet rs) throws SQLException {
		Calendar createdAt = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		createdAt.setTime(rs.getTimestamp("created_at"));
		Calendar lastModified = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		lastModified.setTime(rs.getTimestamp("last_modified"));
		
		Timestamp ttl = rs.getTimestamp("ttl");
		Calendar cttl = null;
		if (ttl != null) {
			cttl = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			cttl.setTime(ttl);
		}
		
		return new KeyserverEntry(rs.getString("ekey"), rs.getBytes("value"), rs.getInt("keyringId"), rs.getLong("version"), createdAt, lastModified, cttl);
	}

	@Override
	public void putEntry(KeyserverEntry entry) throws DatabaseException {
		try {
			this.psPut.clearParameters();
			this.psPut.setString(1, entry.getKey());
			this.psPut.setBlob(2, new ByteArrayInputStream(entry.getValue()));
			this.psPut.setInt(3, entry.getKeyringId());
			this.psPut.setLong(4, entry.getVersion());
			this.psPut.setTimestamp(5, new Timestamp(entry.getCreatedAt().getTimeInMillis()));
			this.psPut.setTimestamp(6, new Timestamp(entry.getLastModified().getTimeInMillis()));
			Calendar c = entry.getTTL();
			if (c == null)
				this.psPut.setNull(7, Types.TIMESTAMP);
			else
				this.psPut.setTimestamp(7, new Timestamp(c.getTimeInMillis()));
			
			this.psPut.executeUpdate();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}
	
	@Override
	public void updateTTL(KeyserverEntry entry) throws DatabaseException {
		try {
			this.psUpdateTTL.clearParameters();
			
			Calendar c = entry.getTTL();
			if (c == null)
				this.psUpdateTTL.setNull(1, Types.TIMESTAMP);
			else
				this.psUpdateTTL.setTimestamp(1, new Timestamp(c.getTimeInMillis()));
			
			this.psUpdateTTL.setString(2, entry.getKey());
			this.psUpdateTTL.setLong(3, entry.getVersion());

			this.psUpdateTTL.executeUpdate();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

}