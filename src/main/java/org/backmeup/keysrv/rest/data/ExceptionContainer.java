package org.backmeup.keysrv.rest.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ExceptionContainer {
	private String message = "";
	private String type = "";

	public ExceptionContainer() {

	}

	public ExceptionContainer(String type, String message) {
		this.type = type;
		this.message = message;
	}

	public ExceptionContainer(String type, Exception e) {
		this.type = type;
		this.message = e.getMessage();
	}

	public String getMessage() {
		return message;
	}

	public String getType() {
		return type;
	}
}
