package org.backmeup.keyserver.rest.auth;

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;
import org.backmeup.keyserver.model.App;

public class KeyserverSecurityContext implements SecurityContext {
    private final KeyserverAppPrincipal principal;

    public KeyserverSecurityContext(App app) {
        this.principal = new KeyserverAppPrincipal(app);
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return principal.getApp().getAppRole().toString().equals(role);
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
