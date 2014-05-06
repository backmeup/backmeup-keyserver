package org.backmeup.keysrv.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.backmeup.keyserver.dal.LogDao;
import org.backmeup.keyserver.dal.UserDao;
import org.backmeup.keysrv.rest.data.LogContainer;
import org.backmeup.keysrv.worker.DataManager;
import org.backmeup.keysrv.worker.User;

@Path("/logs")
public class Logs {
	@GET
	@Path("{bmu_user_id}")
	@Produces("application/json")
	public List<LogContainer> getLogs(@PathParam("bmu_user_id") long bmuUserId) {
		LogDao logdao = DataManager.getLogDao();
		UserDao userdao = DataManager.getUserDao();

		User user = userdao.getUser(bmuUserId);
		return logdao.getLogs(user);
	}
}
