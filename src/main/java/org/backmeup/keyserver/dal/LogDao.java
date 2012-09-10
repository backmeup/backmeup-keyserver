package org.backmeup.keyserver.dal;

import java.util.List;

import org.backmeup.keysrv.rest.data.LogContainer;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.Token;
import org.backmeup.keysrv.worker.User;

public interface LogDao
{
	public void insertLog (User user, String message, String type);
	
	public void insertLog (User user, AuthInfo authinfo, String message, String type);
	
	public void insertLog (User user, Token token, String message, String type);
	
	public void insertLog (User user, AuthInfo authinfo, Token token, String message, String type);
	
	public List<LogContainer> getLogs (User user);
	
	public void deleteAllUserLogs (User user);
}
