package org.backmeup.keyserver.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AuthResponse {
    private String serviceUserId;
    private String loginToken;

    public AuthResponse() {

    }
    
    public AuthResponse(String serviceUserId, String loginToken) {
    	this.serviceUserId = serviceUserId;
    	this.loginToken = loginToken;
    }


	public String getServiceUserId() {
		return serviceUserId;
	}

	public void setServiceUserId(String serviceUserId) {
		this.serviceUserId = serviceUserId;
	}

	public String getLoginToken() {
		return loginToken;
	}

	public void setLoginToken(String loginToken) {
		this.loginToken = loginToken;
	}
}
