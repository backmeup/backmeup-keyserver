package org.backmeup.keyserver.core;

public class KeyserverException extends Exception {	

	private static final long serialVersionUID = 3673890017686453729L;

	public KeyserverException(String message) {
		super(message);
	}
	
	public KeyserverException(Throwable cause) {
		super(cause);
	}
	
	public KeyserverException(String message, Throwable cause) {
		super(message, cause);
	}
}
