package org.backmeup.keysrv.rest.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.backmeup.keysrv.rest.data.ExceptionContainer;

public class RestWrongDecryptionKeyException extends WebApplicationException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RestWrongDecryptionKeyException (long bmu_user_id)
	{
		super (Response.status (400).entity (new ExceptionContainer ("RestWrongDecryptionKeyException", "The password proviced for this user (" + bmu_user_id + ") is not correct")).build ());
	}
}
