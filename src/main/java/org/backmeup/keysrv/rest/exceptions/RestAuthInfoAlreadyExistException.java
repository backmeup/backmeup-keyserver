package org.backmeup.keysrv.rest.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.backmeup.keysrv.rest.data.ExceptionContainer;

public class RestAuthInfoAlreadyExistException extends WebApplicationException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RestAuthInfoAlreadyExistException(long bmuAuthinfoId) {
		super(Response
				.status(400)
				.entity(new ExceptionContainer(
						"RestAuthInfoAlreadyExistException",
						"Authinfo with bmu_authinfo_id (" + bmuAuthinfoId
								+ ") already exists")).build());
	}
}
