package org.backmeup.keysrv.rest.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.backmeup.keysrv.rest.data.ExceptionContainer;

public class RestUserAlreadyExistException extends WebApplicationException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RestUserAlreadyExistException(long bmuUserId) {
		super(Response
				.status(400)
				.entity(new ExceptionContainer("RestUserAlreadyExistException",
						"User with bmu_user_id (" + bmuUserId
								+ ") already exists")).build());
	}
}
