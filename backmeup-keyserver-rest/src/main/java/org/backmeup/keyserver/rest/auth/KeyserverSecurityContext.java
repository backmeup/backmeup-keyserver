package org.backmeup.keyserver.rest.auth;

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;
import org.backmeup.keyserver.model.AppUser;

public class KeyserverSecurityContext implements SecurityContext{
	private final KeyserverAppPrincipal user;
	
	public KeyserverSecurityContext(AppUser user) {
		this.user = new KeyserverAppPrincipal(user.getAppId(), user);
	}

	@Override
	public Principal getUserPrincipal() {
		return user;
	}

	@Override
	public boolean isUserInRole(String role) {
		return true;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public String getAuthenticationScheme() {
		return null;
	}

}
