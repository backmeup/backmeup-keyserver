package org.backmeup.keysrv.dal.postgres.impl;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.backmeup.keyserver.dal.UserDao;
import org.backmeup.keysrv.rest.exceptions.RestSQLException;
import org.backmeup.keysrv.rest.exceptions.RestUserAlreadyExistException;
import org.backmeup.keysrv.rest.exceptions.RestUserNotFoundException;
import org.backmeup.keysrv.worker.FileLogger;
import org.backmeup.keysrv.worker.PGPKeys;
import org.backmeup.keysrv.worker.User;

public class UserDaoImpl implements UserDao
{
	private final String PS_INSERT_USER = "INSERT INTO users (bmu_user_id, bmu_user_pwd_hash) VALUES (?, (pgp_pub_encrypt (?, dearmor(?))))";
	private final String PS_UPDATE_USER = "UPDATE users SET bmu_user_pwd_hash=(pgp_pub_encrypt (?, dearmor(?))) WHERE bmu_user_id=?";
	private final String PS_SELECT_USER_BY_BMU_USER_ID = "SELECT id, bmu_user_id, pgp_pub_decrypt (bmu_user_pwd_hash, dearmor (?)) AS bmu_user_pwd_hash FROM users WHERE bmu_user_id=?";
	private final String PS_DELETE_USER_BY_BMU_USER_ID = "DELETE FROM users WHERE bmu_user_id=?";

	private PGPKeys pgpkeys;

	public UserDaoImpl ()
	{
		try
		{
			pgpkeys = new PGPKeys ();
		}
		catch (IOException e)
		{
			// should not come up
			FileLogger.logException (e);
			e.printStackTrace ();
		}
	}

	@Override
	public void insertUser (User user)
	{
		try
		{
			this.getUser (user.getBmuId ());
			throw new RestUserAlreadyExistException (user.getBmuId ());
		}
		catch (RestUserNotFoundException e)
		{
		}

		PreparedStatement ps = null;

		try
		{
			ps = Connection.getPreparedStatement (PS_INSERT_USER);

			ps.setLong (1, user.getBmuId ());
			ps.setString (2, user.getPwd_hash ());
			ps.setString (3, pgpkeys.getPublickey ());

			ps.executeUpdate ();
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}
		finally
		{
			Connection.closeQuiet (ps);
		}
	}

	@Override
	public User getUser (long bmu_user_id)
	{
		User user = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			ps = Connection.getPreparedStatement (PS_SELECT_USER_BY_BMU_USER_ID);

			ps.setString (1, pgpkeys.getPrivatekey ());
			ps.setLong (2, bmu_user_id);

			rs = ps.executeQuery ();
			if (rs.next ())
			{
				user = new User (rs.getLong ("id"), rs.getLong ("bmu_user_id"));
				user.setPwd_hash (rs.getString ("bmu_user_pwd_hash"));
			}
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}
		finally
		{
			Connection.closeQuiet (rs);
			Connection.closeQuiet (ps);
		}

		if (user == null)
		{
			throw new RestUserNotFoundException (bmu_user_id);
		}

		return user;
	}

	@Override
	public void changeUser (User user)
	{
		PreparedStatement ps = null;

		try
		{
			ps = Connection.getPreparedStatement (PS_UPDATE_USER);

			ps.setString (1, user.getPwd_hash ());
			ps.setString (2, pgpkeys.getPublickey ());
			ps.setLong (3, user.getBmuId ());

			ps.executeUpdate ();
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
		}
		finally
		{
			org.backmeup.keysrv.dal.postgres.impl.Connection.closeQuiet (ps);
		}
	}

	@Override
	public void deleteUser (User user)
	{
		this.getUser (user.getBmuId ());

		PreparedStatement ps = null;

		try
		{
			ps = Connection.getPreparedStatement (PS_DELETE_USER_BY_BMU_USER_ID);

			ps.setLong (1, user.getBmuId ());
			ps.executeUpdate ();
		}
		catch (SQLException e)
		{
			FileLogger.logException (e);
			throw new RestSQLException (e);
		}
		finally
		{
			Connection.closeQuiet (ps);
		}
	}

}
