package org.backmeup.keyserver.model;

import java.util.Calendar;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import org.backmeup.keyserver.model.TokenValue.Role;

@XmlRootElement
public class AuthResponse {
    private Token token;

    @SuppressWarnings("unused")
    private AuthResponse() {
    }

    public AuthResponse(Token token) {
        this.token = token;
    }

    public String getServiceUserId() {
        return this.token.getValue().getServiceUserId();
    }

    public String getLoginToken() {
        return this.token.getB64Token();
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
