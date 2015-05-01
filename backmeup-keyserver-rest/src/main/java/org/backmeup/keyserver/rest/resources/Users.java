package org.backmeup.keyserver.rest.resources;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.keyserver.model.Token;
import org.backmeup.keyserver.model.TokenValue;
import org.backmeup.keyserver.model.App.Approle;
import org.backmeup.keyserver.model.TokenValue.Role;
import org.backmeup.keyserver.model.dto.AuthResponseDTO;
import org.backmeup.keyserver.model.dto.TokenDTO;
import org.backmeup.keyserver.rest.auth.AppsAllowed;
import org.backmeup.keyserver.rest.auth.TokenRequired;

/**
 * All user and plugin data specific operations will be handled within this
 * class.
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class Users extends SecureBase {

    protected byte[] getPluginKey(String pluginId) throws KeyserverException {
        AuthResponse auth = this.getAuthResponse();
        byte[] pluginKey = new byte[0];

        if (auth.getRoles().contains(Role.USER)) {
            pluginKey = this.getKeyserverLogic().getPluginDataKey(auth.getUserId(), pluginId, auth.getAccountKey());
        } else if (auth.getRoles().contains(Role.BACKUP_JOB)) {
            pluginKey = auth.getPluginKey(pluginId);
            if (pluginKey.length == 0) {
                throw new ForbiddenException("token has no access to plugin " + pluginId);
            }
        }

        return pluginKey;
    }

    @AppsAllowed(Approle.CORE)
    @POST
    @Path("/")
    public String register(@NotNull @FormParam("username") String username, @NotNull @FormParam("password") String password)
            throws KeyserverException {
        return getKeyserverLogic().registerUser(username, password);
    }

    @AppsAllowed(Approle.CORE)
    @POST
    @Path("/tokenUser")
    public AuthResponseDTO authenticate(@NotNull @FormParam("username") String username, @NotNull @FormParam("password") String password)
            throws KeyserverException {
        return this.map(this.getKeyserverLogic().authenticateUserWithPassword(username, password), AuthResponseDTO.class);
    }

    @AppsAllowed(Approle.CORE)
    @TokenRequired
    @DELETE
    @Path("/tokenUser")
    public void remove() throws KeyserverException {
        AuthResponse auth = this.getAuthResponse();
        getKeyserverLogic().removeUser(auth.getServiceUserId(), auth.getUsername(), auth.getAccountKey());
    }

    @AppsAllowed(Approle.CORE)
    @DELETE
    @Path("/adminRemove")
    public void adminRemove(@NotNull @QueryParam("serviceUserId") String serviceUserId, @NotNull @QueryParam("username") String username)
            throws KeyserverException {
        getKeyserverLogic().removeUser(serviceUserId, username);
    }

    @AppsAllowed({ Approle.CORE, Approle.INDEXER })
    @TokenRequired
    @GET
    @Path("/tokenUser/index_key")
    public String getIndexKey() throws KeyserverException {
        AuthResponse auth = this.getAuthResponse();
        return this.getKeyserverLogic().getIndexKey(auth.getUserId(), auth.getAccountKey());
    }

    @AppsAllowed({ Approle.CORE, Approle.WORKER, Approle.STORAGE, Approle.INDEXER })
    @TokenRequired
    @GET
    @Path("/tokenUser/profile")
    public String getProfile() throws KeyserverException {
        AuthResponse auth = this.getAuthResponse();
        return this.getKeyserverLogic().getProfile(auth.getUserId(), auth.getAccountKey());
    }

    @AppsAllowed(Approle.CORE)
    @TokenRequired
    @POST
    @Path("/tokenUser/profile")
    public void setProfile(@NotNull @FormParam("profile") String profile) throws KeyserverException {
        AuthResponse auth = this.getAuthResponse();
        this.getKeyserverLogic().setProfile(auth.getUserId(), auth.getAccountKey(), profile);
    }

    @AppsAllowed(Approle.CORE)
    @TokenRequired
    @POST
    @Path("/tokenUser/changePassword")
    public void changePassword(@NotNull @FormParam("oldPassword") String oldPassword, @NotNull @FormParam("newPassword") String newPassword)
            throws KeyserverException {
        AuthResponse auth = this.getAuthResponse();
        this.getKeyserverLogic().changeUserPassword(auth.getUserId(), auth.getUsername(), oldPassword, newPassword);
    }

    @AppsAllowed({ Approle.CORE, Approle.WORKER })
    @TokenRequired({ TokenValue.Role.USER, TokenValue.Role.BACKUP_JOB })
    @POST
    @Path("/tokenUser/plugins/")
    public void createPluginData(@FormParam("pluginId") String pluginId, @NotNull @FormParam("data") String data) throws KeyserverException {
        AuthResponse auth = this.getAuthResponse();
        this.getKeyserverLogic().createPluginData(auth.getUserId(), pluginId, auth.getAccountKey(), data);
    }

    @AppsAllowed({ Approle.CORE, Approle.WORKER })
    @TokenRequired({ TokenValue.Role.USER, TokenValue.Role.BACKUP_JOB })
    @GET
    @Path("/tokenUser/plugins/{pluginId}")
    public String getPluginData(@PathParam("pluginId") String pluginId) throws KeyserverException {
        AuthResponse auth = this.getAuthResponse();
        byte[] pluginKey = this.getPluginKey(pluginId);
        return this.getKeyserverLogic().getPluginData(auth.getUserId(), pluginId, pluginKey);
    }

    @AppsAllowed({ Approle.CORE, Approle.WORKER })
    @TokenRequired({ TokenValue.Role.USER, TokenValue.Role.BACKUP_JOB })
    @POST
    @Path("/tokenUser/plugins/{pluginId}")
    public void updatePluginData(@PathParam("pluginId") String pluginId, @NotNull @FormParam("data") String data) throws KeyserverException {
        AuthResponse auth = this.getAuthResponse();
        byte[] pluginKey = this.getPluginKey(pluginId);
        this.getKeyserverLogic().updatePluginData(auth.getUserId(), pluginId, pluginKey, data);
    }

    @AppsAllowed({ Approle.CORE, Approle.WORKER })
    @TokenRequired({ TokenValue.Role.USER, TokenValue.Role.BACKUP_JOB })
    @DELETE
    @Path("/tokenUser/plugins/{pluginId}")
    public void removePluginData(@PathParam("pluginId") String pluginId) throws KeyserverException {
        AuthResponse auth = this.getAuthResponse();
        this.getKeyserverLogic().removePluginData(auth.getUserId(), pluginId);
    }

    @AppsAllowed(Approle.CORE)
    @TokenRequired
    @POST
    @Path("/tokenUser/tokens/onetime")
    public AuthResponseDTO createOnetimeToken(@FormParam("pluginId") String[] pluginIds,
            @NotNull @FormParam("scheduledExecutionTime") Long scheduledExecutionTime) throws KeyserverException {
        AuthResponse auth = this.getAuthResponse();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis((Long) scheduledExecutionTime);
        return this.map(
                this.getKeyserverLogic().createOnetime(auth.getUserId(), auth.getServiceUserId(), auth.getUsername(), auth.getAccountKey(),
                        pluginIds, cal), AuthResponseDTO.class);
    }

    @AppsAllowed(Approle.CORE)
    @TokenRequired
    @GET
    @Path("/tokenUser/tokens")
    public List<TokenDTO> listTokens(@QueryParam("kind") Token.Kind kind) throws KeyserverException {
        AuthResponse auth = this.getAuthResponse();
        List<Token> tokens = null;

        if (kind != null) {
            tokens = this.getKeyserverLogic().listTokens(auth.getUserId(), auth.getAccountKey(), kind);
        } else {
            tokens = new ArrayList<Token>();
            for (Token.Kind k : Token.Kind.values()) {
                tokens.addAll(this.getKeyserverLogic().listTokens(auth.getUserId(), auth.getAccountKey(), k));
            }
        }

        return this.map(tokens, TokenDTO.class);
    }
}
