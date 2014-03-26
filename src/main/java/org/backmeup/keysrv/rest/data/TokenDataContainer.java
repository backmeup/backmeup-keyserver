package org.backmeup.keysrv.rest.data;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

import org.backmeup.keysrv.worker.Token;
import org.codehaus.jackson.annotate.JsonIgnore;

@XmlRootElement
public class TokenDataContainer {
	private UserContainer usercontainer;
	private ArrayList<ServiceContainer> servicecontainers;
	private ArrayList<AuthInfoContainer> authinfocontainers;
	private TokenContainer newtoken;
	private boolean ignore_new_token = true;
	private String encryption_pwd = null;

	public TokenDataContainer() {
		this.usercontainer = new UserContainer();
		this.servicecontainers = new ArrayList<ServiceContainer>();
		this.authinfocontainers = new ArrayList<AuthInfoContainer>();

		this.servicecontainers.add(new ServiceContainer());

		this.authinfocontainers.add(new AuthInfoContainer());

		this.newtoken = new TokenContainer();
	}

	public TokenDataContainer(Token token) {
		this.usercontainer = new UserContainer(token.getUser());

		int aicount = token.getAuthInfoCount();
		this.servicecontainers = new ArrayList<ServiceContainer>();
		this.authinfocontainers = new ArrayList<AuthInfoContainer>();

		for (int i = 0; i < aicount; i++) {
			this.servicecontainers.add(new ServiceContainer(token
					.getAuthInfo(i).getService()));
			this.authinfocontainers.add(new AuthInfoContainer(token
					.getAuthInfo(i)));
		}

		this.newtoken = new TokenContainer();
	}

	public UserContainer getUser() {
		return usercontainer;
	}

	public void setUser(UserContainer usercontainer) {
		this.usercontainer = usercontainer;
	}

	@JsonIgnore(true)
	public ArrayList<ServiceContainer> getServices() {
		return servicecontainers;
	}

	public void setServices(ArrayList<ServiceContainer> servicecontainers) {
		this.servicecontainers = servicecontainers;
	}

	public ArrayList<AuthInfoContainer> getAuthinfos() {
		return authinfocontainers;
	}

	public void setAuthinfos(ArrayList<AuthInfoContainer> authinfocontainers) {
		this.authinfocontainers = authinfocontainers;
	}

	public TokenContainer getNewToken() {
		return newtoken;
	}

	@JsonIgnore(true)
	public void setNewToken(TokenContainer tokencontainer) {
		this.newtoken = tokencontainer;
	}

	public String getEncryption_pwd() {
		return encryption_pwd;
	}

	public void setEncryption_pwd(String encryption_pwd) {
		this.encryption_pwd = encryption_pwd;
	}
}
