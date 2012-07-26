package org.backmeup.keysrv.worker;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Random;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.backmeup.keysrv.rest.exceptions.RestAuthInfoAlreadyExistException;
import org.backmeup.keysrv.rest.exceptions.RestAuthInfoNotFoundException;
import org.backmeup.keysrv.rest.exceptions.RestSQLException;
import org.backmeup.keysrv.rest.exceptions.RestServiceAlreadyExistException;
import org.backmeup.keysrv.rest.exceptions.RestServiceNotFoundException;
import org.backmeup.keysrv.rest.exceptions.RestUserAlreadyExistException;
import org.backmeup.keysrv.rest.exceptions.RestUserNotFoundException;

public class DBManager
{
	private static final String DB = "db_keysrv";
	private static final String DB_USER = "dbu_keysrv";
	private static final String DB_PWD = "SAQ*$X2tX1bF.,%";
	private static final String DB_HOST = "bmu-keysrv01";
	private static final String DB_URL = "jdbc:postgresql://" + DB_HOST + "/" + DB;

	private static final String PS_INSERT_USER = "INSERT INTO users (bmu_user_id) VALUES (?)";
	private static final String PS_SELECT_USER_BY_BMU_USER_ID = "SELECT id, bmu_user_id FROM users WHERE bmu_user_id=?";
	private static final String PS_SELECT_USER_BY_ID = "SELECT id, bmu_user_id FROM users WHERE id=?";
	private static final String PS_DELETE_USER_BY_BMU_USER_ID = "DELETE FROM users WHERE bmu_user_id=?";

	private static final String PS_INSERT_SERVICE = "INSERT INTO services (bmu_service_id) VALUES (?)";
	private static final String PS_SELECT_SERVICE_BY_BMU_SERVICE_ID = "SELECT id, bmu_service_id FROM services WHERE bmu_service_id=?";
	private static final String PS_SELECT_SERVICE_BY_ID = "SELECT id, bmu_service_id FROM services WHERE id=?";
	private static final String PS_DELETE_SERVICE_BY_BMU_SERVICE_ID = "DELETE FROM services WHERE bmu_service_id=?";

	private static final String PS_INSERT_AUTH_INFO = "INSERT INTO auth_infos (bmu_authinfo_id, user_id, service_id, ai_type, ai_username, ai_pwd, ai_oauth) VALUES" + "(?, ?, ?, " + "(pgp_pub_encrypt (?, dearmor(?))), " + "(pgp_pub_encrypt_bytea (?, dearmor(?))), "
			+ "(pgp_pub_encrypt_bytea (?, dearmor(?))), " + "(pgp_pub_encrypt_bytea (?, dearmor(?))))";

	private static final String PS_SELECT_AUTH_INFO_BY_USER = "SELECT id, bmu_authinfo_id, user_id, service_id, " + "pgp_pub_decrypt (ai_type, dearmor (?)) AS ai_type, " + "pgp_pub_decrypt_bytea (ai_username, dearmor (?)) AS ai_username, " + "pgp_pub_decrypt_bytea (ai_pwd, dearmor (?)) AS ai_pwd, "
			+ "pgp_pub_decrypt_bytea (ai_oauth, dearmor (?)) AS ai_oauth " + "FROM auth_infos WHERE user_id=?";

	private static final String PS_SELECT_AUTH_INFO_BY_USER_SERVICE = PS_SELECT_AUTH_INFO_BY_USER + " AND service_id=?";

	private static final String PS_SELECT_AUTH_INFO_BY_BMU_AUTHINFO_ID = "SELECT id, bmu_authinfo_id, user_id, service_id, " + "pgp_pub_decrypt (ai_type, dearmor (?)) AS ai_type, " + "pgp_pub_decrypt_bytea (ai_username, dearmor (?)) AS ai_username, "
			+ "pgp_pub_decrypt_bytea (ai_pwd, dearmor (?)) AS ai_pwd, " + "pgp_pub_decrypt_bytea (ai_oauth, dearmor (?)) AS ai_oauth " + "FROM auth_infos WHERE bmu_authinfo_id=?";

	private static final String PS_SELECT_AUTH_INFO_BY_ID = "SELECT id, bmu_authinfo_id, user_id, service_id, token_id, " + "pgp_pub_decrypt (name, dearmor (?)) AS name, " + "pgp_pub_decrypt (ai_type, dearmor (?)) AS ai_type, " + "pgp_pub_decrypt_bytea (ai_username, dearmor (?)) AS ai_username, "
			+ "pgp_pub_decrypt_bytea (ai_pwd, dearmor (?)) AS ai_pwd, " + "pgp_pub_decrypt_bytea (ai_oauth, dearmor (?)) AS ai_oauth " + "FROM auth_infos WHERE id=?";

	private static final String PS_DELETE_AUTH_INFO_BY_BMU_AUTHINFO_ID = "DELETE FROM auth_infos WHERE bmu_authinfo_id=?";

	private static final String PS_INSERT_TOKEN = "INSERT INTO tokens (token_pwd) VALUES ((pgp_pub_encrypt_bytea (?, dearmor(?))))";
	private static final String PS_SELECT_TOKEN_BY_ID = "SELECT id, pgp_pub_decrypt_bytea (token_pwd, dearmor (?)) AS token_pwd FROM tokens WHERE id=?";
	private static final String PS_DELETE_TOKEN_BY_ID = "DELETE FROM tokens WHERE id=?";

