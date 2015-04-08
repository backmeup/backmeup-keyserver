package org.backmeup.keyserver.model;

import static org.backmeup.keyserver.model.KeyserverUtils.toBase64String;
import static org.backmeup.keyserver.model.KeyserverUtils.fromBase64String;

import java.util.Arrays;
import java.util.Calendar;

public class Token {
    public static enum Kind {
        INTERNAL, EXTERNAL, ONETIME;

        public String getApplication() {
            switch (this) {
            case INTERNAL:
                return "InternalToken";
            case EXTERNAL:
                return "ExternalToken";
            case ONETIME:
                return "OnetimeToken";
            default:
                return null;
            }
        }
    }

    private Kind kind;
    private String b64Token;
    private byte[] token;
    private String annotation;
    private Calendar ttl;
    private TokenValue value;

    public Token(Kind kind) {
        this.kind = kind;
    }

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "only used internally")
    public Token(Kind kind, byte[] token) {
        this.kind = kind;
        this.token = token;
        this.b64Token = toBase64String(token);
    }

    public Token(Kind kind, String b64Token) {
        this.kind = kind;
        this.b64Token = b64Token;
        this.token = fromBase64String(b64Token);
    }
    
    public Token(Token token) {
        this.kind = token.kind;
        this.b64Token = token.b64Token;
        this.token = Arrays.copyOf(token.token, token.token.length);
        this.annotation = token.annotation;
        if (token.ttl != null) {
            this.ttl = (Calendar) token.ttl.clone();
        }
        this.value = new TokenValue(token.value);
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "only used internally")
    public byte[] getToken() {
        return token;
    }

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "only used internally")
    public void setToken(byte[] token) {
        this.token = token;
        this.b64Token = toBase64String(token);
    }

    public String getB64Token() {
        return b64Token;
    }

    public void setB64Token(String b64Token) {
        this.b64Token = b64Token;
        this.token = fromBase64String(b64Token);
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public boolean isAnnotated() {
        return this.annotation != null;
    }

    public Calendar getTTL() {
        return ttl;
    }

    public void setTTL(Calendar ttl) {
        this.ttl = ttl;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public TokenValue getValue() {
        return value;
    }

    public void setValue(TokenValue value) {
        this.value = value;
    }

    public boolean hasValue() {
        return this.value != null;
    }
}
