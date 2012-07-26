package org.backmeup.keysrv.rest;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.backmeup.keysrv.rest.data.UserContainer;
import org.backmeup.keysrv.worker.DBManager;
import org.backmeup.keysrv.worker.User;

@Path ("/users")
public class Users
{
	@GET
	@Path ("{bmu_user_id}")
	@Produces ("application/json")
	public UserContainer getUser (@PathParam ("bmu_user_id") long bmu_user_id)
	{
		DBManager dbm = new DBManager ();
		User user = dbm.getUser (bmu_user_id);
		
		return new UserContainer (user);
	}

	@DELETE
	@Path ("{bmu_user_id}")
	@Produces ("application/json")
	public void deleteUser (@PathParam ("bmu_user_id") long bmu_user_id)
	{
		DBManager dbm = new DBManager ();
		User user = new User (bmu_user_id);
		
		dbm.deleteUser (user);
	}

	@POST
	@Path ("{bmu_user_id}/register")
	@Produces ("application/json")
	public void registerUser (@PathParam ("bmu_user_id") long  bmu_user_id)
	{
		DBManager dbm = new DBManager ();
		User user = new User (bmu_user_id);
		
		dbm.insertUser (user);
	}
}
