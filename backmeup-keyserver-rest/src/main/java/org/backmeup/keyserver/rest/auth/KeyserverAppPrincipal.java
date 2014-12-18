package org.backmeup.keyserver.rest.auth;

import java.security.Principal;
import org.backmeup.keyserver.model.App;

public class KeyserverAppPrincipal implements Principal {
    private final App app;

    public KeyserverAppPrincipal(App app) {
        this.app = app;
    }

    public App getApp() {
        return app;
    }

    @Override
    public String getName() {
        return this.app.getAppId();
    }
}
