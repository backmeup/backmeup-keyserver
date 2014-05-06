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
import org.backmeup.keysrv.rest.data.AuthInfoContainer;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.DBLogger;
import org.backmeup.keysrv.worker.DataManager;
import org.backmeup.keysrv.worker.Service;
import org.backmeup.keysrv.worker.User;

@Path("/authinfos")
public class AuthInfos {
	@GET
	@Path("{bmu_authinfo_id}/{bmu_user_id}/{bmu_service_id}/{user_pwd}")
	@Produces("application/json")
	public AuthInfoContainer getAuthInfo(
			@PathParam("bmu_authinfo_id") long bmuAuthinfoId,
			@PathParam("bmu_user_id") long bmuUserId,
			@PathParam("bmu_service_id") long bmuServiceId,
			@PathParam("user_pwd") String userPwd) {
		UserDao userdao = DataManager.getUserDao();
		ServiceDao servicedao = DataManager.getServiceDao();
		AuthInfoDao authinfodoa = DataManager.getAuthInfoDao();

		User user = null;
		Service service = null;

		user = userdao.getUser(bmuUserId);
		service = servicedao.getService(bmuServiceId);

		user.setPwd(userPwd);

		AuthInfo ai = authinfodoa.getAuthInfo(bmuAuthinfoId, user, service);

		DBLogger.logProvideAuthInfo(user, ai);

		return new AuthInfoContainer(ai);
	}

	@POST
	@Path("add")
	@Consumes("application/json")
	@Produces("application/json")
	public void addAuthInfo(AuthInfoContainer aic) {
		UserDao userdao = DataManager.getUserDao();
		ServiceDao servicedao = DataManager.getServiceDao();
		AuthInfoDao authinfodoa = DataManager.getAuthInfoDao();

		User user = userdao.getUser(aic.getBmu_user_id());
		user.setPwd(aic.getUser_pwd());

		Service service = servicedao.getService(aic.getBmu_service_id());

		AuthInfo ai = new AuthInfo(aic.getBmu_authinfo_id(), user, service);
		ai.setDecAiData(aic.getAi_data());

		authinfodoa.insertAuthInfo(ai);

		DBLogger.logAddAuthInfo(user, ai);
	}

	@DELETE
	@Path("{bmu_authinfo_id}")
	@Produces("application/json")
	public void deleteAuthInfo(
			@PathParam("bmu_authinfo_id") long bmuAuthinfoId) {
		AuthInfoDao authinfodoa = DataManager.getAuthInfoDao();

		AuthInfo ai = authinfodoa.getAuthInfo(bmuAuthinfoId);

		authinfodoa.deleteAuthInfo(bmuAuthinfoId);

		DBLogger.logDeleteAuthInfo(ai.getUser(), ai);
	}
}
