package org.backmeup.keysrv.rest.data;

import javax.xml.bind.annotation.XmlRootElement;

import org.backmeup.keysrv.worker.Token;

@XmlRootElement
public class TokenContainer
{
	private Long bmu_token_id;
	private String token;
	
	public TokenContainer()
	{
	}
	
	public TokenContainer (Token token)
	{
		this.bmu_token_id = token.getId ();
		this.token = token.getToken ();
	}
	
	public Long getBmu_token_id ()
	{
		return this.bmu_token_id;
	}
	
	public void setBmu_token_id (long bmu_token_id)
	{
		this.bmu_token_id = bmu_token_id;
	}
	
	public String getToken ()
	{
		return this.token;
	}
	
	public void setToken (String token)
	{
		this.token = token;
	}
}
