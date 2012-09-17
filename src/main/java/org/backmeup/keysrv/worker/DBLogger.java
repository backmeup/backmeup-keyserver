package org.backmeup.keysrv.worker;

import org.backmeup.keyserver.dal.LogDao;
import org.backmeup.keysrv.dal.postgres.impl.LogDaoImpl;

public class DBLogger
{
	private static final String CREATE_USER_MSG = "User created";
	private static final String CHANGE_USER_PWD_MSG = "User password changed";
	private static final String USER_LOGIN_MSG = "Successfully login attempt";
	private static final String WRONG_USER_LOGIN_PWD_MSG = "Login attempt with wrong password";
	private static final String WRONG_USER_KEYRING_PWD_MSG = "Wrong Keyring Password provided";
	private static final String CHANGE_USER_KEYRING_PWD_MSG = "User keyring password changed";
	private static final String ADD_AUTHINFO_MSG = "Authinfo added";
	private static final String GET_AUTHINFO_MSG = "Authinfo provided to core";
	private static final String DELETE_AUTHINFO_MSG = "Authinfo deleted";
	private static final String CHANGE_AUTHINFO_MSG = "Authinfo changed";
	private static final String CREATED_TOKEN_MSG = "Token created";
	private static final String USED_TOKEN_MESSAGE = "Token used";
	
	private static final String TYPE_INFO = "info";
	private static final String TYPE_WARNING = "warning";
	private static final String TYPE_ERROR = "error";
	
	private static LogDao logdao = null;
	
	private static void init ()
	{
		if (logdao == null)
		{
			logdao = DataManager.getLogDao ();
		}
	}
	
	public static void logUserCreated (User user)
	{
		init ();
		logdao.insertLog (user, CREATE_USER_MSG, TYPE_INFO);
	}
	
	public static void logUserChangedPassword (User user)
	{
		init ();
		logdao.insertLog (user, CHANGE_USER_PWD_MSG, TYPE_INFO);
	}
	
	public static void logUserLogin (User user)
	{
		init ();
		logdao.insertLog (user, USER_LOGIN_MSG, TYPE_INFO);
	}
	
	public static void logUserWrongLoginPassword (User user)
	{
		init ();
		logdao.insertLog (user, WRONG_USER_LOGIN_PWD_MSG, TYPE_WARNING);
	}
	
	public static void logUserWrongKeyringPassword (User user)
	{
		init ();
		logdao.insertLog (user, WRONG_USER_KEYRING_PWD_MSG, TYPE_WARNING);
	}
	
	public static void logUserChangedKeyringPassword (User user)
	{
		init ();
		logdao.insertLog (user, CHANGE_USER_KEYRING_PWD_MSG, TYPE_INFO);
	}
	
	public static void logAddAuthInfo (User user, AuthInfo authinfo)
	{
		init ();
		logdao.insertLog (user, authinfo, ADD_AUTHINFO_MSG, TYPE_INFO);
	}
	
	public static void logChangeAuthInfo (User user, AuthInfo authinfo)
	{
		init ();
		logdao.insertLog (user, authinfo, CHANGE_AUTHINFO_MSG, TYPE_INFO);
	}
	
	public static void logProvideAuthInfo (User user, AuthInfo authinfo)
	{
		init ();
		logdao.insertLog (user, authinfo, GET_AUTHINFO_MSG, TYPE_INFO);
	}
	
	public static void logDeleteAuthInfo (User user, AuthInfo authinfo)
	{
		init ();
		logdao.insertLog (user, authinfo, DELETE_AUTHINFO_MSG, TYPE_INFO);
	}
	
	public static void logCreateToken (User user, Token token)
	{
		init ();
		logdao.insertLog (user, token, CREATED_TOKEN_MSG, TYPE_INFO);
	}
	
	public static void logUseToken (User user, Token token)
	{
		init ();
		logdao.insertLog (user, token, USED_TOKEN_MESSAGE, TYPE_INFO);
	}
	
	public static void deleteAllUserLogs (User user)
	{
		init ();
		logdao.deleteAllUserLogs (user);
	}
}
