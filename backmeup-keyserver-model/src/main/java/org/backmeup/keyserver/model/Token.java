package org.backmeup.keyserver.model;

import java.util.Calendar;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

public class Token {
	public static enum TokenKind { 
		INTERNAL, EXTERNAL, ONETIME;
	
		public String getApplication() {
			switch(this) {
				case INTERNAL:		return "InternalToken";
				case EXTERNAL:		return "ExternalToken";
				case ONETIME: 		return "OnetimeToken";
				default:			return null;
			}
		}
	};
	
	private TokenKind kind;
	private String b64token;
    private byte[] token;
    private String annotation;
    private Calendar ttl;
    
    private TokenValue value;
    
    public Token(TokenKind kind) {
    	this.kind = kind;
    }

	public Token(TokenKind kind, byte[] token) {
		this.kind = kind;
    	this.setToken(token);
    }
	
	public Token(TokenKind kind, String b64Token) {
		this.kind = kind;
    	this.setB64Token(b64Token);
    }

	public byte[] getToken() {
		return token;
	}
	
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
	
    public Calendar getTTL() {
		return ttl;
	}

	public void setTTL(Calendar ttl) {
		this.ttl = ttl;
	}

	public TokenKind getKind() {
		return kind;
	}

	public void setKind(TokenKind kind) {
		this.kind = kind;
	}

	public TokenValue getValue() {
		return value;
	}

	public void setValue(TokenValue value) {
		this.value = value;
	}
}
