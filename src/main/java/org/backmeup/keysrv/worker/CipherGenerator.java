package org.backmeup.keysrv.worker;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;



public class CipherGenerator
{
	public static final int TYPE_PWD = 1;
	public static final int TYPE_OAUTH = 2;
	
	private final String ENCODING = "UTF-8";
	private final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
	private final String PWD_ALGORITHM = "PBKDF2WithHmacSHA1";
	private final String KEY_SPEC = "AES";
	private final int NUM_PWD_ITERATIONS = 1038;
	private final int AES_KEY_LENGTH = 256;
	private final int SALT_LENGTH = 16;
	private final int IV_LENGTH = 16;
	

	public CipherGenerator ()
	{
		// TODO Auto-generated constructor stub
	}

	
	public byte[] encData (String data, User user)
	{
		return this.encData (data, user.getPwd ());
	}
	
	public String decData (byte[] encdata, User user)
	{
		return this.decData (encdata, user.getPwd ());
	}

	/**
	 * Encrypts the data with the given user password.
	 * 
	 * @param data
	 * @param pwd
	 * @return
	 */
	public byte[] encData (String data, String pwd)
	{	
		// Password Salt
		SecureRandom sr = new SecureRandom ();
		byte[] salt = new byte[SALT_LENGTH];
		sr.nextBytes (salt);

		byte[] iv = null;
		byte[] encdata = null;
		
		PBEKeySpec keyspec = new PBEKeySpec (pwd.toCharArray (), salt, NUM_PWD_ITERATIONS, AES_KEY_LENGTH);
	
		try
		{
			Cipher cipher = Cipher.getInstance (AES_ALGORITHM);
			
			SecretKeyFactory keyfactory = SecretKeyFactory.getInstance(PWD_ALGORITHM);
			
			PBEKey pbekey = (PBEKey) keyfactory.generateSecret(keyspec);
			SecretKey encKey = new SecretKeySpec(pbekey.getEncoded(), KEY_SPEC);
			
			cipher.init (Cipher.ENCRYPT_MODE, encKey);
			
			iv = cipher.getIV ();
				
			// encrypt data
			encdata = cipher.doFinal (data.getBytes (ENCODING));
		}
		catch (Exception e)
		{
			// would never come up
			FileLogger.logException (e);
			e.printStackTrace ();
		}
		
		// create bytearray for salt + iv + encrypted data
		byte[] finaldata = new byte[salt.length + iv.length + encdata.length];
		
		// copy salt + iv + encrypted data to final array
		System.arraycopy (salt, 0, finaldata, 0, salt.length);
		System.arraycopy (iv, 0, finaldata, salt.length, iv.length);
		System.arraycopy (encdata, 0, finaldata, salt.length + iv.length, encdata.length);
		
		return finaldata;
	}

	/**
	 * Decrypts the data with the given user password.
	 * 
	 * @param encdata
	 * @param pwd
	 * @return
	 */
	public String decData (byte[] encdata, String pwd)
	{
		byte[] salt = new byte[SALT_LENGTH];
		byte[] iv = new byte[IV_LENGTH];
		byte[] data = new byte[encdata.length - salt.length - iv.length];
		byte[] decdata = null;
		String finaldata = null;
		
		System.arraycopy (encdata, 0, salt, 0, salt.length);
		System.arraycopy (encdata, salt.length, iv, 0, iv.length);
		System.arraycopy (encdata, salt.length + iv.length, data, 0, data.length);
		
		PBEKeySpec keyspec = new PBEKeySpec (pwd.toCharArray (), salt, NUM_PWD_ITERATIONS, AES_KEY_LENGTH);
		
		try
		{
			Cipher cipher = Cipher.getInstance (AES_ALGORITHM);
			
			SecretKeyFactory keyfactory = SecretKeyFactory.getInstance(PWD_ALGORITHM);
			
			PBEKey pbekey = (PBEKey) keyfactory.generateSecret(keyspec);
			SecretKey encKey = new SecretKeySpec(pbekey.getEncoded(), KEY_SPEC);
			
			IvParameterSpec ips = new IvParameterSpec (iv);
			
			cipher.init (Cipher.DECRYPT_MODE, encKey, ips);
			
			iv = cipher.getIV ();
			
			// encrypt data
			decdata = cipher.doFinal (data);
			finaldata = new String (decdata, ENCODING);
		}
		catch (BadPaddingException badpadding)
		{
			// wrong key
			return null;
		}
		catch (Exception e)
		{
			// would never come up
			FileLogger.logException (e);
			e.printStackTrace ();
		}
		
		return finaldata;
	}
	
	public String generatePassword ()
	{
		SecureRandom sr =  new SecureRandom ();
		byte[] key = new byte[128];
		
		String pwd = "";

		// make sure that the string is at least 65 chars long
		while (pwd.length () < 65)
		{
			// transform the key to UTF8 format length is variable
			try
			{
				// generate an random 128 bit key 
				sr.nextBytes (key);
				pwd = new String (key, ENCODING);
			}
			catch (UnsupportedEncodingException e)
			{
				FileLogger.logException (e);
				e.printStackTrace();
			}
		}
		
		// create a fixed 64 chars long UTF8 String
		pwd = pwd.substring (0, 64);
		
		return pwd;
	}
}
