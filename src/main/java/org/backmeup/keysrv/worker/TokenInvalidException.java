package org.backmeup.keysrv.worker;

public class TokenInvalidException extends Exception
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TokenInvalidException ()
	{
		super ();
	}

	public TokenInvalidException (String message, Throwable cause)
	{
		super (message, cause);
	}

	public TokenInvalidException (String message)
	{
		super (message);
	}

	public TokenInvalidException (Throwable cause)
	{
		super (cause);
	}
}
