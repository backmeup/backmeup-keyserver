package org.backmeup.keyserver.client;

import java.util.Calendar;
import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
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
import org.backmeup.keyserver.model.KeyserverUtils;
import org.backmeup.keyserver.model.dto.AppDTO;
import org.backmeup.keyserver.model.dto.AuthResponseDTO;
import org.backmeup.keyserver.model.dto.TokenDTO;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * Client for Keyserver operations via Keyserver REST API.
 * @author wolfgang
 *
 */
public class KeyserverClient {
    protected static final GenericType<List<AppDTO>> APPDTO_LIST_TYPE = new GenericType<List<AppDTO>>() {
    };
    protected static final GenericType<List<TokenDTO>> TOKENDTO_LIST_TYPE = new GenericType<List<TokenDTO>>() {
    };
    
    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";

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

    /**
     * Should not be used - only for depency injection.
     */
    public KeyserverClient() {
    }
    
    /**
     * Constructs a new keyserver client.
     * @param baseUrl rest base url for calling the keyserver, e.g. http://themis-keysrv01:8080/backmeup-keyserver-rest
     * @param appId authentication id of client (e.g. SERVICE app).
     * @param appSecret authentication key of client
     */
    public KeyserverClient(String baseUrl, String appId, String appSecret) {
        ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder().connectionPoolSize(20);
        this.client = clientBuilder.build();

        UriBuilder base = UriBuilder.fromUri(baseUrl);
        this.apps = this.client.target(base).path("/applications/");
        this.theApp = this.apps.path("/{appId}");
        this.users = this.client.target(base).path("/users/");
        this.theUser = this.users.path("/tokenUser");
        this.thePlugin = this.users.path("/tokenUser/plugins/{pluginId}");
        this.theToken = this.client.target(base).path("/tokens/{kind}/{token}");

        this.setAuthorization(appId, appSecret);
    }

    public String getAppId() {
        return appId;
    }

    public void setAuthorization(String appId, String appSecret) {
        this.appId = appId;
        this.authorizationHeader = appId + ";" + appSecret;
    }

    private Builder createRequest(WebTarget t) {
        return t.request().header(AUTHORIZATION_HEADER_KEY, authorizationHeader);
    }

    private void parsePostResponse(Response response) throws KeyserverException {
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (status.getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw this.parseException(null, response);
        }
    }
    
    private KeyserverException parseException(WebApplicationException exception) {
        return this.parseException(exception, exception.getResponse());
    }
    
    private KeyserverException parseException(WebApplicationException exception, Response response) { //NOSONAR
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());

        if (status == Response.Status.FORBIDDEN || status == Response.Status.UNAUTHORIZED) {
            CallForbiddenException f = null;
            try {
                f = response.readEntity(CallForbiddenException.class);
            } catch (ProcessingException | IllegalStateException e) {
                // in some cases (e.g. SecurityInterceptor denies access) we get
                // a string and thus an exception at the above readEntity
                f = new CallForbiddenException("rest call forbidden/unauthorized", e);
            }
            f.setStatus(status);
            response.close();
            return f;
        }
        
        try {
            if (response.hasEntity()) {
                if (status == Response.Status.NOT_FOUND) {
                    return response.readEntity(EntryNotFoundException.class);
                } else {
                    return response.readEntity(KeyserverException.class);
                }
            } else {
                return new KeyserverException("unparsable/-known rest response", exception);
            }
        } catch (ProcessingException | IllegalStateException e) {
            if (status == Response.Status.NOT_FOUND) {
                return new KeyserverException("rest endpoint not found", exception);
            } else {
                return new KeyserverException("unparsable/-known rest response", e);
            }
        } finally {
            response.close();
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
        return this.theApp.resolveTemplate("appId", appId).request().header(AUTHORIZATION_HEADER_KEY, authorizationHeader);
    }

