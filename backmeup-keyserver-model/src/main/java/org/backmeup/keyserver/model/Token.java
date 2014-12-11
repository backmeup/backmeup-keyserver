package org.backmeup.keyserver.model;

import java.util.Arrays;
import java.util.Calendar;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

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
    private String b64token;
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
        this.b64token = StringUtils.newStringUtf8(Base64.encodeBase64(token));
    }

    public Token(Kind kind, String b64Token) {
        this.kind = kind;
        this.b64token = b64Token;
        this.token = Base64.decodeBase64(StringUtils.getBytesUtf8(b64Token));
    }
    
    public Token(Token token) {
        this.kind = token.kind;
        this.b64token = token.b64token;
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
        this.b64token = StringUtils.newStringUtf8(Base64.encodeBase64(token));
    }

    public String getB64Token() {
        return b64token;
    }

    public void setB64Token(String b64Token) {
        this.b64token = b64Token;
        this.token = Base64.decodeBase64(StringUtils.getBytesUtf8(b64Token));
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
