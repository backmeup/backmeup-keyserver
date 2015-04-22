package org.backmeup.keyserver.rest.provider;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.backmeup.keyserver.model.EntryNotFoundException;
import org.backmeup.keyserver.model.KeyserverException;

@Provider
public class KeyserverExceptionMapper implements ExceptionMapper<KeyserverException> {    
    public Response toResponse(KeyserverException exception) {
        if (exception instanceof EntryNotFoundException) {
            return Response.status(Status.NOT_FOUND).entity(exception).build();
        }
        if (exception.isCausedByCryptoException()) {
            return Response.status(Status.UNAUTHORIZED).entity(exception).build();
        }
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exception).build();
    }
}