package org.backmeup.keyserver.model;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import org.backmeup.keyserver.model.TokenValue.Role;

@XmlRootElement
public class AuthResponse {
    private String serviceUserId;
    private String loginToken;
    private String username;
    private Set<Role> roles = new HashSet<>();
    private Calendar ttl;

    public AuthResponse() {

    }

    public AuthResponse(String serviceUserId, String loginToken, String username, Set<Role> roles, Calendar ttl) {
        this.serviceUserId = serviceUserId;
        this.loginToken = loginToken;
        this.setUsername(username);
        this.roles = roles;
        this.ttl = ttl;
    }
    
    public AuthResponse(Token token) {
        this.serviceUserId = token.getValue().getServiceUserId();
        this.loginToken = token.getB64Token();
        this.setUsername(token.getValue().getValueAsString(JsonKeys.USERNAME));
        this.roles = token.getValue().getRoles();
        this.ttl = token.getTTL();
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

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Calendar getTtl() {
        return ttl;
    }

    public void setTtl(Calendar ttl) {
        this.ttl = ttl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
