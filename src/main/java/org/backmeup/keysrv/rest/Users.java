package org.backmeup.keysrv.rest;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.backmeup.keysrv.rest.data.UserContainer;
import org.backmeup.keysrv.rest.exceptions.RestUserNotFoundException;
import org.backmeup.keysrv.rest.exceptions.RestUserNotValidException;
import org.backmeup.keysrv.rest.exceptions.RestWrongDecryptionKeyException;
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
	@Path ("{bmu_user_id}/{bmu_user_pwd}/register")
	@Produces ("application/json")
	public void registerUser (@PathParam ("bmu_user_id") long  bmu_user_id, @PathParam ("bmu_user_pwd") String  bmu_user_pwd)
	{
		DBManager dbm = new DBManager ();
		User user = new User (bmu_user_id);
		user.setPwd (bmu_user_pwd);
		
		dbm.insertUser (user);
	}
	
	@GET
	@Path ("{bmu_user_id}/{bmu_user_pwd}/validate")
	@Produces ("application/json")
	public void validateUser (@PathParam ("bmu_user_id") long  bmu_user_id, @PathParam ("bmu_user_pwd") String  bmu_user_pwd)
	{
		DBManager dbm = new DBManager ();
		User user = dbm.getUser (bmu_user_id);
		
		if (user.validatePwd (bmu_user_pwd) == false)
		{
			throw new RestUserNotValidException (bmu_user_id);
		}
	}
	
	@GET
	@Path ("{bmu_user_id}/{old_bmu_user_pwd}/{new_bmu_user_pwd}/changeuserpwd")
	@Produces ("application/json")
	public void changeUserPwd (@PathParam ("bmu_user_id") long bmu_user_id, @PathParam ("new_bmu_user_pwd") String new_bmu_user_pwd, @PathParam ("old_bmu_user_pwd") String old_bmu_user_pwd)
	{
		DBManager dbm = new DBManager ();
		User user = dbm.getUser (bmu_user_id);
		
		if (user.validatePwd (old_bmu_user_pwd) == false)
		{
			throw new RestUserNotValidException (bmu_user_id);
		}
		
		user.setPwd (new_bmu_user_pwd);
	}
}
