package org.backmeup.keysrv;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Random;

public class DBManager
{
	private final String DB = "db_keysrv";
	private final String DB_USER = "dbu_keysrv";
	private final String DB_PWD = "SAQ*$X2tX1bF.,%";
	private final String DB_HOST = "bmu-keysrv01";
	private final String DB_URL = "jdbc:postgresql://" + DB_HOST + "/" + DB;
	
	private final String PS_INSERT_USER = "INSERT INTO users (name, name_hash) VALUES (pgp_pub_encrypt (?, dearmor(?)), ?)";
	private final String PS_SELECT_USER_BY_NAME = "SELECT id, pgp_pub_decrypt (name, dearmor (?)) AS name FROM users WHERE name_hash=?";
	private final String PS_SELECT_USER_BY_ID = "SELECT id, pgp_pub_decrypt (name, dearmor (?)) AS name FROM users WHERE id=?";
	
	private final String PS_INSERT_SERVICE = "INSERT INTO services (name, name_hash) VALUES (pgp_pub_encrypt (?, dearmor(?)), ?)";
	private final String PS_SELECT_SERVICE_BY_NAME = "SELECT id, pgp_pub_decrypt (name, dearmor (?)) AS name FROM services WHERE name_hash=?";
	private final String PS_SELECT_SERVICE_BY_ID = "SELECT id, pgp_pub_decrypt (name, dearmor (?)) AS name FROM services WHERE id=?";
	
	private final String PS_INSERT_AUTH_INFO = "INSERT INTO auth_infos (user_id, service_id, token_id, ai_type, ai_username, ai_pwd, ai_oauth) VALUES (" +
			"?, ?, ?, " +
			"(pgp_pub_encrypt (?, dearmor(?)), " +
			"(pgp_pub_encrypt_bytea (?, dearmor(?)), " +
			"(pgp_pub_encrypt_bytea (?, dearmor(?)), " +
			"(pgp_pub_encrypt_bytea (?, dearmor(?)))";
	private final String PS_SELECT_AUTH_INFO_BY_USER = "SELECT id, " +
			"pgp_pub_decrypt (name, dearmor (?)) AS name, " +
			"pgp_pub_decrypt (ai_type, dearmor (?)) AS ai_type, " +
			"pgp_pub_decrypt_bytea (ai_username, dearmor (?)) AS ai_username, " +
			"pgp_pub_decrypt_bytea (ai_pwd, dearmor (?)) AS ai_pwd, " +
			"pgp_pub_decrypt_bytea (ai_oauth, dearmor (?)) AS ai_oauth " +
			"FROM auth_infos WHERE user_id=?";
	private final String PS_SELECT_AUTH_INFO_BY_USER_SERVICE = PS_SELECT_AUTH_INFO_BY_USER + " AND service_id=?";
	private final String PS_SELECT_AUTH_INFO_BY_ID = "SELECT id, user_id, service_id, token_id, " +
		"pgp_pub_decrypt (name, dearmor (?)) AS name, " +
		"pgp_pub_decrypt (ai_type, dearmor (?)) AS ai_type, " +
		"pgp_pub_decrypt_bytea (ai_username, dearmor (?)) AS ai_username, " +
		"pgp_pub_decrypt_bytea (ai_pwd, dearmor (?)) AS ai_pwd, " +
		"pgp_pub_decrypt_bytea (ai_oauth, dearmor (?)) AS ai_oauth " +
		"FROM auth_infos WHERE id=?";
	
	private PreparedStatement ps_insert_user = null;
	private PreparedStatement ps_select_user_by_name = null;
	private PreparedStatement ps_select_user_by_id = null;
	private PreparedStatement ps_insert_service = null;
	private PreparedStatement ps_select_service_by_name = null;
	private PreparedStatement ps_select_service_by_id = null;
	private PreparedStatement ps_insert_auth_info = null;
	private PreparedStatement ps_select_auth_info_by_user = null;
	private PreparedStatement ps_select_auth_info_by_user_service = null;
	private PreparedStatement ps_select_auth_info_by_id = null;
	
	private Connection db_con = null;
	private PGPKeys pgpkeys = null;
	
