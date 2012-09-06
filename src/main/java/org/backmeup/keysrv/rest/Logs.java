package org.backmeup.keysrv.rest;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.backmeup.keysrv.rest.data.LogContainer;
import org.backmeup.keysrv.rest.data.ServiceContainer;
import org.backmeup.keysrv.worker.DBManager;
import org.backmeup.keysrv.worker.Service;
import org.backmeup.keysrv.worker.User;

@Path ("/logs")
public class Logs
{
	@GET
	@Path ("{bmu_user_id}")
	@Produces ("application/json")
	public List<LogContainer> getLogs (@PathParam ("bmu_user_id") long bmu_user_id)
	{
		DBManager dbm = new DBManager ();
		
		User user = dbm.getUser (bmu_user_id);
		return dbm.getLogs (user);
	}
}
