package org.backmeup.keyserver.dal;

import java.util.ArrayList;

import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.Service;
import org.backmeup.keysrv.worker.User;

public interface AuthInfoDao
{
	public void insertAuthInfo (AuthInfo authinfo);
	
	public AuthInfo getAuthInfo (long bmu_authinfo_id, User user, Service service);
	
	public ArrayList<AuthInfo> getUserAuthInfos (User user);
	
	public boolean existAuthInfo (long bmu_authinfo_id);
	
	public void deleteAuthInfo (long bmu_authinfo_id);
}
