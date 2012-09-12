package org.backmeup.keysrv.dal.postgres.impl;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

import org.backmeup.keyserver.dal.TokenDao;
import org.backmeup.keysrv.rest.exceptions.RestSQLException;
import org.backmeup.keysrv.rest.exceptions.RestTokenNotFoundException;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.CipherGenerator;
import org.backmeup.keysrv.worker.FileLogger;
import org.backmeup.keysrv.worker.PGPKeys;
import org.backmeup.keysrv.worker.Service;
import org.backmeup.keysrv.worker.Token;
import org.backmeup.keysrv.worker.User;

public class TokenDaoImpl implements TokenDao
{
	private static final String PS_INSERT_TOKEN = "INSERT INTO tokens (token_id, user_id, service_id, bmu_authinfo_id, reusable, token_key, token_value, backupdate) VALUES (?, ?, ?, ?, ?, (pgp_pub_encrypt_bytea (?, dearmor(?))), (pgp_pub_encrypt_bytea (?, dearmor(?))), (pgp_pub_encrypt_bytea (?, dearmor(?))))";
	private static final String PS_SELECT_TOKEN_BY_TOKEN_ID = "SELECT tokens.id AS id, users.id AS user_id, users.bmu_user_id AS bmu_user_id, services.id AS service_id, services.bmu_service_id AS bmu_service_id, tokens.bmu_authinfo_id AS bmu_authinfo_id, tokens.reusable AS reusable , pgp_pub_decrypt_bytea (token_key, dearmor (?)) AS token_key, pgp_pub_decrypt_bytea (token_value, dearmor (?)) AS token_value, pgp_pub_decrypt_bytea (backupdate, dearmor (?)) AS backupdate FROM tokens INNER JOIN users ON users.id=tokens.user_id INNER JOIN services ON services.id=tokens.service_id WHERE token_id=? ORDER BY tokens.bmu_authinfo_id";
	private static final String PS_DELETE_TOKEN_BY_TOKEN_ID = "DELETE FROM tokens WHERE token_id=?";
	private static final String PS_DELETE_TOKEN_BY_ID = "DELETE FROM tokens WHERE id=?";
	
	private PGPKeys pgpkeys;
	
	public TokenDaoImpl ()
	{
		try
		{
			pgpkeys = new PGPKeys ();
		}
		catch (IOException e)
		{
			// should not come up
			FileLogger.logException (e);
			e.printStackTrace();
		}
	}
	
	@Override
	public long insertToken (Token token)
	{
		long token_id = -1;
		
		PreparedStatement ps_insert = null;
		PreparedStatement ps_delete = null;
		ResultSet rs = null;

		try
		{
			CipherGenerator cipher = new CipherGenerator ();
			ps_insert = Connection.getPreparedStatement (PS_INSERT_TOKEN, PreparedStatement.RETURN_GENERATED_KEYS);

			// Create first entry to get an valid and unique id
			ps_insert.setLong (1, -1);
			ps_insert.setLong (2, token.getUser ().getId ());
			ps_insert.setLong (3, token.getAuthInfo (0).getService ().getId ());
			ps_insert.setLong (4, token.getAuthInfo (0).getBmuAuthinfoId ());
			ps_insert.setBoolean (5, token.isReusable ());
			ps_insert.setBytes (6, "asd".getBytes ());
			ps_insert.setString (7, pgpkeys.getPublickey ());
			ps_insert.setBytes (8, "asd".getBytes ());
			ps_insert.setString (9, pgpkeys.getPublickey ());
			ps_insert.setBytes (10, cipher.encData (token.getBackupdate ().getTime () + "", token.getTokenpwd ()));
			ps_insert.setString (11, pgpkeys.getPublickey ());

			ps_insert.executeUpdate ();
			rs = ps_insert.getGeneratedKeys ();

			if (rs.next ())
			{
				token_id = rs.getLong (1);
			}

			ps_insert.setLong (1, token_id);

			User tokenuser = new User (-1);
			tokenuser.setPwd (token.getTokenpwd ());

			for (int i = 0; i < token.getAuthInfoCount (); i++)
			{
				HashMap<String, String> ai_data = token.getAuthInfo (i).getDecAi_data ();
				HashMap<byte[], byte[]> ai_enc_data = cipher.encData (ai_data, tokenuser);

				for (byte[] key : ai_enc_data.keySet ())
				{
					ps_insert.setLong (3, token.getAuthInfo (i).getService ().getId ());
					ps_insert.setLong (4, token.getAuthInfo (i).getBmuAuthinfoId ());
					ps_insert.setBytes (6, key);
					ps_insert.setBytes (8, ai_enc_data.get (key));
					ps_insert.executeUpdate ();
				}
			}

			// remove the extra line from database
			ps_delete = Connection.getPreparedStatement (PS_DELETE_TOKEN_BY_ID);
			ps_delete.setLong (1, token_id);
			ps_delete.executeUpdate ();
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}
		finally
		{
			Connection.closeQuiet (rs);
			Connection.closeQuiet (ps_insert);
			Connection.closeQuiet (ps_delete);
		}

		return token_id;
	}

	@Override
	public Token getTokenData (long token_id, String token_pwd)
	{
		Token token = null;
		
		PreparedStatement ps_select = null;
		PreparedStatement ps_delete = null;

		try
		{
			ps_select = Connection.getPreparedStatement (PS_SELECT_TOKEN_BY_TOKEN_ID);
			
			ps_select.setString (1, pgpkeys.getPrivatekey ());
			ps_select.setString (2, pgpkeys.getPrivatekey ());
			ps_select.setString (3, pgpkeys.getPrivatekey ());
			ps_select.setLong (4, token_id);

			ResultSet rs = ps_select.executeQuery ();

			CipherGenerator cipher = new CipherGenerator ();

			User user = null;
			AuthInfo ai = null;
			HashMap<byte[], byte[]> ai_data = null;
			while (rs.next ())
			{
				if (token == null)
				{
					user = new User (rs.getLong ("user_id"), rs.getLong ("bmu_user_id"));
					user.setPwd (token_pwd);

					String str_backupdate = "";
					str_backupdate = cipher.decData (rs.getBytes ("backupdate"), user);
					Date backupdate = new Date (new Long (str_backupdate));

					token = new Token (user, backupdate, rs.getBoolean ("reusable"));
					token.setTokenpwd (token_pwd);
				}

				Service service = new Service (rs.getLong ("service_id"), rs.getLong ("bmu_service_id"));

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

			if (ai_data == null)
			{
				throw new RestTokenNotFoundException (token_id);
			}

			ai.setAi_data (ai_data);
			token.addAuthInfo (ai);

			rs.close ();

			ps_delete = Connection.getPreparedStatement (PS_DELETE_TOKEN_BY_TOKEN_ID);
			ps_delete.setLong (1, token_id);
			ps_delete.executeUpdate ();

		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}
		finally
		{
			Connection.closeQuiet (ps_select);
			Connection.closeQuiet (ps_delete);
		}

		token.setId (token_id);
		return token;
	}

}
