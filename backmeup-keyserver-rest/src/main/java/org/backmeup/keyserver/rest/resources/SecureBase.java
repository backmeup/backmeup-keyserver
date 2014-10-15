package org.backmeup.keyserver.rest.resources;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Response.Status;

import org.backmeup.keyserver.model.AppUser;
import org.backmeup.keyserver.rest.auth.KeyserverAppPrincipal;

public class SecureBase extends Base {

    @Context
    private SecurityContext securityContext;

    protected void canOnlyWorkWithMyData(Long appId) {
        AppUser activeUser = ((KeyserverAppPrincipal) securityContext.getUserPrincipal()).getUser();
        if (!activeUser.getAppId().equals(appId)) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
    }

}
