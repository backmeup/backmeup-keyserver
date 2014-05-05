package org.backmeup.keysrv.worker;

import org.backmeup.keyserver.dal.AuthInfoDao;
import org.backmeup.keyserver.dal.LogDao;
import org.backmeup.keyserver.dal.ServiceDao;
import org.backmeup.keyserver.dal.TokenDao;
import org.backmeup.keyserver.dal.UserDao;
import org.backmeup.keysrv.dal.postgres.impl.AuthInfoDaoImpl;
import org.backmeup.keyserver.dal.rabbitmq.impl.LogDaoImpl;
import org.backmeup.keysrv.dal.postgres.impl.ServiceDaoImpl;
import org.backmeup.keysrv.dal.postgres.impl.TokenDaoImpl;
import org.backmeup.keysrv.dal.postgres.impl.UserDaoImpl;

public class DataManager {
	public static UserDao getUserDao() {
		return new UserDaoImpl();
	}

	public static ServiceDao getServiceDao() {
		return new ServiceDaoImpl();
	}

	public static AuthInfoDao getAuthInfoDao() {
		return new AuthInfoDaoImpl();
	}

	public static TokenDao getTokenDao() {
		return new TokenDaoImpl();
	}

	public static LogDao getLogDao() {
		return new LogDaoImpl();
	}
}
