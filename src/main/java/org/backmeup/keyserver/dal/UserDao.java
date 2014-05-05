package org.backmeup.keyserver.dal;

import org.backmeup.keysrv.worker.User;

public interface UserDao {
	void insertUser(User user);

	User getUser(long bmuUserId);

	void changeUser(User user);

	void deleteUser(User user);
}
