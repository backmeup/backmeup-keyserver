package org.backmeup.keyserver.rest.resources;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.keyserver.model.App.Approle;
import org.backmeup.keyserver.model.dto.AppDTO;
import org.backmeup.keyserver.model.dto.AuthResponseDTO;
import org.backmeup.keyserver.rest.auth.AppsAllowed;

/**
 * All app specific operations will be handled within this class.
 * @author wolfgang
 */
@Path("/applications")
@Produces(MediaType.APPLICATION_JSON)
public class Applications extends SecureBase {

    @AppsAllowed(Approle.SERVICE)
    @GET
    @Path("/")
    public List<AppDTO> listApps() throws KeyserverException {
        List<App> apps = getKeyserverLogic().listApps(this.getApp().getPassword());
        return this.map(apps, AppDTO.class);
    }

    @AppsAllowed(Approle.SERVICE)
    @POST
    @Path("/")
    public AppDTO register(@NotNull @FormParam("role") Approle role) throws KeyserverException {
        return this.map(getKeyserverLogic().registerApp(role), AppDTO.class);
    }

    @AppsAllowed(Approle.SERVICE)
    @DELETE
    @Path("/{appId}")
    public void remove(@PathParam("appId") String appId) throws KeyserverException {
        this.getKeyserverLogic().removeApp(appId);
    }

    @AppsAllowed({ Approle.SERVICE, Approle.WORKER, Approle.STORAGE, Approle.INDEXER })
    @POST
    @Path("/{appId}")
    public AuthResponseDTO authenticate(@PathParam("appId") String appId, @NotNull @FormParam("key") String appKey) throws KeyserverException {
        return this.map(this.getKeyserverLogic().authenticateApp(appId, appKey), AuthResponseDTO.class);
    }
}
