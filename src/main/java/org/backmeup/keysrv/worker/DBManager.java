package org.backmeup.keysrv.worker;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.codec.binary.Base64;
import org.backmeup.keysrv.rest.exceptions.RestAuthInfoAlreadyExistException;
import org.backmeup.keysrv.rest.exceptions.RestAuthInfoNotFoundException;
import org.backmeup.keysrv.rest.exceptions.RestSQLException;
import org.backmeup.keysrv.rest.exceptions.RestServiceAlreadyExistException;
import org.backmeup.keysrv.rest.exceptions.RestServiceNotFoundException;
import org.backmeup.keysrv.rest.exceptions.RestUserAlreadyExistException;
import org.backmeup.keysrv.rest.exceptions.RestUserNotFoundException;
import org.backmeup.keysrv.rest.exceptions.RestWrongDecryptionKeyException;

public class DBManager
{
	private static final String DB = "db_keysrv";
	private static final String DB_USER = "dbu_keysrv";
	private static final String DB_PWD = "SAQ*$X2tX1bF.,%";
	private static final String DB_HOST = "bmu-keysrv01";
	private static final String DB_URL = "jdbc:postgresql://" + DB_HOST + "/" + DB;

	private static final String PS_INSERT_USER = "INSERT INTO users (bmu_user_id, bmu_user_pwd_hash) VALUES (?, (pgp_pub_encrypt (?, dearmor(?))))";
	private static final String PS_UPDATE_USER = "UPDATE users SET bmu_user_pwd_hash=(pgp_pub_encrypt (?, dearmor(?))) WHERE bmu_user_id=?";
	private static final String PS_SELECT_USER_BY_BMU_USER_ID = "SELECT id, bmu_user_id, pgp_pub_decrypt (bmu_user_pwd_hash, dearmor (?)) AS bmu_user_pwd_hash FROM users WHERE bmu_user_id=?";
	private static final String PS_SELECT_USER_BY_ID = "SELECT id, bmu_user_id FROM users WHERE id=?";
	private static final String PS_DELETE_USER_BY_BMU_USER_ID = "DELETE FROM users WHERE bmu_user_id=?";

	private static final String PS_INSERT_SERVICE = "INSERT INTO services (bmu_service_id) VALUES (?)";
	private static final String PS_SELECT_SERVICE_BY_BMU_SERVICE_ID = "SELECT id, bmu_service_id FROM services WHERE bmu_service_id=?";
	private static final String PS_SELECT_SERVICE_BY_ID = "SELECT id, bmu_service_id FROM services WHERE id=?";
	private static final String PS_DELETE_SERVICE_BY_BMU_SERVICE_ID = "DELETE FROM services WHERE bmu_service_id=?";

	// private static final String PS_INSERT_AUTH_INFO =
	// "INSERT INTO auth_infos (bmu_authinfo_id, user_id, service_id, ai_type, ai_username, ai_pwd, ai_oauth) VALUES"
	// + "(?, ?, ?, " + "(pgp_pub_encrypt (?, dearmor(?))), " +
	// "(pgp_pub_encrypt_bytea (?, dearmor(?))), "
	// + "(pgp_pub_encrypt_bytea (?, dearmor(?))), " +
	// "(pgp_pub_encrypt_bytea (?, dearmor(?))))";

	private static final String PS_INSERT_AUTH_INFO = "INSERT INTO auth_infos (bmu_authinfo_id, user_id, service_id, ai_key, ai_value) VALUES" + "(?, ?, ?, (pgp_pub_encrypt_bytea (?, dearmor(?))), (pgp_pub_encrypt_bytea (?, dearmor(?))))";

	// private static final String PS_SELECT_AUTH_INFO_BY_USER =
	// "SELECT id, bmu_authinfo_id, user_id, service_id, " +
	// "pgp_pub_decrypt (ai_type, dearmor (?)) AS ai_type, " +
	// "pgp_pub_decrypt_bytea (ai_username, dearmor (?)) AS ai_username, " +
	// "pgp_pub_decrypt_bytea (ai_pwd, dearmor (?)) AS ai_pwd, "
	// + "pgp_pub_decrypt_bytea (ai_oauth, dearmor (?)) AS ai_oauth " +
	// "FROM auth_infos WHERE user_id=?";

