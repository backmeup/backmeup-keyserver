package org.backmeup.keyserver.dal;

import java.util.List;

import org.backmeup.keysrv.rest.data.LogContainer;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.Token;
import org.backmeup.keysrv.worker.User;

public interface LogDao {
	void insertLog(User user, String message, String type);

	void insertLog(User user, AuthInfo authinfo, String message,
			String type);

	void insertLog(User user, Token token, String message, String type);

	void insertLog(User user, AuthInfo authinfo, Token token,
			String message, String type);

	/**
	 * @deprecated
	 */
	@Deprecated
	List<LogContainer> getLogs(User user);

	/**
	 * @deprecated
	 */
	@Deprecated
	void deleteAllUserLogs(User user);
}
