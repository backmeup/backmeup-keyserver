package org.backmeup.keysrv.rest;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.backmeup.keyserver.dal.AuthInfoDao;
import org.backmeup.keyserver.dal.UserDao;
import org.backmeup.keysrv.rest.data.UserContainer;
import org.backmeup.keysrv.rest.exceptions.RestUserNotValidException;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.DBLogger;
import org.backmeup.keysrv.worker.DataManager;
import org.backmeup.keysrv.worker.User;

@Path("/users")
public class Users {
	@GET
	@Path("{bmu_user_id}")
	@Produces("application/json")
	public UserContainer getUser(@PathParam("bmu_user_id") long bmuUserId) {
		UserDao userdao = DataManager.getUserDao();
		User user = userdao.getUser(bmuUserId);

		return new UserContainer(user);
	}

	@DELETE
	@Path("{bmu_user_id}")
	@Produces("application/json")
	public void deleteUser(@PathParam("bmu_user_id") long bmuUserId) {
		UserDao userdao = DataManager.getUserDao();

		User user = new User(bmuUserId);

		userdao.deleteUser(user);
		DBLogger.deleteAllUserLogs(user);
	}

	@POST
	@Path("{bmu_user_id}/{bmu_user_pwd}/register")
	@Produces("application/json")
	public void registerUser(@PathParam("bmu_user_id") long bmuUserId,
			@PathParam("bmu_user_pwd") String bmuUserPwd) {
		UserDao userdao = DataManager.getUserDao();

		User user = new User(bmuUserId);
		user.setPwd(bmuUserPwd);

		userdao.insertUser(user);

		DBLogger.logUserCreated(user);
	}

	@GET
	@Path("{bmu_user_id}/{bmu_user_pwd}/validate")
	@Produces("application/json")
	public void validateUser(@PathParam("bmu_user_id") long bmuUserId,
			@PathParam("bmu_user_pwd") String bmuUserPwd) {
		UserDao userdao = DataManager.getUserDao();
		User user = userdao.getUser(bmuUserId);

		if (!user.validatePwd(bmuUserPwd)) {
			DBLogger.logUserWrongLoginPassword(user);
			throw new RestUserNotValidException(bmuUserId);
		}

		DBLogger.logUserLogin(user);
	}

	@GET
	@Path("{bmu_user_id}/{old_bmu_user_pwd}/{new_bmu_user_pwd}/changeuserpwd")
	@Produces("application/json")
	public void changeUserPwd(@PathParam("bmu_user_id") long bmuUserId,
			@PathParam("new_bmu_user_pwd") String newBmuUserPwd,
			@PathParam("old_bmu_user_pwd") String oldBmuUserPwd) {
		UserDao userdao = DataManager.getUserDao();
		User user = userdao.getUser(bmuUserId);

		if (!user.validatePwd(oldBmuUserPwd)) {
			throw new RestUserNotValidException(bmuUserId);
		}

		user.setPwd(newBmuUserPwd);
		userdao.changeUser(user);

		DBLogger.logUserChangedPassword(user);
	}

	@GET
	@Path("{bmu_user_id}/{old_bmu_user_keyring_pwd}/{new_bmu_user_keyring_pwd}/changeuserkeyringpwd")
	@Produces("application/json")
	public void changeUserKeyringPwd(
			@PathParam("bmu_user_id") long bmuUserId,
			@PathParam("new_bmu_user_keyring_pwd") String newBmuUserKeyringPwd,
			@PathParam("old_bmu_user_keyring_pwd") String oldBmuUserKeyringPwd) {
		UserDao userdao = DataManager.getUserDao();
		AuthInfoDao authinfodao = DataManager.getAuthInfoDao();
		User user = userdao.getUser(bmuUserId);

		List<AuthInfo> authinfos = authinfodao.getUserAuthInfos(user);

		for (AuthInfo authinfo : authinfos) {
			authinfo.changePassword(oldBmuUserKeyringPwd,
					newBmuUserKeyringPwd);
			authinfodao.deleteAuthInfo(authinfo.getBmuAuthinfoId());
			authinfodao.insertAuthInfo(authinfo);
		}

		DBLogger.logUserChangedKeyringPassword(user);
	}
}
