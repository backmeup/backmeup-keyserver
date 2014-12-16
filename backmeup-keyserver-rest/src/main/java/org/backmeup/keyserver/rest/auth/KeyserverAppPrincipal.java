package org.backmeup.keyserver.rest.auth;

import java.security.Principal;
import org.backmeup.keyserver.model.App;

public class KeyserverAppPrincipal implements Principal {
	private String appId;
	private final App app;

	public KeyserverAppPrincipal(String appId, App app) {
		super();
		this.appId = appId;
		this.app = app;
	}

	public String getUserId() {
		return appId;
	}

	public void setUserId(String userId) {
		this.appId = userId;
	}

	public App getApp() {
		return app;
	}

	@Override
	public String getName() {
		return appId;
	}
}
