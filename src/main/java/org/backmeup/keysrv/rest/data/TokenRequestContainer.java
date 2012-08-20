package org.backmeup.keysrv.rest.data;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TokenRequestContainer
{
	private long bmu_user_id;
	private String user_pwd;
	private long[] bmu_service_ids;
	private long[] bmu_authinfo_ids;
	private long backupdate;
	private boolean reusable;

	public TokenRequestContainer (long bmu_user_id, String user_pwd, long[] bmu_service_ids, long[] bmu_authinfo_ids, long backupdate, boolean reusable)
	{
		this.bmu_user_id = bmu_user_id;
		this.user_pwd = user_pwd;
		this.bmu_service_ids = bmu_service_ids;
		this.bmu_authinfo_ids = bmu_authinfo_ids;
		this.backupdate = backupdate;
		this.reusable = reusable;
	}
	
	public TokenRequestContainer ()
	{
		
	}

	public long getBmu_user_id ()
	{
		return bmu_user_id;
	}
	
	public void setBmu_user_id (long bmu_user_id)
	{
		this.bmu_user_id = bmu_user_id;
	}

	public String getUser_pwd ()
	{
		return user_pwd;
	}

	public void setUser_pwd (String user_pwd)
	{
		this.user_pwd = user_pwd;
	}

	public long[] getBmu_service_ids ()
	{
		return bmu_service_ids;
	}

	public void setBmu_service_ids (long[] bmu_service_ids)
	{
		this.bmu_service_ids = bmu_service_ids;
	}

	public long[] getBmu_authinfo_ids ()
	{
		return bmu_authinfo_ids;
	}

	public void setBmu_authinfo_ids (long[] bmu_authinfo_ids)
	{
		this.bmu_authinfo_ids = bmu_authinfo_ids;
	}
	
	public long getBackupdate ()
	{
		return backupdate;
	}

	public void setBackupdate (Long backupdate)
	{
		this.backupdate = backupdate;
	}

	public boolean isReusable ()
	{
		return reusable;
	}

	public void setReusable (boolean reusable)
	{
		this.reusable = reusable;
	}

	public boolean validRequest ()
	{
		if (bmu_user_id <= 0)
		{
			return false;
		}
		
		if ((user_pwd == null) || (bmu_authinfo_ids == null) || (bmu_authinfo_ids == null))
		{
			return false;
		}
		
		if (backupdate <= (new Date ().getTime () - 600000))
		{
			return false;
		}
		
		if (this.bmu_authinfo_ids.length != this.bmu_service_ids.length)
		{
			return false;
		}
		
		return true;
	}
}
