package org.backmeup.keyserver.model.dto;

import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;
import org.backmeup.keyserver.model.TokenValue.Role;

@XmlRootElement
@SuppressWarnings("unused")
public class AuthResponseDTO {
    private String serviceUserId;
    private String username;
    private Set<Role> roles;
    private TokenDTO token;
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

    public TokenDTO getToken() {
        return token;
    }

    public void setToken(TokenDTO token) {
        this.token = token;
    }

    public AuthResponseDTO getNext() {
        return next;
    }

    public void setNext(AuthResponseDTO next) {
        this.next = next;
    }
}
