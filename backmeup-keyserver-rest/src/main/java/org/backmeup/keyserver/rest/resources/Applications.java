package org.backmeup.keyserver.rest.resources;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.backmeup.keyserver.core.EntryNotFoundException;
import org.backmeup.keyserver.core.KeyserverException;
import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.dto.AppDTO;

/**
 * All app specific operations will be handled within this class.
 */
@Path("/applications")
public class Applications extends SecureBase {

    @RolesAllowed("CORE")
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AppDTO> listApps() {
        List<AppDTO> appList = new ArrayList<>();
        try {
            List<App> apps = getKeyserverLogic().listApps(this.getApp().getPassword());
            for (App a : apps) {
                appList.add(getMapper().map(a, AppDTO.class));
            }
        } catch(KeyserverException e) {
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
        return appList;
    }
    
    @RolesAllowed("CORE")
    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public AppDTO register(@NotNull @FormParam("role") App.Approle role) {
        try {
            return getMapper().map(getKeyserverLogic().registerApp(role), AppDTO.class);
        } catch(KeyserverException e) {
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    @RolesAllowed("CORE")
    @DELETE
    @Path("/{appId}/")
    @Produces(MediaType.APPLICATION_JSON)
    public void remove(@PathParam("appId") String appId) {
        try {
            this.getKeyserverLogic().removeApp(appId);
        } catch(EntryNotFoundException e) {
            throw new WebApplicationException(e, Status.NOT_FOUND);
        } catch(KeyserverException e) {
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    @RolesAllowed({"CORE", "WORKER", "STORAGE", "INDEXER"})
    @POST
    @Path("/{appId}/")
    @Produces(MediaType.APPLICATION_JSON)
    public void authenticate(@PathParam("appId") String appId, @NotNull @FormParam("key") String appKey) {
        try {
            this.getKeyserverLogic().authenticateApp(appId, appKey);
        } catch(EntryNotFoundException e) {
            throw new WebApplicationException(e, Status.NOT_FOUND);
        } catch(KeyserverException e) {
            throw new WebApplicationException(e, Status.UNAUTHORIZED);
        }
    }
}
