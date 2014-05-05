package org.backmeup.keysrv.rest.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.backmeup.keysrv.rest.data.ExceptionContainer;

public class RestTokenNotFoundException extends WebApplicationException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RestTokenNotFoundException(long bmuTokenId) {
		super(Response
				.status(400)
				.entity(new ExceptionContainer("RestTokenNotFoundException",
						"Token with bmu_token_id (" + bmuTokenId
								+ ") not found")).build());
	}
}
