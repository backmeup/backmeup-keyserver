package org.backmeup.keyserver.rest.resources;

import java.util.Calendar;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.keyserver.model.Token;
import org.backmeup.keyserver.model.App.Approle;
import org.backmeup.keyserver.model.dto.AuthResponseDTO;
import org.backmeup.keyserver.rest.auth.AppsAllowed;

/**
 * All token specific operations (except onetime token creation and user token
 * listing) will be handled within this class.
 * @author wolfgang
 */
@Path("/tokens")
@Produces(MediaType.APPLICATION_JSON)
public class Tokens extends SecureBase {

    @AppsAllowed(Approle.SERVICE)
    @POST
    @Path("/INTERNAL/{token}")
    public AuthResponseDTO authenticateWithInternalToken(@PathParam("token") String tokenHash) throws KeyserverException {
        return this.map(this.getKeyserverLogic().authenticateWithInternalToken(tokenHash), AuthResponseDTO.class);
    }

    @AppsAllowed({ Approle.SERVICE, Approle.WORKER })
    @POST
    @Path("/ONETIME/{token}")
    public AuthResponseDTO authenticateWithOnetimeToken(@PathParam("token") String tokenHash, @FormParam("renew") boolean renew,
            @FormParam("nextScheduledExecutionTime") Long nextScheduledExecutionTime) throws KeyserverException {
        Calendar cal = null;
        if (nextScheduledExecutionTime != null) {
            cal = Calendar.getInstance();
            cal.setTimeInMillis((Long) nextScheduledExecutionTime);
        }

        return this.map(this.getKeyserverLogic().authenticateWithOnetime(tokenHash, renew, cal), AuthResponseDTO.class);
    }

    @AppsAllowed(Approle.SERVICE)
    @DELETE
    @Path("/{kind}/{token}")
    public void remove(@PathParam("kind") Token.Kind kind, @PathParam("token") String tokenHash) throws KeyserverException {
        this.getKeyserverLogic().revokeToken(kind, tokenHash);
    }
}
