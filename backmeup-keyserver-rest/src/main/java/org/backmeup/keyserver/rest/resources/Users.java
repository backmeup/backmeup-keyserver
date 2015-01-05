package org.backmeup.keyserver.rest.resources;

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
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.dto.AuthResponseDTO;
import org.backmeup.keyserver.rest.auth.TokenRequired;

/**
 * All user specific operations will be handled within this class.
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class Users extends SecureBase {

    public Users() {
        // TODO Auto-generated constructor stub
    }

    @RolesAllowed("CORE")
    @POST
    @Path("/")
    public String register(@NotNull @FormParam("username") String username, @NotNull @FormParam("password") String password) throws KeyserverException {
        return getKeyserverLogic().registerUser(username, password);
    }

    @RolesAllowed("CORE")
    @POST
    @Path("/authenticate/")
    public AuthResponseDTO authenticate(@NotNull @FormParam("username") String username, @NotNull @FormParam("password") String password) throws KeyserverException {
        return getMapper().map(this.getKeyserverLogic().authenticateUserWithPassword(username, password), AuthResponseDTO.class);
    }
    
    @RolesAllowed({"CORE", "INDEXER"})
    @TokenRequired
    @GET
    @Path("/index_key/")
    public String getIndexKey() throws KeyserverException {
        AuthResponse auth = this.getAuthResponse();
        return this.getKeyserverLogic().getIndexKey(auth.getUserId(), auth.getAccountKey());
    }
    
/*    @RolesAllowed("CORE")
    @GET
    @Path("/")
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
    @DELETE
    @Path("/{appId}/")
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
    public void authenticate(@PathParam("appId") String appId, @NotNull @FormParam("key") String appKey) {
        try {
            this.getKeyserverLogic().authenticateApp(appId, appKey);
        } catch(EntryNotFoundException e) {
            throw new WebApplicationException(e, Status.NOT_FOUND);
        } catch(KeyserverException e) {
            throw new WebApplicationException(e, Status.UNAUTHORIZED);
        }
    }
}*/

}