	private static PreparedStatement ps_insert_user = null;
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
	private static PreparedStatement ps_select_token_by_id = null;
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
			e.printStackTrace();
		}
	}

	private static void prepareStatements () throws SQLException
	{
		ps_insert_user = db_con.prepareStatement (PS_INSERT_USER);
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
		ps_select_token_by_id = db_con.prepareStatement (PS_SELECT_TOKEN_BY_ID);
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
			ps_insert_auth_info.setString (4, Integer.toString (authinfo.getAi_type ()));
			ps_insert_auth_info.setString (5, pgpkeys.getPublickey ());
			ps_insert_auth_info.setBytes (6, authinfo.getAi_username ());
			ps_insert_auth_info.setString (7, pgpkeys.getPublickey ());
			ps_insert_auth_info.setBytes (8, authinfo.getAi_pwd ());
			ps_insert_auth_info.setString (9, pgpkeys.getPublickey ());
			ps_insert_auth_info.setBytes (10, authinfo.getAi_oauth ());
			ps_insert_auth_info.setString (11, pgpkeys.getPublickey ());

			ps_insert_auth_info.executeUpdate ();
		}
		catch (SQLException e)
		{
			throw new RestSQLException (e);
		}
	}

	public AuthInfo getAuthInfo (long bmu_authinfo_id, User user, Service service) throws RestSQLException, RestAuthInfoNotFoundException
	{
		AuthInfo ai = null;

		try
		{
			ps_select_auth_info_by_bmu_authinfo_id.setString (1, pgpkeys.getPrivatekey ());
			ps_select_auth_info_by_bmu_authinfo_id.setString (2, pgpkeys.getPrivatekey ());
			ps_select_auth_info_by_bmu_authinfo_id.setString (3, pgpkeys.getPrivatekey ());
			ps_select_auth_info_by_bmu_authinfo_id.setString (4, pgpkeys.getPrivatekey ());
			ps_select_auth_info_by_bmu_authinfo_id.setLong (5, bmu_authinfo_id);

			ResultSet rs = ps_select_auth_info_by_bmu_authinfo_id.executeQuery ();

			if (rs.next ())
			{
				ai = new AuthInfo (rs.getLong ("id"), rs.getLong ("bmu_authinfo_id"), user, service, rs.getInt ("ai_type"), rs.getBytes ("ai_username"), rs.getBytes ("ai_pwd"), rs.getBytes ("ai_oauth"));
			}
			rs.close ();
		}
		catch (SQLException e)
		{
			throw new RestSQLException (e);
		}

		if (ai == null)
		{
			throw new RestAuthInfoNotFoundException (bmu_authinfo_id);
		}

		return ai;
	}
	
	public boolean existAuthInfo (long bmu_authinfo_id)
	{
		try
		{
			ps_select_auth_info_by_bmu_authinfo_id.setString (1, pgpkeys.getPrivatekey ());
			ps_select_auth_info_by_bmu_authinfo_id.setString (2, pgpkeys.getPrivatekey ());
			ps_select_auth_info_by_bmu_authinfo_id.setString (3, pgpkeys.getPrivatekey ());
			ps_select_auth_info_by_bmu_authinfo_id.setString (4, pgpkeys.getPrivatekey ());
			ps_select_auth_info_by_bmu_authinfo_id.setLong (5, bmu_authinfo_id);

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
			ps_select_user_by_bmu_user_id.setLong (1, bmu_user_id);

			ResultSet rs = ps_select_user_by_bmu_user_id.executeQuery ();
			if (rs.next ())
			{
				user = new User (rs.getLong ("id"), rs.getLong ("bmu_user_id"));
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

	public void selectUserById (long id) throws SQLException
	{
		ps_select_user_by_id.setLong (1, id);

		ResultSet rs = ps_select_user_by_id.executeQuery ();
		if (rs.next ())
		{
			System.out.println (rs.getString ("id"));
			System.out.println (rs.getString ("bmu_user_id"));
		}
		rs.close ();
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

	public void selectServiceById (long id) throws SQLException
	{
		ps_select_service_by_id.setString (1, pgpkeys.getPrivatekey ());
		ps_select_service_by_id.setLong (2, id);

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

	public long insertToken (Token token) throws SQLException
	{
		ps_insert_token.setBytes (1, token.getTokenpwd ().getBytes ());
		ps_insert_token.setString (2, pgpkeys.getPublickey ());

		ps_insert_token.executeUpdate ();
		ResultSet rs = ps_insert_token.getGeneratedKeys ();

		if (rs.next ())
		{
			long id = rs.getLong (1);
			rs.close ();
			return id;
		}

		return -1;
	}

	public String getTokenPwd (long id) throws SQLException
	{
		ps_select_token_by_id.setString (1, pgpkeys.getPrivatekey ());
		ps_select_token_by_id.setLong (2, id);

		ResultSet rs = ps_select_token_by_id.executeQuery ();

		String pwd = "";

		if (rs.next ())
		{
			try
			{
				pwd = new String (rs.getBytes ("token_pwd"), "UTF-8");
			}
			catch (UnsupportedEncodingException e)
			{
				FileLogger.logException (e);
				pwd = "";
			}
		}

		ps_delete_token_by_id.setLong (1, id);
		ps_delete_token_by_id.executeUpdate ();

		return pwd;
	}
}
