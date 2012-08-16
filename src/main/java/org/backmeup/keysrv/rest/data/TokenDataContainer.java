package org.backmeup.keysrv.rest.data;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

import org.backmeup.keysrv.worker.Token;
import org.codehaus.jackson.annotate.JsonIgnore;

@XmlRootElement
public class TokenDataContainer
{
	private UserContainer usercontainer;
	private ArrayList<ServiceContainer> servicecontainers;
	private ArrayList<AuthInfoContainer> authinfocontainers;
	
	
	public TokenDataContainer()
	{
		this.usercontainer = new UserContainer ();
		this.servicecontainers = new ArrayList<ServiceContainer> ();
		this.authinfocontainers = new ArrayList<AuthInfoContainer> ();
		
		this.servicecontainers.add (new ServiceContainer ());
		
		this.authinfocontainers.add (new AuthInfoContainer ());
	}
	
	public TokenDataContainer (Token token)
	{
		this.usercontainer = new UserContainer (token.getUser ());
		
		int aicount = token.getAuthInfoCount ();
		this.servicecontainers = new ArrayList<ServiceContainer> ();
		this.authinfocontainers = new ArrayList<AuthInfoContainer> ();
		
		for (int i = 0; i < aicount; i++)
		{
			this.servicecontainers.add (new ServiceContainer (token.getAuthInfo (i).getService ()));
			this.authinfocontainers.add (new AuthInfoContainer (token.getAuthInfo (i)));
		}
	}

	public UserContainer getUser ()
	{
		return usercontainer;
	}

	public void setUser (UserContainer usercontainer)
	{
		this.usercontainer = usercontainer;
	}

	@JsonIgnore(true)
	public ArrayList<ServiceContainer> getServices ()
	{
		return servicecontainers;
	}

	public void setServices (ArrayList<ServiceContainer> servicecontainers)
	{
		this.servicecontainers = servicecontainers;
	}

	public ArrayList<AuthInfoContainer> getAuthinfos ()
	{
		return authinfocontainers;
	}

	public void setAuthinfos (ArrayList<AuthInfoContainer> authinfocontainers)
	{
		this.authinfocontainers = authinfocontainers;
	}
}
