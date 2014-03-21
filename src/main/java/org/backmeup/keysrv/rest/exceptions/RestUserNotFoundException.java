package org.backmeup.keysrv.rest.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.backmeup.keysrv.rest.data.ExceptionContainer;

public class RestUserNotFoundException extends WebApplicationException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RestUserNotFoundException(long bmu_user_id) {
		super(
				Response.status(400)
						.entity(new ExceptionContainer(
								"RestUserNotFoundException",
								"User with bmu_user_id (" + bmu_user_id
										+ ") not found")).build());
	}
}
