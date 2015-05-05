package org.backmeup.keyserver.client;

import java.util.Calendar;
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
import org.backmeup.keyserver.model.dto.AuthResponseDTO;
import org.backmeup.keyserver.model.dto.TokenDTO;

public class KeyserverClient {
    protected static final GenericType<List<AppDTO>> APPDTO_LIST_TYPE = new GenericType<List<AppDTO>>() {
    };
    protected static final GenericType<List<TokenDTO>> TOKENDTO_LIST_TYPE = new GenericType<List<TokenDTO>>() {
    };

    @SuppressWarnings("PMD.SingularField")
    private Client client;
    private WebTarget apps;
    private WebTarget theApp;
    private WebTarget users;
    private WebTarget theUser;
    private WebTarget thePlugin;
    private WebTarget theToken;
    private String appId;
    private String authorizationHeader;

    public KeyserverClient(String baseUrl, String appId, String appSecret) {
        this.client = ClientBuilder.newClient();

        UriBuilder base = UriBuilder.fromUri(baseUrl);
        this.apps = this.client.target(base).path("/applications/");
        this.theApp = this.apps.path("/{appId}");
        this.users = this.client.target(base).path("/users/");
        this.theUser = this.users.path("/tokenUser");
        this.thePlugin = this.users.path("/tokenUser/plugins/{pluginId}");
        this.theToken = this.client.target(base).path("/tokens/{kind}/{token}");

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

    // =========================================================================
    // App logic
    // =========================================================================

    private Builder createAppSpecificRequest(String appId) {
        return this.theApp.resolveTemplate("appId", appId).request().header("Authorization", authorizationHeader);
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
            this.createAppSpecificRequest(appId).delete();
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public AppDTO authenticateApp(String appId, String appKey) throws KeyserverException {
        try {
            return this.createAppSpecificRequest(appId).post(Entity.form(new Form("key", appKey)), AppDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    // =========================================================================
    // User logic
    // =========================================================================

    private Builder createUserSpecificRequest(TokenDTO t) {
        return this.createUserSpecificRequest(null, t);
    }

    private Builder createUserSpecificRequest(String path, TokenDTO t) {
        WebTarget target = this.theUser;
        if (path != null) {
            target = target.path(path);
        }

        return target.request().header("Authorization", authorizationHeader).header("Token", t.toTokenString());
    }

    public String registerUser(String username, String password) throws KeyserverException {
        try {
            Form f = new Form().param("username", username).param("password", password);

            return this.createRequest(this.users).post(Entity.form(f), String.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public AuthResponseDTO authenticateUserWithPassword(String username, String password) throws KeyserverException {
        try {
            Form f = new Form().param("username", username).param("password", password);

            return this.createRequest(this.theUser).post(Entity.form(f), AuthResponseDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public String getProfile(TokenDTO token) throws KeyserverException {
        try {
            return this.createUserSpecificRequest("/profile", token).get(String.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public void setProfile(TokenDTO token, String profile) throws KeyserverException {
        try {
            this.createUserSpecificRequest("/profile", token).post(Entity.form(new Form("profile", profile)));
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public String getIndexKey(TokenDTO token) throws KeyserverException {
        try {
            return this.createUserSpecificRequest("/index_key", token).get(String.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public void removeUser(TokenDTO token) throws KeyserverException {
        try {
            this.createUserSpecificRequest(token).delete();
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public void removeUserByAdmin(String serviceUserId, String username) throws KeyserverException {
        try {
            this.createRequest(this.users.path("/adminRemove").queryParam("serviceUserId", serviceUserId).queryParam("username", username)).delete();
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public void changeUserPassword(TokenDTO token, String oldPassword, String newPassword) throws KeyserverException {
        Form f = new Form().param("oldPassword", oldPassword).param("newPassword", newPassword);

        try {
            this.createUserSpecificRequest("/changePassword", token).post(Entity.form(f));
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    // =========================================================================
    // PluginData logic
    // =========================================================================

    private Builder createPluginSpecificRequest(String pluginId, TokenDTO t) {
        return this.thePlugin.resolveTemplate("pluginId", pluginId).request().header("Authorization", authorizationHeader)
                .header("Token", t.toTokenString());
    }

    public void createPluginData(TokenDTO token, String pluginId, String data) throws KeyserverException {
        Form f = new Form().param("pluginId", pluginId).param("data", data);

        try {
            this.createUserSpecificRequest("/plugins/", token).post(Entity.form(f));
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public String getPluginData(TokenDTO token, String pluginId) throws KeyserverException {
        try {
            return this.createPluginSpecificRequest(pluginId, token).get(String.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public void updatePluginData(TokenDTO token, String pluginId, String data) throws KeyserverException {
        try {
            this.createPluginSpecificRequest(pluginId, token).post(Entity.form(new Form("data", data)));
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public void removePluginData(TokenDTO token, String pluginId) throws KeyserverException {
        try {
            this.createPluginSpecificRequest(pluginId, token).delete();
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    // =========================================================================
    // Token logic
    // =========================================================================

    private Builder createTokenSpecificRequest(TokenDTO token) {
        return this.theToken.resolveTemplate("kind", token.getKind()).resolveTemplate("token", token.getB64Token()).request()
                .header("Authorization", authorizationHeader);
    }

    public AuthResponseDTO authenticateWithInternalToken(TokenDTO token) throws KeyserverException {
        try {
            return this.createTokenSpecificRequest(token).post(Entity.form(new Form()), AuthResponseDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public void revokeToken(TokenDTO token) throws KeyserverException {
        try {
            this.createTokenSpecificRequest(token).delete();
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }

    }

    public AuthResponseDTO createOnetime(TokenDTO token, String[] pluginIds, Calendar scheduledExecutionTime) throws KeyserverException {
        Form f = new Form().param("scheduledExecutionTime", "" + scheduledExecutionTime.getTime().getTime());
        for (String pluginId : pluginIds) {
            f.param("pluginId", pluginId);
        }

        try {
            return this.createUserSpecificRequest("/tokens/onetime", token).post(Entity.form(f), AuthResponseDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public AuthResponseDTO authenticateWithOnetime(TokenDTO token) throws KeyserverException {
        return this.authenticateWithOnetime(token, null);
    }

    public AuthResponseDTO authenticateWithOnetime(TokenDTO token, Calendar nextScheduledExecutionTime) throws KeyserverException {
        try {
            Form f = new Form();

            if (nextScheduledExecutionTime == null) {
                f.param("nextScheduledExecutionTime", null);
            } else {
                f.param("nextScheduledExecutionTime", "" + nextScheduledExecutionTime.getTime().getTime());
            }
            return this.createTokenSpecificRequest(token).post(Entity.form(f), AuthResponseDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    public List<TokenDTO> listTokens(TokenDTO token) throws KeyserverException {
        try {
            return this.createUserSpecificRequest("/tokens/", token).get(TOKENDTO_LIST_TYPE);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }
}