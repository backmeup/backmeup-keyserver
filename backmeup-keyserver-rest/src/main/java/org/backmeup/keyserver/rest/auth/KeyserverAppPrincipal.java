package org.backmeup.keyserver.rest.auth;

import java.security.Principal;
import org.backmeup.keyserver.model.AppUser;

public class KeyserverAppPrincipal implements Principal {
	private String userId;
	private final AppUser user;

	public KeyserverAppPrincipal(String userId, AppUser user) {
		super();
		this.userId = userId;
		this.user = user;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public AppUser getUser() {
		return user;
	}

	@Override
	public String getName() {
		return userId;
	}
}
