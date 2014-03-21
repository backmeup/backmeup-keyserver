package org.backmeup.keysrv.rest.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.backmeup.keysrv.rest.data.ExceptionContainer;

public class RestServiceNotFoundException extends WebApplicationException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RestServiceNotFoundException(long bmu_service_id) {
		super(Response
				.status(400)
				.entity(new ExceptionContainer("RestServiceNotFoundException",
						"Service with bmu_service_id (" + bmu_service_id
								+ ") not found")).build());
	}
}
