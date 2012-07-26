package org.backmeup.keysrv.worker;

import java.security.MessageDigest;

public class HashGenerator
{
	private final String ENCODING = "UTF-8";
	private final String HASH_ALGO = "SHA-512";

	public HashGenerator ()
	{
	}
	
	/**
	 * Returns an sha512 hash (hex formated) of an given string value (UTF-8)
	 * 
	 * @param value
	 * @return
	 */
	public String calcSHA512 (String value)
	{
		MessageDigest md = null;
		byte[] hash = null;
		
		try
		{
			md = MessageDigest.getInstance (HASH_ALGO);
			hash = md.digest (value.getBytes (ENCODING));
		}
		catch (Exception e)
		{
			FileLogger.logException (e);
			e.printStackTrace ();
		}
		
		StringBuffer sb = new StringBuffer ();
		for (int i = 0; i < hash.length; i++)
		{
			sb.append (Integer.toString ((hash[i] & 0xff) + 0x100, 16).substring (1));
		}
		
		return sb.toString ();
	}
}
