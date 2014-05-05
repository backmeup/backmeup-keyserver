package org.backmeup.keysrv.rest.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.backmeup.keysrv.rest.data.ExceptionContainer;

public class RestServiceAlreadyExistException extends WebApplicationException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RestServiceAlreadyExistException(long bmuServiceId) {
		super(Response
				.status(400)
				.entity(new ExceptionContainer(
						"RestServiceAlreadyExistException",
						"Service with bmu_service_id (" + bmuServiceId
								+ ") already exists")).build());
	}
}
