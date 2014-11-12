package org.backmeup.keyserver.rest.auth;

import java.security.Principal;
import org.backmeup.keyserver.model.App;

public class KeyserverAppPrincipal implements Principal {
	private String userId;
	private final App user;

	public KeyserverAppPrincipal(String userId, App user) {
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

	public App getUser() {
		return user;
	}

	@Override
	public String getName() {
		return userId;
	}
}