	private static final String PS_SELECT_AUTH_INFO_BY_USER = "SELECT DISTINCT auth_infos.bmu_authinfo_id AS bmu_authinfo_id, auth_infos.service_id AS service_id, services.bmu_service_id AS bmu_service_id FROM auth_infos INNER JOIN services ON services.id=auth_infos.service_id WHERE auth_infos.user_id=? ORDER BY auth_infos.bmu_authinfo_id";
	private static final String PS_SELECT_AUTH_INFO_BY_USER_SERVICE = PS_SELECT_AUTH_INFO_BY_USER + " AND service_id=?";
	private static final String PS_SELECT_AUTH_INFO_BY_BMU_AUTHINFO_ID = "SELECT id, bmu_authinfo_id, user_id, service_id, pgp_pub_decrypt_bytea (ai_key, dearmor (?)) AS ai_key, pgp_pub_decrypt_bytea (ai_value, dearmor (?)) AS ai_value FROM auth_infos WHERE bmu_authinfo_id=?";
	private static final String PS_SELECT_AUTH_INFO_BY_ID = "SELECT id, bmu_authinfo_id, user_id, service_id, pgp_pub_decrypt_bytea (ai_key, dearmor (?)) AS ai_key, pgp_pub_decrypt_bytea (ai_value, dearmor (?)) AS ai_value FROM auth_infos WHERE id=?";
	private static final String PS_DELETE_AUTH_INFO_BY_BMU_AUTHINFO_ID = "DELETE FROM auth_infos WHERE bmu_authinfo_id=?";

	private static final String PS_INSERT_TOKEN = "INSERT INTO tokens (token_id, user_id, service_id, bmu_authinfo_id, token_key, token_value, backupdate) VALUES (?, ?, ?, ?, (pgp_pub_encrypt_bytea (?, dearmor(?))), (pgp_pub_encrypt_bytea (?, dearmor(?))), (pgp_pub_encrypt_bytea (?, dearmor(?))))";
	private static final String PS_SELECT_TOKEN_BY_TOKEN_ID = "SELECT tokens.id AS id, users.bmu_user_id AS bmu_user_id, services.bmu_service_id AS bmu_service_id, tokens.bmu_authinfo_id AS bmu_authinfo_id, pgp_pub_decrypt_bytea (token_key, dearmor (?)) AS token_key, pgp_pub_decrypt_bytea (token_value, dearmor (?)) AS token_value, pgp_pub_decrypt_bytea (backupdate, dearmor (?)) AS backupdate FROM tokens INNER JOIN users ON users.id=tokens.user_id INNER JOIN services ON services.id=tokens.service_id WHERE token_id=? ORDER BY tokens.bmu_authinfo_id";
	private static final String PS_DELETE_TOKEN_BY_TOKEN_ID = "DELETE FROM tokens WHERE token_id=?";
	private static final String PS_DELETE_TOKEN_BY_ID = "DELETE FROM tokens WHERE id=?";

	// private static final String PS_WRITE_LOG = "INSERT INTO log (user_id, )";

	private static PreparedStatement ps_insert_user = null;
	private static PreparedStatement ps_update_user = null;
	private static PreparedStatement ps_select_user_by_bmu_user_id = null;
	private static PreparedStatement ps_select_user_by_id = null;
	private static PreparedStatement ps_delete_user_by_bmu_user_id = null;
	private static PreparedStatement ps_insert_service = null;
	private static PreparedStatement ps_select_service_by_bmu_service_id = null;
	private static PreparedStatement ps_select_service_by_id = null;
	private static PreparedStatement ps_delete_service_by_bmu_service_id = null;
	private static PreparedStatement ps_insert_auth_info = null;
	private static PreparedStatement ps_select_auth_info_by_user = null;
	private static PreparedStatement ps_select_auth_info_by_user_service = null;
	private static PreparedStatement ps_select_auth_info_by_bmu_authinfo_id = null;
	private static PreparedStatement ps_select_auth_info_by_id = null;
	private static PreparedStatement ps_delete_auth_info_by_bmu_authinfo_id = null;
	private static PreparedStatement ps_insert_token = null;
	private static PreparedStatement ps_select_token_by_token_id = null;
	private static PreparedStatement ps_delete_token_by_token_id = null;
	private static PreparedStatement ps_delete_token_by_id = null;

	private static Connection db_con = null;
	private PGPKeys pgpkeys = null;

	private static DataSource ds = null;

