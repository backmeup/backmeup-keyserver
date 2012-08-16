package org.backmeup.keysrv.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.backmeup.keysrv.rest.data.AuthInfoContainer;
import org.backmeup.keysrv.rest.exceptions.*;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.DBManager;
import org.backmeup.keysrv.worker.Service;
import org.backmeup.keysrv.worker.User;

@Path ("/authinfos")
public class AuthInfos
{
	@GET
	@Path ("{bmu_authinfo_id}/{bmu_user_id}/{bmu_service_id}/{user_pwd}")
	@Produces ("application/json")
	public AuthInfoContainer getAuthInfo (@PathParam ("bmu_authinfo_id") long bmu_authinfo_id, @PathParam ("bmu_user_id") long bmu_user_id, @PathParam ("bmu_service_id") long bmu_service_id, @PathParam ("user_pwd") String user_pwd) throws RestSQLException
	{
		DBManager dbm = new DBManager ();
		
		User user = null;
		Service service = null;

		user = dbm.getUser (bmu_user_id);
		service = dbm.getService (bmu_service_id);
		
		user.setPwd (user_pwd);
		
		AuthInfo ai = dbm.getAuthInfo (bmu_authinfo_id, user, service);
			
		return new AuthInfoContainer (ai);
	}
	
	@POST
	@Path ("add")
	@Consumes ("application/json")
	@Produces ("application/json")
	public void addAuthInfo (AuthInfoContainer aic) throws RestUserNotFoundException, RestSQLException
	{
		DBManager dbm = new DBManager ();
		
		User user = dbm.getUser (aic.getBmu_user_id ());
		user.setPwd (aic.getUser_pwd ());
		
		Service service = dbm.getService (aic.getBmu_service_id ());
		
		AuthInfo ai = new AuthInfo (aic.getBmu_authinfo_id (), user, service);
		ai.setDecAi_data (aic.getAi_data ());
		
		dbm.insertAuthInfo (ai);
	}
	
	@DELETE
	@Path ("{bmu_authinfo_id}")
	@Produces ("application/json")
	public void deleteAuthInfo (@PathParam ("bmu_authinfo_id") long bmu_authinfo_id) throws RestSQLException
	{
		DBManager dbm = new DBManager ();
		
		dbm.deleteAuthInfo (bmu_authinfo_id);
	}
}
