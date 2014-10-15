package org.backmeup.keyserver.rest.resources;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.backmeup.keyserver.model.AppUser;
import org.backmeup.keyserver.model.dto.AppDTO;

/**
 * All app specific operations will be handled within this class.
 */
@Path("/app")
public class App extends SecureBase {

    @RolesAllowed("core")
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AppDTO> listApps(// 
            @QueryParam("offset") int offset, //
            @QueryParam("limit") int limit) {
        List<AppDTO> userList = new ArrayList<>();
        userList.add(new AppDTO("pass01"));
        userList.add(new AppDTO("pass02"));
        return userList;
    }

    /*
    @PermitAll
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AppDTO addUser(AppDTO user) {
        AppUser userModel = getMapper().map(user, BackMeUpUser.class);
        userModel = getLogic().addUser(userModel);
        return getMapper().map(userModel, UserDTO.class);
    }
    */

    /*
    @RolesAllowed("user")
    @GET
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserDTO getUser(@PathParam("userId") Long userId) {
        canOnlyWorkWithMyData(userId);

        BackMeUpUser userModel = getLogic().getUserByUserId(userId);
        return getMapper().map(userModel, UserDTO.class);
    }

    @RolesAllowed("user")
    @PUT
    @Path("/{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public UserDTO updateUser(@PathParam("userId") Long userId, UserDTO user) {
        canOnlyWorkWithMyData(userId);

        BackMeUpUser userModel = getLogic().getUserByUserId(userId);
        userModel.setFirstname(user.getFirstname());
        userModel.setLastname(user.getLastname());
        userModel.setEmail(user.getEmail());

        BackMeUpUser newUser = getLogic().updateUser(userModel);
        return getMapper().map(newUser, UserDTO.class);
    }

    @RolesAllowed("user")
    @DELETE
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public void deleteUser(@PathParam("userId") Long userId) {
        canOnlyWorkWithMyData(userId);

        getLogic().deleteUser(userId);
    }
    */
}