	public DBManager () throws SQLException, IOException
	{
		db_con = DriverManager.getConnection (DB_URL, DB_USER, DB_PWD);
		db_con.setAutoCommit (false);
		
		pgpkeys = new PGPKeys ();
		this.prepareStatements ();
	}
	
	protected void finalize ()
	{
		this.closeQuiet (ps_insert_user);
		this.closeQuiet (ps_select_user_by_name);
		this.closeQuiet (ps_select_user_by_id);
		
		this.closeQuiet (ps_insert_service);
		this.closeQuiet (ps_select_service_by_name);
		this.closeQuiet (ps_select_service_by_id);
		
		this.closeQuiet (ps_insert_auth_info);
		this.closeQuiet (ps_select_auth_info_by_user);
		this.closeQuiet (ps_select_auth_info_by_user_service);
		this.closeQuiet (ps_select_auth_info_by_id);
		
		this.closeQuiet (db_con);
	}
	
	private void closeQuiet (PreparedStatement ps)
	{
		try
		{
			ps.close ();
		}
		catch (Exception e)
		{
			// Ignore
		}
	}
	
	private void closeQuiet (Connection con)
	{
		try
		{
			con.close ();
		}
		catch (Exception e)
		{
			// Ignore
		}
	}
	
	private void prepareStatements () throws SQLException
	{
		ps_insert_user = db_con.prepareStatement (PS_INSERT_USER);
		ps_select_user_by_name = db_con.prepareStatement (PS_SELECT_USER_BY_NAME);
		ps_select_user_by_id = db_con.prepareStatement (PS_SELECT_USER_BY_ID);
		
		ps_insert_service = db_con.prepareStatement (PS_INSERT_SERVICE);
		ps_select_service_by_name = db_con.prepareStatement (PS_SELECT_SERVICE_BY_NAME);
		ps_select_service_by_id = db_con.prepareStatement (PS_SELECT_SERVICE_BY_ID);
		
		ps_insert_auth_info = db_con.prepareStatement (PS_INSERT_AUTH_INFO);
		ps_select_auth_info_by_user = db_con.prepareStatement (PS_SELECT_AUTH_INFO_BY_USER);
		ps_select_auth_info_by_user_service = db_con.prepareStatement (PS_SELECT_AUTH_INFO_BY_USER_SERVICE);
		ps_select_auth_info_by_id = db_con.prepareStatement (PS_SELECT_AUTH_INFO_BY_ID);
	}
	
	public void insertAuthInfo (User user, Service service, AuthInfo authinfo) throws SQLException
	{
		/*
		ps_insert_auth_info.setLong (1, user.getId ());
		ps_insert_auth_info.setLong (2, service.getId ());
		ps_insert_auth_info.setLong (3, 0);
		ps_insert_auth_info.setString (4, "userpwd");
		ps_insert_auth_info.setString (5, pgpkeys.getPublickey ());
		ps_insert_auth_info.setBytes (6, authinfo.encryptAiUsername ());
		ps_insert_auth_info.setString (7, pgpkeys.getPublickey ());
		ps_insert_auth_info.setBytes (8, authinfo.encryptAiPwd ());
		ps_insert_auth_info.setString (9, pgpkeys.getPublickey ());
		ps_insert_auth_info.setBytes (10, authinfo.encryptAiOAuth ());
		ps_insert_auth_info.setString (11, pgpkeys.getPublickey ());
		*/
		
		ps_insert_user.executeUpdate ();
		db_con.commit ();
	}
	
	public void insertUser (String username) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		ps_insert_user.setString (1, username);
		ps_insert_user.setString (2, pgpkeys.getPublickey ());
		ps_insert_user.setString (3, this.calcSHA512 (username));
		
