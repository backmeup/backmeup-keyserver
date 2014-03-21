package org.backmeup.keysrv.rest.exceptions;

import java.sql.SQLException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.backmeup.keysrv.rest.data.ExceptionContainer;

public class RestSQLException extends WebApplicationException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RestSQLException(SQLException e) {
		super(Response.status(400)
				.entity(new ExceptionContainer("RestSQLEXception", e)).build());
	}

}