	private static void init ()
	{
		try
		{
			if ( (db_con == null) || (db_con.isClosed () == true))
			{
				InitialContext ctx = new InitialContext ();
				ds = (DataSource) ctx.lookup ("java:comp/env/jdbc/keysrvdb");
				db_con = ds.getConnection ();
				db_con.setAutoCommit (true);

				prepareStatements ();
			}
		}
		catch (Exception e)
		{
			FileLogger.logException (e);
			e.printStackTrace ();
		}
	}

	public DBManager ()
	{
		init ();

		try
		{
			this.pgpkeys = new PGPKeys ();
		}
		catch (IOException e)
		{
			FileLogger.logException (e);
			e.printStackTrace ();
		}
	}

	private static void prepareStatements () throws SQLException
	{
		ps_insert_user = db_con.prepareStatement (PS_INSERT_USER);
		ps_update_user = db_con.prepareStatement (PS_UPDATE_USER);
		ps_select_user_by_bmu_user_id = db_con.prepareStatement (PS_SELECT_USER_BY_BMU_USER_ID);
		ps_select_user_by_id = db_con.prepareStatement (PS_SELECT_USER_BY_ID);
		ps_delete_user_by_bmu_user_id = db_con.prepareStatement (PS_DELETE_USER_BY_BMU_USER_ID);

		ps_insert_service = db_con.prepareStatement (PS_INSERT_SERVICE);
		ps_select_service_by_bmu_service_id = db_con.prepareStatement (PS_SELECT_SERVICE_BY_BMU_SERVICE_ID);
		ps_select_service_by_id = db_con.prepareStatement (PS_SELECT_SERVICE_BY_ID);
		ps_delete_service_by_bmu_service_id = db_con.prepareStatement (PS_DELETE_SERVICE_BY_BMU_SERVICE_ID);

		ps_insert_auth_info = db_con.prepareStatement (PS_INSERT_AUTH_INFO);
		ps_select_auth_info_by_user = db_con.prepareStatement (PS_SELECT_AUTH_INFO_BY_USER);
		ps_select_auth_info_by_user_service = db_con.prepareStatement (PS_SELECT_AUTH_INFO_BY_USER_SERVICE);
		ps_select_auth_info_by_bmu_authinfo_id = db_con.prepareStatement (PS_SELECT_AUTH_INFO_BY_BMU_AUTHINFO_ID);
		ps_select_auth_info_by_id = db_con.prepareStatement (PS_SELECT_AUTH_INFO_BY_ID);
		ps_delete_auth_info_by_bmu_authinfo_id = db_con.prepareStatement (PS_DELETE_AUTH_INFO_BY_BMU_AUTHINFO_ID);

		ps_insert_token = db_con.prepareStatement (PS_INSERT_TOKEN, PreparedStatement.RETURN_GENERATED_KEYS);
		ps_select_token_by_token_id = db_con.prepareStatement (PS_SELECT_TOKEN_BY_TOKEN_ID);
		ps_delete_token_by_token_id = db_con.prepareStatement (PS_DELETE_TOKEN_BY_TOKEN_ID);
		ps_delete_token_by_id = db_con.prepareStatement (PS_DELETE_TOKEN_BY_ID);
	}

	public void insertAuthInfo (AuthInfo authinfo) throws RestSQLException
	{
		try
		{
			this.getAuthInfo (authinfo.getBmuAuthinfoId (), authinfo.getUser (), authinfo.getService ());
			throw new RestAuthInfoAlreadyExistException (authinfo.getBmuAuthinfoId ());
		}
		catch (RestAuthInfoNotFoundException e)
		{
		}

		try
		{
			ps_insert_auth_info.setLong (1, authinfo.getBmuAuthinfoId ());
			ps_insert_auth_info.setLong (2, authinfo.getUser ().getId ());
			ps_insert_auth_info.setLong (3, authinfo.getService ().getId ());

			for (byte[] key : authinfo.getAi_data ().keySet ())
			{
				ps_insert_auth_info.setBytes (4, key);
				ps_insert_auth_info.setString (5, pgpkeys.getPublickey ());

				ps_insert_auth_info.setBytes (6, authinfo.getAi_data ().get (key));
				ps_insert_auth_info.setString (7, pgpkeys.getPublickey ());

				ps_insert_auth_info.executeUpdate ();
			}
		}
		catch (SQLException e)
		{
			throw new RestSQLException (e);
		}
	}

