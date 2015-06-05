package org.backmeup.keyserver.rest.provider;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.backmeup.keyserver.model.EntryNotFoundException;
import org.backmeup.keyserver.model.KeyserverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class KeyserverExceptionMapper implements ExceptionMapper<KeyserverException> {    
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyserverExceptionMapper.class);
    
    public Response toResponse(KeyserverException exception) {
        LOGGER.error("KeyserverException at REST", exception);
        
        if (exception instanceof EntryNotFoundException) {
            return Response.status(Status.NOT_FOUND).entity(exception).build();
        }
        if (exception.isCausedByCryptoException()) {
            return Response.status(Status.UNAUTHORIZED).entity(exception).build();
        }
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exception).build();
    }
}