package org.backmeup.keyserver.dal;

import org.backmeup.keysrv.worker.User;

public interface UserDao {
	public void insertUser(User user);

	public User getUser(long bmu_user_id);

	public void changeUser(User user);

	public void deleteUser(User user);
}
