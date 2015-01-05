package org.backmeup.keyserver.rest.provider;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.backmeup.keyserver.core.EntryNotFoundException;
import org.backmeup.keyserver.core.KeyserverException;

@Provider
public class KeyserverExceptionMapper implements ExceptionMapper<KeyserverException> {
    
    private static final boolean PRINT_STACKTRACE = true;
    
    public Response toResponse(KeyserverException exception) {
        StringWriter sw = new StringWriter();
        
        if (PRINT_STACKTRACE) {
            exception.printStackTrace(new PrintWriter(sw));
        }
        else {
            sw.write(exception.toString());
        }
        
        if (exception instanceof EntryNotFoundException) {
            return Response.status(Status.NOT_FOUND).entity(sw.toString()).build();
        }
        if (exception.isCausedByCryptoException()) {
            return Response.status(Status.UNAUTHORIZED).entity(sw.toString()).build();
        }
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(sw.toString()).build();
    }
}