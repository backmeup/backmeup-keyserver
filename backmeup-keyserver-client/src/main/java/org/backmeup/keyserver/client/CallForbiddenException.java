package org.backmeup.keyserver.client;

import javax.ws.rs.core.Response;
import org.backmeup.keyserver.model.KeyserverException;

/**
 * Gets thrown if a rest call at keyserver responses with 
 * {@link javax.ws.rs.core.Response.Status#FORBIDDEN} or {@link javax.ws.rs.core.Response.Status#UNAUTHORIZED}.
 * @author wolfgang
 *
 */
public class CallForbiddenException extends KeyserverException {

    private static final long serialVersionUID = -3008690041437547327L;
    private Response.Status status;

    public CallForbiddenException(String message) {
        super(message);
    }

    public CallForbiddenException(Throwable cause) {
        super(cause);
    }

    public CallForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    public Response.Status getStatus() {
        return status;
    }

    public void setStatus(Response.Status status) {
        this.status = status;
    }

}
