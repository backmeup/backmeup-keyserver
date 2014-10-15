package org.backmeup.keyserver.model;

/**
 * Application User will be used for applications that are allowed to connect to the keyserver
 * 
 * @author spawn
 *
 */
public class AppUser {
	
	public static enum Approle {
		Core,
		Worker,
		Storage,
		Indexer
	}
	
	private String appId;
    private String password;
    private Approle approle;
    
    public AppUser (String addId, String password, Approle approle) {
    	this.appId = appId;
    	this.password = password;
    	this.approle = approle;
    }

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Approle getApprole() {
		return approle;
	}

	public void setApprole(Approle approle) {
		this.approle = approle;
	}
    
    
}
