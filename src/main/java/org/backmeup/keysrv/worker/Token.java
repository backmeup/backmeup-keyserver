package org.backmeup.keysrv.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.jboss.resteasy.util.Base64;

public class Token
{
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
		
		CipherGenerator cipher = new CipherGenerator ();
		return Base64.encodeBytes (cipher.encData (tokenstring, this.tokenpwd));
	}
	
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
	
	public static Token decodeToken (String tokenstr, String tokenpwd) throws TokenInvalidException
	{		
		User user = null;
		Token token = null;
		Service service = null;
		AuthInfo ai = null;
		CipherGenerator cipher = new CipherGenerator ();
		
		String enctoken = null;
		try
		{
			enctoken = cipher.decData (Base64.decode (tokenstr), tokenpwd);
		}
		catch (IOException e)
		{
			FileLogger.logException (e);
			e.printStackTrace();
		}
		
		String[] lines = enctoken.split ("\n");
		for (int i = 0; i < lines.length; i++)
		{
			if (lines[i].compareTo ("<tokeninfo>") == 0)
			{
				user = new User (new Long (lines[i + 1]), new Long (lines[i + 2]));
				user.setPwd (cipher.generatePassword ());
				i += 2;
				
				token = new Token (user, new Date (new Long (lines[i + 1])));
				i++;
			}
			if ((lines[i].compareTo ("<autinfo>") == 0))
			{
				service = new Service (new Long (lines[i + 1]), new Long (lines[i + 2]));
				i += 2;
				
				ai = new AuthInfo (new Long (lines[i + 1]), user, service, new Integer (lines[i + 2]));
				i += 2;
				
				if (ai.getAi_type () == AuthInfo.TYPE_PWD)
				{
					ai.setDecAi_username (lines[i + 1]);
					i++;

					ai.setDecAi_pwd (lines[i + 1]);
					i++;
				}
				else
				{
					ai.setDecAi_oauth (lines[i + 1]);
					i++;
				}
				
				token.addAuthInfo (ai);
			}
		}
		
		if (token.checkToken () == false)
		{
			String message = "Token is not valid anymore!\n";
			message += "User ID: " + user.getId () + "\n";
			message += "User BMU ID: " + user.getBmuId () + "\n";
			message += "Now: " + new Date () + "\n";
			message += "Valid from: " + new Date (token.getBackupdate ().getTime () - token.TIME_WINDOW) + "\n";
			message += "Valid to: " + new Date (token.getBackupdate ().getTime () + token.TIME_WINDOW) + "\n";
			
			FileLogger.logException (new TokenInvalidException (message));
			throw new TokenInvalidException (message);
		}
		
		return token;
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
