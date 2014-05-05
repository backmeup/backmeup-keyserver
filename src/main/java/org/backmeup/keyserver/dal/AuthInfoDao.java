package org.backmeup.keyserver.dal;

import java.util.List;

import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.Service;
import org.backmeup.keysrv.worker.User;

public interface AuthInfoDao {
	void insertAuthInfo(AuthInfo authinfo);

	/**
	 * Returns an complete authinfo object with data.
	 * 
	 * @param bmu_authinfo_id
	 * @param user
	 * @param service
	 * @return
	 */
	AuthInfo getAuthInfo(long bmuAuthinfoId, User user, Service service);

	/**
	 * Returns an authinfo object without data. Only bmu_authinfo_id,
	 * bmu_service_id and bmu_user_id are set.
	 * 
	 * @param bmu_authinfo_id
	 * @return
	 */
	AuthInfo getAuthInfo(long bmuAuthinfoId);

	List<AuthInfo> getUserAuthInfos(User user);

	boolean existAuthInfo(long bmuAuthinfoId);

	void deleteAuthInfo(long bmuAuthinfoId);
}
