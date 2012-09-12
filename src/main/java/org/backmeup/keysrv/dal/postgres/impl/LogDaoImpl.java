package org.backmeup.keysrv.dal.postgres.impl;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.backmeup.keyserver.dal.LogDao;
import org.backmeup.keysrv.rest.data.LogContainer;
import org.backmeup.keysrv.rest.exceptions.RestSQLException;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.FileLogger;
import org.backmeup.keysrv.worker.PGPKeys;
import org.backmeup.keysrv.worker.Token;
import org.backmeup.keysrv.worker.User;

public class LogDaoImpl implements LogDao
{
	private static final String PS_INSERT_LOG = "INSERT INTO logs (bmu_user_id, bmu_service_id, bmu_authinfo_id, bmu_token_id, date, type, description) VALUES (?, ?, ?, ?, ?, (pgp_pub_encrypt (?, dearmor(?))), (pgp_pub_encrypt (?, dearmor(?))))";
	private static final String PS_DELETE_LOG_BY_BMU_USER_ID = "DELETE FROM logs WHERE bmu_user_id=?";
	private static final String PS_SELECT_LOG_BY_BMU_USER_ID = "SELECT bmu_user_id, bmu_service_id, bmu_authinfo_id, bmu_token_id, date, pgp_pub_decrypt (type, dearmor (?)) AS type, pgp_pub_decrypt (description, dearmor (?)) AS description FROM logs WHERE bmu_user_id=?";
	
	private PGPKeys pgpkeys;

	public LogDaoImpl ()
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
	public void insertLog (User user, String message, String type)
	{
		insertLog (user, null, null, message, type);
	}

	@Override
	public void insertLog (User user, AuthInfo authinfo, String message, String type)
	{
		insertLog (user, authinfo, null, message, type);
	}

	@Override
	public void insertLog (User user, Token token, String message, String type)
	{
		insertLog (user, null, token, message, type);
	}

	@Override
	public void insertLog (User user, AuthInfo authinfo, Token token, String message, String type)
	{
		PreparedStatement ps = null;
		
		try
		{
			ps = Connection.getPreparedStatement (PS_INSERT_LOG);
			ps.setLong (1, user.getBmuId ());

			if (authinfo != null)
			{
				ps.setLong (2, authinfo.getService ().getBmuId ());
				ps.setLong (3, authinfo.getBmuAuthinfoId ());
			}
			else
			{
				ps.setNull (2, java.sql.Types.INTEGER);
				ps.setNull (3, java.sql.Types.INTEGER);
			}
			if (token != null)
			{
				ps.setLong (4, token.getId ());
			}
			else
			{
				ps.setNull (4, java.sql.Types.INTEGER);
			}
			ps.setLong (5, new Date ().getTime ());
			ps.setString (6, type);
			ps.setString (7, pgpkeys.getPublickey ());
			ps.setString (8, message);
			ps.setString (9, pgpkeys.getPublickey ());

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
	public List<LogContainer> getLogs (User user)
	{
		PreparedStatement ps = null;
		
		try
		{
			ps = Connection.getPreparedStatement (PS_SELECT_LOG_BY_BMU_USER_ID);
			
			ps.setString (1, pgpkeys.getPrivatekey ());
			ps.setString (2, pgpkeys.getPrivatekey ());
			ps.setLong (3, user.getBmuId ());

			List<LogContainer> containers = new LinkedList<LogContainer> ();
			ResultSet rs = ps.executeQuery ();
			while (rs.next ())
			{
				LogContainer container = new LogContainer ();
				container.setBmu_user_id (user.getBmuId ());
				container.setBmu_service_id (rs.getLong ("bmu_service_id"));
				container.setBmu_authinfo_id (rs.getLong ("bmu_authinfo_id"));
				container.setBmu_token_id (rs.getLong ("bmu_token_id"));
				container.setDate (rs.getLong ("date"));
				container.setType (rs.getString ("type"));
				container.setMessage (rs.getString ("description"));

				containers.add (container);
			}
			rs.close ();

			return containers;
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
	public void deleteAllUserLogs (User user)
	{
		PreparedStatement ps = null;
		
		try
		{
			ps = Connection.getPreparedStatement (PS_DELETE_LOG_BY_BMU_USER_ID);
			
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
