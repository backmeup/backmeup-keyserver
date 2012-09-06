package org.backmeup.keysrv.worker;

public class DBLogger
{
	private static final String CREATE_USER_MSG = "User created";
	private static final String CHANGE_USER_PWD_MSG = "User password changed";
	private static final String USER_LOGIN_MSG = "Successfully login attempt";
	private static final String WRONG_USER_LOGIN_PWD_MSG = "Login attempt with wrong password";
	private static final String WRONG_USER_KEYRING_PWD_MSG = "Wrong Keyring Password provided";
	private static final String ADD_AUTHINFO_MSG = "Authinfo added";
	private static final String GET_AUTHINFO_MSG = "Authinfo provided to core";
	private static final String DELETE_AUTHINFO_MSG = "Authinfo deleted";
	private static final String CHANGE_AUTHINFO_MSG = "Authinfo changed";
	private static final String CREATED_TOKEN_MSG = "Token created";
	private static final String USED_TOKEN_MESSAGE = "Token used";
	
	private static final String TYPE_INFO = "info";
	private static final String TYPE_WARNING = "warning";
	private static final String TYPE_ERROR = "error";
	
	public static void logUserCreated (User user)
	{
		DBManager dbm = new DBManager ();
		dbm.insertLog (user, CREATE_USER_MSG, TYPE_INFO);
		dbm = null;
	}
	
	public static void logUserChangedPassword (User user)
	{
		DBManager dbm = new DBManager ();
		dbm.insertLog (user, CHANGE_USER_PWD_MSG, TYPE_INFO);
		dbm = null;
	}
	
	public static void logUserLogin (User user)
	{
		DBManager dbm = new DBManager ();
		dbm.insertLog (user, USER_LOGIN_MSG, TYPE_INFO);
		dbm = null;
	}
	
	public static void logUserWrongLoginPassword (User user)
	{
		DBManager dbm = new DBManager ();
		dbm.insertLog (user, WRONG_USER_LOGIN_PWD_MSG, TYPE_WARNING);
		dbm = null;
	}
	
	public static void logUserWrongKeyringPassword (User user)
	{
		DBManager dbm = new DBManager ();
		dbm.insertLog (user, WRONG_USER_KEYRING_PWD_MSG, TYPE_WARNING);
		dbm = null;
	}
	
	public static void logAddAuthInfo (User user, AuthInfo authinfo)
	{
		DBManager dbm = new DBManager ();
		dbm.insertLog (user, authinfo, ADD_AUTHINFO_MSG, TYPE_INFO);
		dbm = null;
	}
	
	public static void logChangeAuthInfo (User user, AuthInfo authinfo)
	{
		DBManager dbm = new DBManager ();
		dbm.insertLog (user, authinfo, CHANGE_AUTHINFO_MSG, TYPE_INFO);
		dbm = null;
	}
	
	public static void logProvideAuthInfo (User user, AuthInfo authinfo)
	{
		DBManager dbm = new DBManager ();
		dbm.insertLog (user, authinfo, GET_AUTHINFO_MSG, TYPE_INFO);
		dbm = null;
	}
	
	public static void logDeleteAuthInfo (User user, AuthInfo authinfo)
	{
		DBManager dbm = new DBManager ();
		dbm.insertLog (user, authinfo, DELETE_AUTHINFO_MSG, TYPE_INFO);
		dbm = null;
	}
	
	public static void logCreateToken (User user, Token token)
	{
		DBManager dbm = new DBManager ();
		dbm.insertLog (user, token, CREATED_TOKEN_MSG, TYPE_INFO);
		dbm = null;
	}
	
	public static void logUseToken (User user, Token token)
	{
		DBManager dbm = new DBManager ();
		dbm.insertLog (user, token, USED_TOKEN_MESSAGE, TYPE_INFO);
		dbm = null;
	}
	
	public static void deleteAllUserLogs (User user)
	{
		DBManager dbm = new DBManager ();
		dbm.deleteAllUserLogs (user);
		dbm = null;
	}
}
