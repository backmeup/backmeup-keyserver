package org.backmeup.keysrv.dal.postgres.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.backmeup.keysrv.worker.FileLogger;

public class Connection
{
	private static DataSource datasource = null;

	private static void init ()
	{
		if (datasource == null)
		{
			try
			{
				InitialContext ctx = new InitialContext ();
				datasource = (DataSource) ctx.lookup ("java:comp/env/jdbc/keysrvdb");
			}
			catch (Exception e)
			{
				FileLogger.logException (e);
				e.printStackTrace ();
			}
		}
	}

	public static java.sql.Connection getInstance () throws SQLException
	{
		init ();
		return datasource.getConnection ();
	}
	
	public static void closeQuiet (PreparedStatement ps)
	{
		try
		{
			if ((ps != null) && (ps.isClosed () == false))
			{
				ps.close ();
			}
		}
		catch (Exception e)
		{
			// ignore
			FileLogger.logException (e);
		}
	}
	
	public static void closeQuiet (java.sql.Connection con)
	{
		try
		{
			if ((con != null) && (con.isClosed () == false))
			{
				con.close ();
			}
		}
		catch (Exception e)
		{
			// ignore
			FileLogger.logException (e);
		}
	}
	
	public static void closeQuiet (ResultSet rs)
	{
		try
		{
			if ((rs != null) && (rs.isClosed () == false))
			{
				rs.close ();
			}
		}
		catch (Exception e)
		{
			// ignore
			FileLogger.logException (e);
		}
	}
}
