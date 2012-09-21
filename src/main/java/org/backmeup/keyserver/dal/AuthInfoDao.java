package org.backmeup.keyserver.dal;

import java.util.ArrayList;

import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.Service;
import org.backmeup.keysrv.worker.User;

public interface AuthInfoDao
{
	public void insertAuthInfo (AuthInfo authinfo);
	
	/**
	 * Returns an complete authinfo object with data.
	 * 
	 * @param bmu_authinfo_id
	 * @param user
	 * @param service
	 * @return
	 */
	public AuthInfo getAuthInfo (long bmu_authinfo_id, User user, Service service);
	
	/**
	 * Returns an authinfo object without data.
	 * Only bmu_authinfo_id, bmu_service_id and bmu_user_id are set.
	 * 
	 * @param bmu_authinfo_id
	 * @return
	 */
	public AuthInfo getAuthInfo (long bmu_authinfo_id);
	
	public ArrayList<AuthInfo> getUserAuthInfos (User user);
	
	public boolean existAuthInfo (long bmu_authinfo_id);
	
	public void deleteAuthInfo (long bmu_authinfo_id);
}
