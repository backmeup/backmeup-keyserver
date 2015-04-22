package org.backmeup.keyserver.client;

import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.backmeup.keyserver.model.App.Approle;
import org.backmeup.keyserver.model.EntryNotFoundException;
import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.keyserver.model.dto.AppDTO;

public class KeyserverClient {
    protected static final GenericType<List<AppDTO>> APPDTO_LIST_TYPE = new GenericType<List<AppDTO>>() {
    };
    
    @SuppressWarnings("PMD.SingularField")
    private Client client;
    private WebTarget apps;
    private WebTarget theApp;
    private String appId;
    private String authorizationHeader;

    public KeyserverClient(String baseUrl, String appId, String appSecret) {
        this.client = ClientBuilder.newClient();
        UriBuilder base = UriBuilder.fromUri(baseUrl);
        this.apps = this.client.target(base.path("/applications/"));
        this.theApp = this.apps.path("/{appId}");
        this.appId = appId;
        this.authorizationHeader = appId + ";" + appSecret;
    }

    public String getAppId() {
        return appId;
    }

    public void setAuthorization(String appId, String appSecret) {
        this.appId = appId;
        this.authorizationHeader = appId + ";" + appSecret;
    }

    private Builder createRequest(WebTarget t) {
        return t.request().header("Authorization", authorizationHeader);
    }

    private KeyserverException parseException(WebApplicationException exception) {
        Response response = exception.getResponse();
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());

        if (status == Response.Status.FORBIDDEN || status == Response.Status.UNAUTHORIZED) {
            CallForbiddenException f = null;
            try {
                f = response.readEntity(CallForbiddenException.class);
            } catch (ProcessingException | IllegalStateException e) {
                // in some cases (e.g. SecurityInterceptor denies access) we get
                // a string and thus an exception at the above readEntity
                f = new CallForbiddenException("rest call forbidden/unauthorized");
            }
            f.setStatus(status);
            return f;
        }

        if (response.hasEntity()) {
            try {
                if (status == Response.Status.NOT_FOUND) {
                    return response.readEntity(EntryNotFoundException.class);
                } else {
                    return response.readEntity(KeyserverException.class);
                }
            } catch (ProcessingException | IllegalStateException e) {
                return new KeyserverException("unparsable/-known rest response", exception);
            }
        } else {
            return new KeyserverException("unparsable/-known rest response", exception);
        }
    }

    private KeyserverException parseException(RuntimeException exception) {
        if (exception instanceof ProcessingException) {
            return new KeyserverException("error at rest processing", exception);
        } else if (exception instanceof WebApplicationException) {
            return this.parseException((WebApplicationException) exception);
        } else {
            throw exception;
        }
    }

    public List<AppDTO> listApps() throws KeyserverException {
        try {
            return this.createRequest(this.apps).get(APPDTO_LIST_TYPE);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public AppDTO registerApp(Approle role) throws KeyserverException {
        try {
            return this.createRequest(this.apps).post(Entity.form(new Form("role", role.toString())), AppDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public void removeApp(String appId) throws KeyserverException {
        try {
            this.createRequest(this.theApp.resolveTemplate("appId", appId)).delete();
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public AppDTO authenticateApp(String appId, String appKey) throws KeyserverException {
        try {
            return this.createRequest(this.theApp.resolveTemplate("appId", appId)).post(Entity.form(new Form("key", appKey)), AppDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }
}