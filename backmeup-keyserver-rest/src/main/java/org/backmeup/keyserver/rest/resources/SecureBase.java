package org.backmeup.keyserver.rest.resources;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
//import javax.ws.rs.core.Response.Status;
//import javax.ws.rs.WebApplicationException;


import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.rest.auth.KeyserverPrincipal;

public class SecureBase extends Base {

    @Context
    private SecurityContext securityContext;

//    protected void canOnlyWorkWithMyData(String appId) {
//        App activeUser = ((KeyserverAppPrincipal) securityContext.getUserPrincipal()).getApp();
//        if (!activeUser.getAppId().equals(appId)) {
//            throw new WebApplicationException(Status.FORBIDDEN);
//        }
//    }
    
    protected SecurityContext getSecurityContext() {
        return this.securityContext;
    }
    
    protected App getApp() {
        return ((KeyserverPrincipal) this.getSecurityContext().getUserPrincipal()).getApp();
    }
    
    protected boolean hasTokenAuth() {
        return ((KeyserverPrincipal) this.getSecurityContext().getUserPrincipal()).hasTokenAuth();
    }
    
    protected AuthResponse getAuthResponse() {
        return ((KeyserverPrincipal) this.getSecurityContext().getUserPrincipal()).getAuthResponse();
    }

}
