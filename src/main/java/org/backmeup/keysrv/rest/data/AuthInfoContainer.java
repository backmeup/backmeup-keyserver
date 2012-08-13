package org.backmeup.keysrv.rest.data;

import java.util.HashMap;

import org.backmeup.keysrv.rest.exceptions.RestWrongDecryptionKeyException;
import org.backmeup.keysrv.worker.AuthInfo;
import org.codehaus.jackson.annotate.JsonIgnore;

public class AuthInfoContainer
{
	private long authinfo_id = -1;
	private long bmu_authinfo_id = -1;
	private long bmu_user_id = -1;
	private long bmu_service_id = -1;

	private String ai_username = "";
	private String ai_pwd = "";
	private String ai_oauth = "";
	private String ai_type = "";
	private String user_pwd = "";
	
	HashMap<String, String> ai_data = new HashMap<String, String> ();
	
	public AuthInfoContainer ()
	{
	}

	public AuthInfoContainer (AuthInfo ai)
	{
		this.authinfo_id = ai.getId ();
		this.bmu_authinfo_id = ai.getBmuAuthinfoId ();
		this.bmu_user_id = ai.getUser ().getBmuId ();
		this.bmu_service_id = ai.getService ().getBmuId ();

		if (ai.getAi_type () == AuthInfo.TYPE_PWD)
		{
			this.ai_type = "userpwd";
			this.ai_username = ai.getDecAiUsername ();
			this.ai_pwd = ai.getDecAiPwd ();
			this.ai_oauth = null;
		}
		else
		{
			this.ai_type = "oauth";
			this.ai_username = null;
			this.ai_pwd = null;
			this.ai_oauth = ai.getDecAiOAuth ();
		}
		
		if ((this.ai_username == null) && (this.ai_oauth == null))
		{
			throw new RestWrongDecryptionKeyException (this.bmu_user_id);
		}
		
		ai_data.put ("e-mail", "ft@x-net.at");
		ai_data.put ("server-ip", "127.0.0.1");
	}

	@JsonIgnore(true)
	public long getAuthinfo_id ()
	{
		return authinfo_id;
	}

	public void setAuthinfo_id (long authinfo_id)
	{
		this.authinfo_id = authinfo_id;
	}

	public long getBmu_authinfo_id ()
	{
		return bmu_authinfo_id;
	}

	public void setBmu_authinfo_id (long bmu_authinfo_id)
	{
		this.bmu_authinfo_id = bmu_authinfo_id;
	}

	public long getBmu_user_id ()
	{
		return bmu_user_id;
	}

	public void setBmu_user_id (long bmu_user_id)
	{
		this.bmu_user_id = bmu_user_id;
	}

	public long getBmu_service_id ()
	{
		return bmu_service_id;
	}

	public void setBmu_service_id (long bmu_service_id)
	{
		this.bmu_service_id = bmu_service_id;
	}

	public String getAi_username ()
	{
		return ai_username;
	}

	public void setAi_username (String ai_username)
	{
		this.ai_username = ai_username;
	}

	public String getAi_pwd ()
	{
		return ai_pwd;
	}

	public void setAi_pwd (String ai_pwd)
	{
		this.ai_pwd = ai_pwd;
	}

	public String getAi_oauth ()
	{
		return ai_oauth;
	}

	public void setAi_oauth (String ai_oauth)
	{
		this.ai_oauth = ai_oauth;
	}

	public String getAi_type ()
	{
		return ai_type;
	}

	public void setAi_type (String ai_type)
	{
		this.ai_type = ai_type;
	}

	@JsonIgnore(true)
	public String getUser_pwd ()
	{
		return user_pwd;
	}

	public void setUser_pwd (String user_pwd)
	{
		this.user_pwd = user_pwd;
	}

	public HashMap<String, String> getAi_data ()
	{
		return ai_data;
	}

	public void setAi_data (HashMap<String, String> ai_data)
	{
		this.ai_data = ai_data;
	}
}
