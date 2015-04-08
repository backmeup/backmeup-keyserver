package org.backmeup.keyserver.rest.resources;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.NotNull;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.backmeup.keyserver.core.KeyserverException;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.dto.AuthResponseDTO;
import org.backmeup.keyserver.rest.auth.TokenRequired;

/**
 * All user and plugin data specific operations will be handled within this
 * class.
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class Users extends SecureBase {

    @RolesAllowed("CORE")
    @POST
    @Path("/")
    public String register(@NotNull @FormParam("username") String username, @NotNull @FormParam("password") String password)
            throws KeyserverException {
        return getKeyserverLogic().registerUser(username, password);
    }

    @RolesAllowed("CORE")
    @POST
    @Path("/authenticate")
    public AuthResponseDTO authenticate(@NotNull @FormParam("username") String username, @NotNull @FormParam("password") String password)
            throws KeyserverException {
        return getMapper().map(this.getKeyserverLogic().authenticateUserWithPassword(username, password), AuthResponseDTO.class);
    }

    @RolesAllowed("CORE")
    @TokenRequired
    @DELETE
    @Path("/{serviceUserId}")
    public void remove(@PathParam("serviceUserId") String serviceUserId) throws KeyserverException {
        this.checkServiceUserId(serviceUserId);
        AuthResponse auth = this.getAuthResponse();
        getKeyserverLogic().removeUser(auth.getServiceUserId(), auth.getUsername(), auth.getAccountKey());
    }

    @RolesAllowed("CORE")
    @DELETE
    @Path("/{serviceUserId}/adminRemove")
    public void adminRemove(@PathParam("serviceUserId") String serviceUserId, @NotNull @QueryParam("username") String username)
            throws KeyserverException {
        getKeyserverLogic().removeUser(serviceUserId, username);
    }

    @RolesAllowed({ "CORE", "INDEXER" })
    @TokenRequired
    @GET
    @Path("/{serviceUserId}/index_key")
    public String getIndexKey(@PathParam("serviceUserId") String serviceUserId) throws KeyserverException {
        this.checkServiceUserId(serviceUserId);
        AuthResponse auth = this.getAuthResponse();
        return this.getKeyserverLogic().getIndexKey(auth.getUserId(), auth.getAccountKey());
    }

    @RolesAllowed({ "CORE", "INDEXER", "WORKER" })
    @TokenRequired
    @GET
    @Path("/{serviceUserId}/profile")
    public String getProfile(@PathParam("serviceUserId") String serviceUserId) throws KeyserverException {
        this.checkServiceUserId(serviceUserId);
        AuthResponse auth = this.getAuthResponse();
        return this.getKeyserverLogic().getProfile(auth.getUserId(), auth.getAccountKey());
    }

    @RolesAllowed({ "CORE" })
    @TokenRequired
    @POST
    @Path("/{serviceUserId}/profile")
    public void setProfile(@PathParam("serviceUserId") String serviceUserId, @NotNull @FormParam("profile") String profile) throws KeyserverException {
        this.checkServiceUserId(serviceUserId);
        AuthResponse auth = this.getAuthResponse();
        this.getKeyserverLogic().setProfile(auth.getUserId(), auth.getAccountKey(), profile);
    }

    @RolesAllowed({ "CORE" })
    @TokenRequired
    @POST
    @Path("/{serviceUserId}/changePassword")
    public void changePassword(@PathParam("serviceUserId") String serviceUserId, @NotNull @FormParam("oldPassword") String oldPassword,
            @NotNull @FormParam("newPassword") String newPassword) throws KeyserverException {
        this.checkServiceUserId(serviceUserId);
        AuthResponse auth = this.getAuthResponse();
        this.getKeyserverLogic().changeUserPassword(auth.getUserId(), auth.getUsername(), oldPassword, newPassword);
    }

    @RolesAllowed({ "CORE" })
    @TokenRequired
    @POST
    @Path("/{serviceUserId}/plugins/")
    public void createPluginData(@PathParam("serviceUserId") String serviceUserId, @FormParam("pluginId") String pluginId,
            @NotNull @FormParam("data") String data) throws KeyserverException {
        this.checkServiceUserId(serviceUserId);
        AuthResponse auth = this.getAuthResponse();
        this.getKeyserverLogic().createPluginData(auth.getUserId(), pluginId, auth.getAccountKey(), data);
    }

    @RolesAllowed({ "CORE", "WORKER" })
    @TokenRequired
    @GET
    @Path("/{serviceUserId}/plugins/{pluginId}")
    public String getPluginData(@PathParam("serviceUserId") String serviceUserId, @PathParam("pluginId") String pluginId) throws KeyserverException {
        this.checkServiceUserId(serviceUserId);
        AuthResponse auth = this.getAuthResponse();
        byte[] pluginKey = this.getKeyserverLogic().getPluginDataKey(auth.getUserId(), pluginId, auth.getAccountKey());
        return this.getKeyserverLogic().getPluginData(auth.getUserId(), pluginId, pluginKey);
    }

    @RolesAllowed({ "CORE", "WORKER" })
    @TokenRequired
    @POST
    @Path("/{serviceUserId}/plugins/{pluginId}")
    public void updatePluginData(@PathParam("serviceUserId") String serviceUserId, @PathParam("pluginId") String pluginId,
            @NotNull @FormParam("data") String data) throws KeyserverException {
        this.checkServiceUserId(serviceUserId);
        AuthResponse auth = this.getAuthResponse();
        byte[] pluginKey = this.getKeyserverLogic().getPluginDataKey(auth.getUserId(), pluginId, auth.getAccountKey());
        this.getKeyserverLogic().updatePluginData(auth.getUserId(), pluginId, pluginKey, data);
    }

    @RolesAllowed({ "CORE" })
    @TokenRequired
    @DELETE
    @Path("/{serviceUserId}/plugins/{pluginId}")
    public void removePluginData(@PathParam("serviceUserId") String serviceUserId, @PathParam("pluginId") String pluginId) throws KeyserverException {
        this.checkServiceUserId(serviceUserId);
        AuthResponse auth = this.getAuthResponse();
        this.getKeyserverLogic().removePluginData(auth.getUserId(), pluginId);
    }
}
