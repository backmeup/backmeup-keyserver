package org.backmeup.keysrv.rest.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.backmeup.keysrv.rest.data.ExceptionContainer;

public class RestTokenRequestNotValidException extends WebApplicationException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RestTokenRequestNotValidException() {
		super(Response
				.status(400)
				.entity(new ExceptionContainer(
						"RestTokenRequestNotValidException",
						"The given request for an token is not valid")).build());
	}
}
