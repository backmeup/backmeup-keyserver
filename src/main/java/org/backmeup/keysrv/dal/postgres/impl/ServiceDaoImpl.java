package org.backmeup.keysrv.dal.postgres.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.backmeup.keyserver.dal.ServiceDao;
import org.backmeup.keysrv.rest.exceptions.RestSQLException;
import org.backmeup.keysrv.rest.exceptions.RestServiceAlreadyExistException;
import org.backmeup.keysrv.rest.exceptions.RestServiceNotFoundException;
import org.backmeup.keysrv.worker.FileLogger;
import org.backmeup.keysrv.worker.PGPKeys;
import org.backmeup.keysrv.worker.Service;

public class ServiceDaoImpl implements ServiceDao
{
	private static final String PS_INSERT_SERVICE = "INSERT INTO services (bmu_service_id) VALUES (?)";
	private static final String PS_SELECT_SERVICE_BY_BMU_SERVICE_ID = "SELECT id, bmu_service_id FROM services WHERE bmu_service_id=?";
	private static final String PS_DELETE_SERVICE_BY_BMU_SERVICE_ID = "DELETE FROM services WHERE bmu_service_id=?";
	
	public ServiceDaoImpl ()
	{
	}

	@Override
	public void insertService (Service service)
	{
		try
		{
			this.getService (service.getBmuId ());
			throw new RestServiceAlreadyExistException (service.getBmuId ());
		}
		catch (RestServiceNotFoundException e)
		{
		}
		
		PreparedStatement ps = null;
		
		try
		{
			ps = Connection.getPreparedStatement (PS_INSERT_SERVICE);
			
			ps.setLong (1, service.getBmuId ());

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
	public Service getService (long bmu_service_id)
	{
		Service service = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try
		{
			ps = Connection.getPreparedStatement (PS_SELECT_SERVICE_BY_BMU_SERVICE_ID);
			
			ps.setLong (1, bmu_service_id);
			
			rs = ps.executeQuery ();
			if (rs.next ())
			{
				service = new Service (rs.getLong ("id"), rs.getLong ("bmu_service_id"));
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

		if (service == null)
		{
			throw new RestServiceNotFoundException (bmu_service_id);
		}

		return service;
	}

	@Override
	public void deleteService (Service service)
	{
		this.getService (service.getBmuId ());
		
		PreparedStatement ps = null;

		try
		{
			ps = Connection.getPreparedStatement (PS_DELETE_SERVICE_BY_BMU_SERVICE_ID);
			
			ps.setLong (1, service.getBmuId ());

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
