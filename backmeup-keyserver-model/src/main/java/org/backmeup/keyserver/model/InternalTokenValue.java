package org.backmeup.keyserver.model;



public class InternalTokenValue extends TokenValue {
    private String username;
    private byte[] accountKey;

    public InternalTokenValue() {

    }
    
    public InternalTokenValue(String userId, String serviceUserId, Role role, String username, byte[] accountKey) {
		super(userId, serviceUserId, role);
		this.username = username;
		this.accountKey = accountKey;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public byte[] getAccountKey() {
		return accountKey;
	}

	public void setAccountKey(byte[] accountKey) {
		this.accountKey = accountKey;
	}
}
