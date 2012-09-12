package org.backmeup.keysrv.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.backmeup.keyserver.dal.AuthInfoDao;
import org.backmeup.keyserver.dal.ServiceDao;
import org.backmeup.keyserver.dal.UserDao;
import org.backmeup.keysrv.dal.postgres.impl.AuthInfoDaoImpl;
import org.backmeup.keysrv.dal.postgres.impl.ServiceDaoImpl;
import org.backmeup.keysrv.dal.postgres.impl.UserDaoImpl;
import org.backmeup.keysrv.rest.data.AuthInfoContainer;
import org.backmeup.keysrv.rest.exceptions.*;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.DBLogger;
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
		UserDao userdao = new UserDaoImpl ();
		ServiceDao servicedao = new ServiceDaoImpl ();
		AuthInfoDao authinfodoa = new AuthInfoDaoImpl ();
		
		User user = null;
		Service service = null;

		user = userdao.getUser (bmu_user_id);
		service = servicedao.getService (bmu_service_id);
		
		user.setPwd (user_pwd);
		
		AuthInfo ai = authinfodoa.getAuthInfo (bmu_authinfo_id, user, service);
		
		DBLogger.logProvideAuthInfo (user, ai);
		
		return new AuthInfoContainer (ai);
	}
	
	@POST
	@Path ("add")
	@Consumes ("application/json")
	@Produces ("application/json")
	public void addAuthInfo (AuthInfoContainer aic) throws RestUserNotFoundException, RestSQLException
	{
		UserDao userdao = new UserDaoImpl ();
		ServiceDao servicedao = new ServiceDaoImpl ();
		AuthInfoDao authinfodoa = new AuthInfoDaoImpl ();
		
		User user = userdao.getUser (aic.getBmu_user_id ());
		user.setPwd (aic.getUser_pwd ());
		
		Service service = servicedao.getService (aic.getBmu_service_id ());
		
		AuthInfo ai = new AuthInfo (aic.getBmu_authinfo_id (), user, service);
		ai.setDecAi_data (aic.getAi_data ());
		
		authinfodoa.insertAuthInfo (ai);
		
		DBLogger.logAddAuthInfo (user, ai);
	}
	
	@DELETE
	@Path ("{bmu_authinfo_id}")
	@Produces ("application/json")
	public void deleteAuthInfo (@PathParam ("bmu_authinfo_id") long bmu_authinfo_id) throws RestSQLException
	{
		AuthInfoDao authinfodoa = new AuthInfoDaoImpl ();
		
		authinfodoa.deleteAuthInfo (bmu_authinfo_id);
	}
}
