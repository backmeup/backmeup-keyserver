package org.backmeup.keyserver.model;

import java.util.Calendar;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import org.backmeup.keyserver.model.TokenValue.Role;

@XmlRootElement
public class AuthResponse {
    private Token token;
    private AuthResponse next;

    @SuppressWarnings("unused")
    private AuthResponse() {
    }

    public AuthResponse(Token token) {
        this.token = token;
    }
    
    public AuthResponse(Token token, AuthResponse next) {
        this.token = token;
        this.next = next;
    }

    public String getServiceUserId() {
        return this.token.getValue().getServiceUserId();
    }

    public String getB64Token() {
        return this.token.getB64Token();
    }
    
    public Token.Kind getTokenKind() {
        return this.token.getKind();
    }

    public Set<Role> getRoles() {
        return this.token.getValue().getRoles();
    }

    public Calendar getTtl() {
        return this.token.getTTL();
    }

    public String getUsername() {
        return this.token.getValue().getValueAsString(JsonKeys.USERNAME);
    }
    
    public boolean hasNext() {
        return this.next != null;
    }
    
    public AuthResponse getNext() {
        return this.next;
    }

    /*
     * Should only be used inside Keyserver!
     */
    public Token getToken() {
        return this.token;
    }

    /*
     * Should only be used inside Keyserver!
     */
    public void setToken(Token token) {
        this.token = token;
    }

    /*
     * Should only be used inside Keyserver!
     */
    public String getUserId() {
        return this.token.getValue().getUserId();
    }

    /*
     * Should only be used inside Keyserver!
     */
    public byte[] getAccountKey() {
        return this.token.getValue().getValueAsByteArray(JsonKeys.ACCOUNT_KEY);
    }
}
