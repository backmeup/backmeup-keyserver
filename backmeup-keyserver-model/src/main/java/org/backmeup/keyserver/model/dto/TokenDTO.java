package org.backmeup.keyserver.model.dto;

import java.util.Calendar;
import javax.xml.bind.annotation.XmlRootElement;
import org.backmeup.keyserver.model.Token.Kind;

/**
 * DTO for {@link org.backmeup.keyserver.model.Token} objects.
 * @author wolfgang
 *
 */
@XmlRootElement
@SuppressWarnings("unused")
public class TokenDTO {
    private static final String SEPARATOR = ";";
    
    private Kind kind;
    private String b64Token;
    private String annotation;
    private Calendar ttl;

    public TokenDTO() {

    }
    
    public TokenDTO(Kind kind, String b64token) {
        this.kind = kind;
        this.b64Token = b64token;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public String getB64Token() {
        return b64Token;
    }

    public void setB64Token(String b64Token) {
        this.b64Token = b64Token;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public Calendar getTtl() {
        return ttl;
    }

    public void setTtl(Calendar ttl) {
        this.ttl = ttl;
    }
    
    /**
     * @return token string representation (e.g. INTERNAL;xnefOdjxCb1DzTc65HfQ-ZhaoLV0RAFflBTLbq_w4q0)
     */
    public String toTokenString() {
        return this.kind + SEPARATOR + this.b64Token;
    }
    
    /**
     * Parses a token string (e.g. INTERNAL;xnefOdjxCb1DzTc65HfQ-ZhaoLV0RAFflBTLbq_w4q0).
     * @see #toTokenString()
     * @param tokenString token string to parse.
     * @return
     */
    public static TokenDTO fromTokenString(String tokenString) {
        String[] parts = tokenString.split(SEPARATOR);
        if (parts.length != 2) {
            throw new IllegalArgumentException("token string must consist of <kind>"+SEPARATOR+"<token>");
        }
        
        return new TokenDTO(Kind.valueOf(parts[0]), parts[1]);
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(this.toTokenString());
        b.append(" (");
        b.append(String.format("%tF %<tR %<tz", ttl));
        if (this.annotation != null) {
            b.append(", ");
            b.append(this.annotation);
        }
        b.append(")");
        return b.toString();
    }
}
