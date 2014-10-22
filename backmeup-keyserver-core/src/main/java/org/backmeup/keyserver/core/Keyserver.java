package org.backmeup.keyserver.core;

import org.backmeup.keyserver.model.AppUser;
import org.backmeup.keyserver.model.AuthResponse;

public interface Keyserver {
	
	public String registerUser(String username, String password)
			throws KeyserverException;

	public String registerAnonoumysUser(String username, String password)
			throws KeyserverException;
	
	public AuthResponse authenticateUserWithPassword(String username, String password) 
			throws KeyserverException;

	public AppUser registerApp(AppUser.Approle role) throws KeyserverException;

	public void removeApp(String appId) throws KeyserverException;

	public AppUser authenticateApp(String appId, String appKey)
			throws KeyserverException;

}