	public AuthInfo getAuthInfo (long bmu_authinfo_id, User user, Service service) throws RestSQLException, RestAuthInfoNotFoundException
	{
		AuthInfo ai = null;
		HashMap<byte[], byte[]> ai_data = new HashMap<byte[], byte[]> ();
		long id = -1;

		try
		{
			ps_select_auth_info_by_bmu_authinfo_id.setString (1, pgpkeys.getPrivatekey ());
			ps_select_auth_info_by_bmu_authinfo_id.setString (2, pgpkeys.getPrivatekey ());
			ps_select_auth_info_by_bmu_authinfo_id.setLong (3, bmu_authinfo_id);

			ResultSet rs = ps_select_auth_info_by_bmu_authinfo_id.executeQuery ();

			while (rs.next () == true)
			{
				ai_data.put (rs.getBytes ("ai_key"), rs.getBytes ("ai_value"));

				if (id < 0)
				{
					id = rs.getLong ("id");
				}
			}
			rs.close ();

			ai = new AuthInfo (id, bmu_authinfo_id, user, service, ai_data);
		}
		catch (SQLException e)
		{
			throw new RestSQLException (e);
		}

		if (ai.getAi_data ().size () == 0)
		{
			throw new RestAuthInfoNotFoundException (bmu_authinfo_id);
		}

		return ai;
	}
	
	// "SELECT DISTINCT auth_infos.bmu_authinfo_id AS bmu_authinfo_id, auth_infos.service_id AS service_id,
	// services.bmu_service_id AS bmu_service_id FROM auth_infos
	// INNER JOIN services ON services.id=auth_infos.service_id WHERE auth_infos.user_id=? ORDER BY auth_infos.bmu_authinfo_id";
	public ArrayList<AuthInfo> getUserAuthInfos (User user) throws RestSQLException
	{
		ArrayList<AuthInfo> authinfos = new ArrayList<AuthInfo> ();
		
		try
		{
			ps_select_auth_info_by_user.setLong (1, user.getId ());
			
			ResultSet rs = ps_select_auth_info_by_user.executeQuery ();
			
			while (rs.next () == true)
			{
				Service service = new Service (rs.getLong ("service_id"), rs.getLong ("bmu_service_id"));
				authinfos.add (this.getAuthInfo (rs.getLong ("bmu_authinfo_id"), user, service));
			}
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}
		
		return authinfos;
	}

