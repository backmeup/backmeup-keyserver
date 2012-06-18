package org.backmeup.keysrv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PGPKeys
{
	private final String PUBLIC_KEY_FILE = "/root/public.key";
	private final String PRIVATE_KEY_FILE = "/root/private.key";
	private final int MAX_FILE_SIZE = 1048576; // 1 MiB
	
	private String publickey = "";
	private String privatekey = "";

	public PGPKeys () throws IOException
	{
		publickey = this.readPGPFile (PUBLIC_KEY_FILE);
		privatekey = this.readPGPFile (PRIVATE_KEY_FILE);
	}
	
	public String getPrivatekey ()
	{
		return privatekey;
	}
	
	public String getPublickey ()
	{
		return publickey;
	}
	
	/**
	 * Reads an pgp Keyfile and returns its content as String.
	 * 
	 * @param pgpfilename
	 * @return
	 * @throws IOException
	 */
	private String readPGPFile (String pgpfilename) throws IOException
	{
		File pgpfile = new File (pgpfilename);
		
		if (pgpfile.exists () == false)
		{
			throw new FileNotFoundException ("File \"" + pgpfilename + "\" not found");
		}
		
		if (pgpfile.canRead () == false)
		{
			throw new IOException ("File \"" + pgpfilename + "\" not readable");
		}
		
		if ((pgpfile.length () > MAX_FILE_SIZE) || (pgpfile.length () > Integer.MAX_VALUE))
		{
			throw new IOException ("File \"" + pgpfilename + "\" is bigger than max allowed file size (" + MAX_FILE_SIZE + " bytes), or bigger than Integer.MAX_VALUE");
		}
		
		// length is checked -> cast to int should be ok
		byte[] data = new byte[(int) pgpfile.length ()];
		
		FileInputStream fis = new FileInputStream (pgpfile);
		fis.read (data);
		
		return new String (data);
	}
}
