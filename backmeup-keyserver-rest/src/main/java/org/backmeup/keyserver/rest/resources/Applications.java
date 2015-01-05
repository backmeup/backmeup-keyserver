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
@Produces(MediaType.APPLICATION_JSON)
public class Applications extends SecureBase {

    @RolesAllowed("CORE")
    @GET
    @Path("/")
    public List<AppDTO> listApps() throws KeyserverException {
        List<AppDTO> appList = new ArrayList<>();
        
        List<App> apps = getKeyserverLogic().listApps(this.getApp().getPassword());
        for (App a : apps) {
            appList.add(getMapper().map(a, AppDTO.class));
        }
        
        return appList;
    }
    
    @RolesAllowed("CORE")
    @POST
    @Path("/")
    public AppDTO register(@NotNull @FormParam("role") App.Approle role) throws KeyserverException {
        return getMapper().map(getKeyserverLogic().registerApp(role), AppDTO.class);
    }
    
    @RolesAllowed("CORE")
    @DELETE
    @Path("/{appId}/")
    public void remove(@PathParam("appId") String appId) throws KeyserverException {
        this.getKeyserverLogic().removeApp(appId);
    }
    
    @RolesAllowed({"CORE", "WORKER", "STORAGE", "INDEXER"})
    @POST
    @Path("/{appId}/")
    public void authenticate(@PathParam("appId") String appId, @NotNull @FormParam("key") String appKey) throws KeyserverException {
        this.getKeyserverLogic().authenticateApp(appId, appKey);
    }
}