		ps_insert_user.executeUpdate ();
		db_con.commit ();
	}
	
	public void insertUser (User user) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		if (this.getUser (user.getName ()) != null)
		{
			throw new SQLException ("User \"" + user.getName () + "\" does already exist");
		}
		
		ps_insert_user.setString (1, user.getName ());
		ps_insert_user.setString (2, pgpkeys.getPublickey ());
		ps_insert_user.setString (3, this.calcSHA512 (user.getName ()));
		
		ps_insert_user.executeUpdate ();
		db_con.commit ();
	}
	
	public User getUser (String username) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		ps_select_user_by_name.setString (1, pgpkeys.getPrivatekey ());
		ps_select_user_by_name.setString (2, this.calcSHA512 (username));
		
		User user = null;
		
		ResultSet rs = ps_select_user_by_name.executeQuery ();
		if (rs.next ())
		{
			user = new User (rs.getLong ("id"), rs.getString ("name"));
		}
		rs.close ();
		
		return user;
	}
	
	public void selectUserByName (String username) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		ps_select_user_by_name.setString (1, pgpkeys.getPrivatekey ());
		ps_select_user_by_name.setString (2, this.calcSHA512 (username));
		
		ResultSet rs = ps_select_user_by_name.executeQuery ();
		if (rs.next ())
		{
			System.out.println (rs.getString ("id"));
			System.out.println (rs.getString ("name"));
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
			System.out.println (rs.getString ("name"));
		}
		rs.close ();
	}
	
	public void insertService (String service) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		ps_insert_service.setString (1, service);
		ps_insert_service.setString (2, pgpkeys.getPublickey ());
		ps_insert_service.setString (3, this.calcSHA512 (service));
		
		ps_insert_service.executeUpdate ();
		db_con.commit ();
	}
	
	public void insertService (Service service) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		if (this.getService (service.getName ()) != null)
		{
			throw new SQLException ("Service \"" + service.getName () + "\" does already exist");
		}
		
		ps_insert_service.setString (1, service.getName ());
		ps_insert_service.setString (2, pgpkeys.getPublickey ());
		ps_insert_service.setString (3, this.calcSHA512 (service.getName ()));

		ps_insert_service.executeUpdate ();
		db_con.commit ();
	}
	
	public Service getService (String servicename) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		ps_select_service_by_name.setString (1, pgpkeys.getPrivatekey ());
		ps_select_service_by_name.setString (2, this.calcSHA512 (servicename));
		
		Service service = null;
		
		ResultSet rs = ps_select_service_by_name.executeQuery ();
		if (rs.next ())
		{
			service = new Service (rs.getLong ("id"), rs.getString ("name"));
		}
		rs.close ();
		
		return service;
	}
	
	public void selectServiceByName (String service) throws SQLException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		ps_select_service_by_name.setString (1, pgpkeys.getPrivatekey ());
		ps_select_service_by_name.setString (2, this.calcSHA512 (service));
		
		ResultSet rs = ps_select_service_by_name.executeQuery ();
		rs.close ();
	}
	
	public void selectServiceById (long id) throws SQLException
	{
		ps_select_service_by_id.setString (1, pgpkeys.getPrivatekey ());
		ps_select_service_by_id.setLong (2, id);
		
		ResultSet rs = ps_select_service_by_id.executeQuery ();
		rs.close ();
	}
	
	public void insertRandom () throws SQLException
	{
		String query = "INSERT INTO users (name) VALUES (pgp_pub_encrypt (?, dearmor(?)))";
		//query = "INSERT INTO services (name) VALUES (?)";
		
		Random rnd = new Random ();
		PreparedStatement ps = db_con.prepareStatement (query);
		
		for (int i = 0; i < 1000; i++)
		{
			String str = "" + rnd.nextInt ();
			
			ps.setString (1, str);
			ps.setString (2, pgpkeys.getPublickey ());
			
			int result = ps.executeUpdate ();
			
			System.out.println (str);
		}
		
		db_con.commit ();
	}
	
	/**
	 * Returns an sha512 hash (hex formated) of an given string value (UTF-8)
	 * 
	 * @param value
	 * @return
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 */
	private String calcSHA512 (String value) throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		String algo = "SHA-512";
		String charset = "UTF-8";
		
		MessageDigest md = MessageDigest.getInstance (algo);
		
		byte[] hash = md.digest (value.getBytes (charset));
		
		StringBuffer sb = new StringBuffer ();
		for (int i = 0; i < hash.length; i++)
		{
			sb.append (Integer.toString ( (hash[i] & 0xff) + 0x100, 16).substring (1));
		}
		
		return sb.toString ();
	}
}
