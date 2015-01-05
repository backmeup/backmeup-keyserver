package org.backmeup.keyserver.model.dto;

import java.util.Calendar;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import org.backmeup.keyserver.model.TokenValue.Role;

@XmlRootElement
@SuppressWarnings("unused")
public class AuthResponseDTO {
    private String serviceUserId;
    private String username;
    private Set<Role> roles;
    private String b64Token;    
    private Calendar ttl;
    private AuthResponseDTO next;
   
    public AuthResponseDTO() {
        
    }

    public String getServiceUserId() {
        return serviceUserId;
    }

    public void setServiceUserId(String serviceUserId) {
        this.serviceUserId = serviceUserId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public String getB64Token() {
        return b64Token;
    }

    public void setB64Token(String b64Token) {
        this.b64Token = b64Token;
    }

    public Calendar getTtl() {
        return ttl;
    }

    public void setTtl(Calendar ttl) {
        this.ttl = ttl;
    }

    public AuthResponseDTO getNext() {
        return next;
    }

    public void setNext(AuthResponseDTO next) {
        this.next = next;
    }
}