    /**
     * Lists all apps that are registered on the keyserver.
     * Only a keyserver client which is authenticated as SERVICE app can use this method.
     * @return list of AppDTOs.
     * @throws KeyserverException
     */
    public List<AppDTO> listApps() throws KeyserverException {
        try {
            return this.createRequest(this.apps).get(APPDTO_LIST_TYPE);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Register a new app on the keyserver.
     * Only a keyserver client which is authenticated as SERVICE app can use this method.
     * @param role role of the app to register.
     * @return AppDTO with app ID and key for later use.
     * @throws KeyserverException
     */
    public AppDTO registerApp(Approle role) throws KeyserverException {
        try {
            return this.createRequest(this.apps).post(Entity.form(new Form("role", role.toString())), AppDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }
    
    /**
     * Removes an app from keyserver.
     * Only a keyserver client which is authenticated as SERVICE app can use this method.
     * @param appId id of the app to remove.
     * @throws KeyserverException
     */
    public void removeApp(String appId) throws KeyserverException {
        try {
            Response r = this.createAppSpecificRequest(appId).delete();
            this.parsePostResponse(r);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Authenticates an app at keyserver.
     * @param appId
     * @param appKey
     * @return AppDTO including role of app - if authentication is valid.
     * @throws KeyserverException at any error or invalid authentication.
     */
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

        return target.request().header(AUTHORIZATION_HEADER_KEY, authorizationHeader).header("Token", t.toTokenString());
    }

    /**
     * Register a new user at keyserver.
     * Only a keyserver client which is authenticated as SERVICE app can use this method.
     * @param username
     * @param password
     * @return the (external) user id for later use.
     * @throws KeyserverException
     */
    public String registerUser(String username, String password) throws KeyserverException {
        try {
            Form f = new Form().param("username", username).param("password", password);

            return this.createRequest(this.users).post(Entity.form(f), String.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }
    
    /**
     * Register an anonymous user at keyserver.
     * Only a keyserver client which is authenticated as SERVICE app can use this method.
     * @param token the authentication token that identifies the decedant user.
     * @return an AuthResponse - including an activation token - for the anonymous user
     * @throws KeyserverException
     */
    public AuthResponseDTO registerAnonymousUser(TokenDTO token) throws KeyserverException {
        try {
            return this.createRequest(this.users.path("/anonymousUser")).header("Token", token.toTokenString()).post(Entity.form(new Form()), AuthResponseDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Authenticate user with username and password.
     * Only a keyserver client which is authenticated as SERVICE app can use this method.
     * @param username
     * @param password
     * @return AuthResponseDTO object with user infos and internal token for later use/authentication.
     * @throws KeyserverException at any error or invalid authentication.
     */
    public AuthResponseDTO authenticateUserWithPassword(String username, String password) throws KeyserverException {
        try {
            Form f = new Form().param("username", username).param("password", password);

            return this.createRequest(this.theUser).post(Entity.form(f), AuthResponseDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Gets the user profile.
     * @param token the authentication token that identifies the user.
     * @return user profile as string, interpretation is up to caller.
     * @throws KeyserverException
     */
    public String getProfile(TokenDTO token) throws KeyserverException {
        try {
            return this.createUserSpecificRequest("/profile", token).get(String.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Sets the user profile.
     * Only a keyserver client which is authenticated as SERVICE app can use this method.
     * @param token the authentication token that identifies the user.
     * @param profile user profile as string, interpretation is up to caller.
     * @throws KeyserverException
     */
    public void setProfile(TokenDTO token, String profile) throws KeyserverException {
        try {
            Response r = this.createUserSpecificRequest("/profile", token).post(Entity.form(new Form("profile", profile)));
            this.parsePostResponse(r);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Gets the key for index en-/decryption.
     * Only a keyserver client which is authenticated as SERVICE or INDEXER app can use this method.
     * @param token the authentication token that identifies the user.
     * @return the encryption key.
     * @throws KeyserverException
     */
    public String getIndexKey(TokenDTO token) throws KeyserverException {
        try {
            return this.createUserSpecificRequest("/index_key", token).get(String.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Gets the public key of user for data encryption.
     * Only a keyserver client which is authenticated as SERVICE, STORAGE or INDEXER app can use this method.
     * @param token the authentication token that identifies the user or a backup of this user.
     * @return the public key.
     * @throws KeyserverException
     */
    public byte[] getPublicKey(TokenDTO token) throws KeyserverException {
        try {
            return KeyserverUtils.fromBase64String(this.createUserSpecificRequest("/public_key", token).get(String.class));
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }
    
    /**
     * Gets the private key of user for data decryption.
     * Only a keyserver client which is authenticated as SERVICE, STORAGE or INDEXER app can use this method.
     * @param token the authentication token that identifies the user.
     * @return the private key.
     * @throws KeyserverException
     */
    public byte[] getPrivateKey(TokenDTO token) throws KeyserverException {
        try {
            return KeyserverUtils.fromBase64String(this.createUserSpecificRequest("/private_key", token).get(String.class));
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Removes the user and all of its data from keyserver.
     * Only a keyserver client which is authenticated as SERVICE app can use this method.
     * @param token the authentication token that identifies the user.
     * @throws KeyserverException
     */
    public void removeUser(TokenDTO token) throws KeyserverException {
        try {
            Response r = this.createUserSpecificRequest(token).delete();
            this.parsePostResponse(r);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Does an administrative remove of the user and all of its data from keyserver.
     * Only a keyserver client which is authenticated as SERVICE app can use this method.
     * @param serviceUserId the (external) user id which was created at user registration.
     * @param username the username of the user to remove.
     * @throws KeyserverException
     */
    public void removeUserByAdmin(String serviceUserId, String username) throws KeyserverException {
        try {
            Response r = this.createRequest(this.users.path("/adminRemove").queryParam("serviceUserId", serviceUserId).queryParam("username", username)).delete();
            this.parsePostResponse(r);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Change the password of an user.
     * Only a keyserver client which is authenticated as SERVICE app can use this method.
     * @param token the authentication token that identifies the user.
     * @param oldPassword
     * @param newPassword
     * @throws KeyserverException at any error or invalid authentication.
     */
    public void changeUserPassword(TokenDTO token, String oldPassword, String newPassword) throws KeyserverException {
        Form f = new Form().param("oldPassword", oldPassword).param("newPassword", newPassword);

        try {
            Response r = this.createUserSpecificRequest("/changePassword", token).post(Entity.form(f));
            this.parsePostResponse(r);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    // =========================================================================
    // PluginData logic
    // =========================================================================

    private Builder createPluginSpecificRequest(String pluginId, TokenDTO t) {
        return this.thePlugin.resolveTemplate("pluginId", pluginId).request().header(AUTHORIZATION_HEADER_KEY, authorizationHeader)
                .header("Token", t.toTokenString());
    }

    /**
     * Create (data)store for plugin and set data.
     * Only a keyserver client which is authenticated as SERVICE or WORKER app can use this method.
     * @param token the authentication token that identifies the user or a backup of this user.
     * @param pluginId id for the plugin store that should be created. Has to be unique within the user account.
     * @param data data to store into plugin store. Interpretation is up to caller.
     * @throws KeyserverException
     */
    public void createPluginData(TokenDTO token, String pluginId, String data) throws KeyserverException {
        Form f = new Form().param("pluginId", pluginId).param("data", data);

        try {
            Response r = this.createUserSpecificRequest("/plugins/", token).post(Entity.form(f));
            this.parsePostResponse(r);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Get data from specified plugin store.
     * Only a keyserver client which is authenticated as SERVICE or WORKER app can use this method.
     * @param token the authentication token that identifies the user or a backup of this user.
     * @param pluginId the id of the plugin store to get data from.
     * @return data from the plugin store. Interpretation is up to caller.
     * @throws KeyserverException
     */
    public String getPluginData(TokenDTO token, String pluginId) throws KeyserverException {
        try {
            return this.createPluginSpecificRequest(pluginId, token).get(String.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Updates data in plugin store but doesn't create a store if not existant.
     * @see KeyserverClient#updatePluginData(TokenDTO, String, String, boolean).
     */
    public void updatePluginData(TokenDTO token, String pluginId, String data) throws KeyserverException {
        this.updatePluginData(token, pluginId, data, false);
    }
    
    /**
     * Updates data in plugin store.
     * Only a keyserver client which is authenticated as SERVICE or WORKER app can use this method.
     * @param token the authentication token that identifies the user or a backup of this user.
     * @param pluginId the id of the plugin store to update.
     * @param data data to store into plugin store. Interpretation is up to caller.
     * @param create if true, create store if not existant.
     * @throws KeyserverException
     */
    public void updatePluginData(TokenDTO token, String pluginId, String data, boolean create) throws KeyserverException {
        Form f = new Form().param("data", data).param("create", Boolean.toString(create));
        
        try {
            Response r = this.createPluginSpecificRequest(pluginId, token).post(Entity.form(f));
            this.parsePostResponse(r);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Remove plugin (data)store.
     * Only a keyserver client which is authenticated as SERVICE or WORKER app can use this method.
     * @param token the authentication token that identifies the user or a backup of this user.
     * @param pluginId the id of the plugin store to remove.
     * @throws KeyserverException
     */
    public void removePluginData(TokenDTO token, String pluginId) throws KeyserverException {
        try {
            Response r = this.createPluginSpecificRequest(pluginId, token).delete();
            this.parsePostResponse(r);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    // =========================================================================
    // Token logic
    // =========================================================================

    private Builder createTokenSpecificRequest(TokenDTO token) {
        return this.theToken.resolveTemplate("kind", token.getKind()).resolveTemplate("token", token.getB64Token()).request()
                .header(AUTHORIZATION_HEADER_KEY, authorizationHeader);
    }

    /**
     * Authenticate internal token.
     * @param token the internal token to authenticate.
     * @return AuthResponseDTO object with user infos and internal token for later use/authentication.
     * @throws KeyserverException at any error or invalid authentication.
     */
    public AuthResponseDTO authenticateWithInternalToken(TokenDTO token) throws KeyserverException {
        try {
            return this.createTokenSpecificRequest(token).post(Entity.form(new Form()), AuthResponseDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Revoke token (any kind) at keyserver.
     * @param token the token to revoke.
     * @throws KeyserverException
     */
    public void revokeToken(TokenDTO token) throws KeyserverException {
        try {
            Response r = this.createTokenSpecificRequest(token).delete();
            this.parsePostResponse(r);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }

    }

    /**
     * Creates an onetime token for later use at a backup.
     * Only a keyserver client which is authenticated as SERVICE app can use this method.
     * @param token the authentication token that identifies the user.
     * @param pluginIds puginIds which should be included in the onetime tokens. 
     *  Only the specified plugin data stores can be retrieved later on with the generated onetime token.
     * @param scheduledExecutionTime the scheduled execution time of the backup.
     * @return AuthResponseDTO object with user infos and onetime token for later use at a backup 
     *  (has to be converted to an internal token, see {@link #authenticateWithOnetime(TokenDTO)}).
     * @throws KeyserverException
     */
    public AuthResponseDTO createOnetimeForBackup(TokenDTO token, String[] pluginIds, Calendar scheduledExecutionTime) throws KeyserverException {
        Form f = new Form().param("scheduledExecutionTime", "" + scheduledExecutionTime.getTime().getTime());
        for (String pluginId : pluginIds) {
            f.param("pluginId", pluginId);
        }

        try {
            return this.createUserSpecificRequest("/tokens/onetime/backup", token).post(Entity.form(f), AuthResponseDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }
    
    /**
     * Creates an onetime token for later use to authenticate the user.
     * Only a keyserver client which is authenticated as SERVICE app can use this method.
     * @param token the authentication token that identifies the user.
     * @return AuthResponseDTO object with user infos and onetime token for later use 
     *  (has to be converted to an internal token, see {@link #authenticateWithOnetime(TokenDTO)}).
     * @throws KeyserverException
     */
    public AuthResponseDTO createOnetimeForAuthentication(TokenDTO token) throws KeyserverException {
        try {
            return this.createUserSpecificRequest("/tokens/onetime/authentication", token).post(Entity.form(new Form()), AuthResponseDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * Authenticate onetime token. This transforms the onetime token to an internal token.
     * @see KeyserverClient#authenticateWithOnetime(TokenDTO, boolean, Calendar)
     * @param token the onetime token to authenticate.
     * @return AuthResponseDTO object with user infos and internal token for later use/authentication.
     * @throws KeyserverException at any error or invalid authentication.
     */
    public AuthResponseDTO authenticateWithOnetime(TokenDTO token) throws KeyserverException {
        return this.authenticateWithOnetime(token, false, null);
    }

    /**
     * Authenticate onetime token. This transforms the onetime token to an internal token.
     * @param token the onetime token to authenticate.
     * @param renew should a new token be derived from this one? If a new token is needed, renew has to be true for a token of role AUTHORIZATION and will be automatically set to true if nextScheduledExecutionTime is given (tokens of role BACKUP_JOB).
     * @param nextScheduledExecutionTime if not null, retrieve the new onetime token for the given execution time. 
     * @return AuthResponseDTO object with user infos and internal token for later use/authentication, including a next onetime token (if requested).
     * @throws KeyserverException at any error or invalid authentication.
     */
    public AuthResponseDTO authenticateWithOnetime(TokenDTO token, boolean renew, Calendar nextScheduledExecutionTime) throws KeyserverException {
        try {
            Form f = new Form();
            boolean shouldRenew = renew;

            if (nextScheduledExecutionTime == null) {
                f.param("nextScheduledExecutionTime", null);
            } else {
                shouldRenew = true;
                f.param("nextScheduledExecutionTime", Long.toString(nextScheduledExecutionTime.getTime().getTime()));
            }
            f.param("renew", Boolean.toString(shouldRenew));
            
            return this.createTokenSpecificRequest(token).post(Entity.form(f), AuthResponseDTO.class);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }

    /**
     * List tokens of specified user. Only tokens with annotation get listed (this excludes internal and onetime tokens).
     * @param token the authentication token that identifies the user.
     * @return a list of user tokens as TokenDTO objects.
     * @throws KeyserverException
     */
    public List<TokenDTO> listTokens(TokenDTO token) throws KeyserverException {
        try {
            return this.createUserSpecificRequest("/tokens/", token).get(TOKENDTO_LIST_TYPE);
        } catch (WebApplicationException | ProcessingException exception) {
            throw this.parseException(exception);
        }
    }
}