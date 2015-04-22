package org.backmeup.keyserver.model.dto;

import java.util.Calendar;
import javax.xml.bind.annotation.XmlRootElement;
import org.backmeup.keyserver.model.Token.Kind;

@XmlRootElement
@SuppressWarnings("unused")
public class TokenDTO {
    private Kind kind;
    private String b64Token;
    private String annotation;
    private Calendar ttl;

    public TokenDTO() {

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
}
