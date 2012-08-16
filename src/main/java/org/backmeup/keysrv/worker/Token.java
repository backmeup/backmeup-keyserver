package org.backmeup.keysrv.worker;

import java.util.ArrayList;
import java.util.Date;

import org.jboss.resteasy.util.Base64;

public class Token
{
	private static final String ENCODING = "UTF-8";

	// the time the token would be valid +/- in milliseconds. 600000 = 10 Minutes
	public final long TIME_WINDOW = 600000;
	
	private long id = -1;
	private boolean extendable = false;
	private Date backupdate = null;
	private User user = null;
	private String tokenpwd = null;
	private ArrayList<AuthInfo> auth_infos = null;
	
	public Token (User user, Date backupdate)
	{
		auth_infos = new ArrayList<AuthInfo> ();
		
		CipherGenerator cipher = new CipherGenerator ();
		tokenpwd = cipher.generatePassword ();
		
		this.backupdate = backupdate;
		
		this.user = user;
	}
	
	public void addAuthInfo (AuthInfo ai)
	{
		this.auth_infos.add (ai);
	}
	
	public AuthInfo getAuthInfo (int index)
	{
		return auth_infos.get (index);
	}
	
	public int getAuthInfoCount ()
	{
		return auth_infos.size ();
	}
	
	public void setId (long id)
	{
		this.id = id;
	}
	
	public String getToken ()
	{
		return Base64.encodeBytes (this.tokenpwd.getBytes ());
	}
	
	public void setEncTokenPwd (String enc_tokenpwd)
	{
		try
		{
			this.tokenpwd = new String (Base64.decode (enc_tokenpwd), ENCODING);
		}
		catch (Exception e)
		{
			// ignore -> should never come up
			FileLogger.logException (e);
		}
	}
	
	@Deprecated
	public String toString ()
	{
		String tokenstring = "";
		
		tokenstring += "<token>\n";
		tokenstring += "<tokeninfo>\n";
		tokenstring += user.getId () + "\n";
		tokenstring += user.getBmuId () + "\n";
		tokenstring += backupdate.getTime () + "\n";
		tokenstring += extendable + "\n";
		
		tokenstring += "</tokeninfo>\n";
		for (int i = 0; i < auth_infos.size (); i++)
		{
			tokenstring += auth_infos.get (i).toString ();
		}
		
		tokenstring += "</token>\n";
		
		return tokenstring;
	}
	
	public String getTokenpwd ()
	{
		return tokenpwd;
	}
	
	public void setTokenpwd (String tokenpwd)
	{
		this.tokenpwd = tokenpwd;
	}
	
	public boolean checkToken ()
	{
		boolean valid = true;
		Date now = new Date ();
		Date not_valid_before = new Date ();
		Date not_valid_after  = new Date ();
		
		not_valid_before.setTime (this.backupdate.getTime () - this.TIME_WINDOW);
		not_valid_after.setTime (this.backupdate.getTime () + this.TIME_WINDOW);
		
		if (now.before (not_valid_before) == true)
		{
			valid = false;
		}
		
		if (now.after (not_valid_after) == true)
		{
			valid = false;
		}
		
		return valid;
	}
	
	public Date getBackupdate ()
	{
		return this.backupdate;
	}
	
	public User getUser ()
	{
		return this.user;
	}
	
	public long getId ()
	{
		return this.id;
	}
}