	public boolean existAuthInfo (long bmu_authinfo_id)
	{
		try
		{
			ps_select_auth_info_by_bmu_authinfo_id.setString (1, pgpkeys.getPrivatekey ());
			ps_select_auth_info_by_bmu_authinfo_id.setString (2, pgpkeys.getPrivatekey ());
			ps_select_auth_info_by_bmu_authinfo_id.setLong (3, bmu_authinfo_id);

			ResultSet rs = ps_select_auth_info_by_bmu_authinfo_id.executeQuery ();

			if (rs.next ())
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		catch (SQLException e)
		{
			throw new RestSQLException (e);
		}
	}

	public void deleteAuthInfo (long bmu_authinfo_id) throws RestSQLException
	{
		if (this.existAuthInfo (bmu_authinfo_id) == false)
		{
			throw new RestAuthInfoNotFoundException (bmu_authinfo_id);
		}

		try
		{
			ps_delete_auth_info_by_bmu_authinfo_id.setLong (1, bmu_authinfo_id);

			ps_delete_auth_info_by_bmu_authinfo_id.executeUpdate ();
		}
		catch (SQLException e)
		{
			throw new RestSQLException (e);
		}
	}

	public void insertUser (long bmu_user_id) throws SQLException
	{
		ps_insert_user.setLong (1, bmu_user_id);

		ps_insert_user.executeUpdate ();
	}

	public void insertUser (User user) throws RestSQLException
	{
		try
		{
			this.getUser (user.getBmuId ());
			throw new RestUserAlreadyExistException (user.getBmuId ());
		}
		catch (RestUserNotFoundException e)
		{
		}

		try
		{
			ps_insert_user.setLong (1, user.getBmuId ());
			ps_insert_user.setString (2, user.getPwd_hash ());
			ps_insert_user.setString (3, pgpkeys.getPublickey ());

			ps_insert_user.executeUpdate ();
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}
	}

	public User getUser (long bmu_user_id) throws RestUserNotFoundException, RestSQLException
	{
		User user = null;

		try
		{
			ps_select_user_by_bmu_user_id.setString (1, pgpkeys.getPrivatekey ());
			ps_select_user_by_bmu_user_id.setLong (2, bmu_user_id);

			ResultSet rs = ps_select_user_by_bmu_user_id.executeQuery ();
			if (rs.next ())
			{
				user = new User (rs.getLong ("id"), rs.getLong ("bmu_user_id"));
				user.setPwd_hash (rs.getString ("bmu_user_pwd_hash"));
			}
			rs.close ();
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}

		if (user == null)
		{
			throw new RestUserNotFoundException (bmu_user_id);
		}

		return user;
	}

	public void changeUser (User user) throws RestSQLException
	{
		try
		{
			ps_update_user.setString (1, user.getPwd_hash ());
			ps_update_user.setString (2, pgpkeys.getPublickey ());
			ps_update_user.setLong (3, user.getBmuId ());
			
			ps_update_user.executeUpdate ();
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
		}
	}

	public void selectUserByBmuUserId (long bmu_user_id) throws SQLException
	{
		ps_select_user_by_bmu_user_id.setLong (1, bmu_user_id);

		ResultSet rs = ps_select_user_by_bmu_user_id.executeQuery ();
		if (rs.next ())
		{
			System.out.println (rs.getString ("id"));
			System.out.println (rs.getString ("bmu_user_id"));
		}
		rs.close ();
	}

	public User getUserById (long id) throws SQLException
	{
		ps_select_user_by_id.setLong (1, id);

		ResultSet rs = ps_select_user_by_id.executeQuery ();

		User user = null;
		if (rs.next ())
		{
			user = new User (rs.getLong ("id"), rs.getLong ("bmu_user_id"));
		}
		rs.close ();

		return user;
	}

	public void deleteUser (User user) throws RestSQLException
	{
		this.getUser (user.getBmuId ());

		try
		{
			ps_delete_user_by_bmu_user_id.setLong (1, user.getBmuId ());

			ps_delete_user_by_bmu_user_id.executeUpdate ();
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}
	}

	public void insertService (long bmu_service_id) throws SQLException
	{
		ps_insert_service.setLong (1, bmu_service_id);

		ps_insert_service.executeUpdate ();
	}

	public void insertService (Service service) throws RestSQLException, RestServiceAlreadyExistException
	{
		try
		{
			this.getService (service.getBmuId ());
			throw new RestServiceAlreadyExistException (service.getBmuId ());
		}
		catch (RestServiceNotFoundException e)
		{
		}

		try
		{
			ps_insert_service.setLong (1, service.getBmuId ());

			ps_insert_service.executeUpdate ();
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}
	}

	public Service getService (long bmu_service_id) throws RestSQLException, RestServiceNotFoundException
	{
		Service service = null;

		try
		{
			ps_select_service_by_bmu_service_id.setLong (1, bmu_service_id);

			ResultSet rs = ps_select_service_by_bmu_service_id.executeQuery ();
			if (rs.next ())
			{
				service = new Service (rs.getLong ("id"), rs.getLong ("bmu_service_id"));
			}
			rs.close ();
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}

		if (service == null)
		{
			throw new RestServiceNotFoundException (bmu_service_id);
		}

		return service;
	}

	public void selectServiceByBmuServiceId (long bmu_service_id) throws SQLException
	{
		ps_select_service_by_bmu_service_id.setLong (1, bmu_service_id);

		ResultSet rs = ps_select_service_by_bmu_service_id.executeQuery ();
		rs.close ();
	}

	public void getServiceById (long id) throws SQLException
	{
		ps_select_service_by_id.setLong (1, id);

		ResultSet rs = ps_select_service_by_id.executeQuery ();
		rs.close ();
	}

	public void deleteService (Service service) throws RestSQLException
	{
		this.getService (service.getBmuId ());

		try
		{
			ps_delete_service_by_bmu_service_id.setLong (1, service.getBmuId ());

			ps_delete_service_by_bmu_service_id.executeUpdate ();
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}
	}

	// "INSERT INTO tokens (token_id, user_id, service_id, bmu_authinfo_id,
	// token_key, token_value, backupdate) VALUES
	// (?, ?, ?, ?, (pgp_pub_encrypt_bytea (?, dearmor(?))),
	// (pgp_pub_encrypt_bytea (?, dearmor(?))),
	// (pgp_pub_encrypt_bytea (?, dearmor(?))))";
	public long insertToken (Token token) throws RestSQLException
	{
		long token_id = -1;

		try
		{
			CipherGenerator cipher = new CipherGenerator ();

			// Create first entry to get an valid and unique id
			ps_insert_token.setLong (1, -1);
			ps_insert_token.setLong (2, token.getUser ().getId ());
			ps_insert_token.setLong (3, token.getAuthInfo (0).getService ().getId ());
			ps_insert_token.setLong (4, token.getAuthInfo (0).getBmuAuthinfoId ());
			ps_insert_token.setBytes (5, "asd".getBytes ());
			ps_insert_token.setString (6, pgpkeys.getPublickey ());
			ps_insert_token.setBytes (7, "asd".getBytes ());
			ps_insert_token.setString (8, pgpkeys.getPublickey ());
			ps_insert_token.setBytes (9, cipher.encData (token.getBackupdate ().getTime () + "", token.getTokenpwd ()));
			ps_insert_token.setString (10, pgpkeys.getPublickey ());

			ps_insert_token.executeUpdate ();
			ResultSet rs = ps_insert_token.getGeneratedKeys ();

			if (rs.next ())
			{
				token_id = rs.getLong (1);
				rs.close ();
			}

			ps_insert_token.setLong (1, token_id);

			User tokenuser = new User (-1);
			tokenuser.setPwd (token.getTokenpwd ());

			for (int i = 0; i < token.getAuthInfoCount (); i++)
			{
				HashMap<String, String> ai_data = token.getAuthInfo (i).getDecAi_data ();
				HashMap<byte[], byte[]> ai_enc_data = cipher.encData (ai_data, tokenuser);

				for (byte[] key : ai_enc_data.keySet ())
				{
					ps_insert_token.setLong (3, token.getAuthInfo (i).getService ().getId ());
					ps_insert_token.setLong (4, token.getAuthInfo (i).getBmuAuthinfoId ());
					ps_insert_token.setBytes (5, key);
					ps_insert_token.setBytes (7, ai_enc_data.get (key));
					ps_insert_token.executeUpdate ();
				}
			}

			// remove the extra line from database
			ps_delete_token_by_id.setLong (1, token_id);
			ps_delete_token_by_id.executeUpdate ();
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}

		return token_id;
	}

	public Token getTokenData (long token_id, String token_pwd) throws RestSQLException
	{
		Token token = null;

		try
		{
			ps_select_token_by_token_id.setString (1, pgpkeys.getPrivatekey ());
			ps_select_token_by_token_id.setString (2, pgpkeys.getPrivatekey ());
			ps_select_token_by_token_id.setString (3, pgpkeys.getPrivatekey ());
			ps_select_token_by_token_id.setLong (4, token_id);

			ResultSet rs = ps_select_token_by_token_id.executeQuery ();

			CipherGenerator cipher = new CipherGenerator ();

			User user = null;
			AuthInfo ai = null;
			HashMap<byte[], byte[]> ai_data = null;
			while (rs.next ())
			{
				if (token == null)
				{
					user = new User (rs.getLong ("bmu_user_id"));
					user.setPwd (token_pwd);
					
					String str_backupdate = "";
					str_backupdate = cipher.decData (rs.getBytes ("backupdate"), user);
					Date backupdate = new Date (new Long (str_backupdate));

					token = new Token (user, backupdate);
					token.setTokenpwd (token_pwd);
				}

				Service service = new Service (rs.getLong ("bmu_service_id"));

				if (ai == null)
				{
					ai = new AuthInfo (rs.getLong ("bmu_authinfo_id"), user, service);
					ai_data = new HashMap<byte[], byte[]> ();
				}
				if (ai.getBmuAuthinfoId () != rs.getLong ("bmu_authinfo_id"))
				{
					ai.setAi_data (ai_data);
					token.addAuthInfo (ai);

					ai = new AuthInfo (rs.getLong ("bmu_authinfo_id"), user, service);
					ai_data = new HashMap<byte[], byte[]> ();

					ai_data.put (rs.getBytes ("token_key"), rs.getBytes ("token_value"));
				}
				else
				{
					ai_data.put (rs.getBytes ("token_key"), rs.getBytes ("token_value"));
				}
			}

			ai.setAi_data (ai_data);
			token.addAuthInfo (ai);

			rs.close ();

			ps_delete_token_by_token_id.setLong (1, token_id);
			ps_delete_token_by_token_id.executeUpdate ();

		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}

		return token;
	}
}
