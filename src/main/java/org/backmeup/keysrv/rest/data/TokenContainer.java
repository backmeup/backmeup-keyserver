package org.backmeup.keysrv.rest.data;

import javax.xml.bind.annotation.XmlRootElement;

import org.backmeup.keysrv.worker.Token;
import org.codehaus.jackson.annotate.JsonIgnore;

@XmlRootElement
public class TokenContainer
{
	private long bmu_token_id;
	private String token;
	private long backupdate = -1;
	
	public TokenContainer()
	{
		this.bmu_token_id = -1;
		this.token = "";
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

	@JsonIgnore(true)
	public long getBackupdate ()
	{
		return backupdate;
	}

	public void setBackupdate (long backupdate)
	{
		this.backupdate = backupdate;
	}
}
