package org.backmeup.keysrv.worker;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.backmeup.keysrv.rest.exceptions.RestWrongDecryptionKeyException;

public class AuthInfo
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
	
	private long id = -1;
	private long bmu_authinfo_id = -1;
	private User user = null;
	private Service service = null;
	
	private byte[] ai_username = null;
	private byte[] ai_pwd = null;
	private byte[] ai_oauth = null;
	private int ai_type = 0;
	
	private CipherGenerator cipher = null;

	public AuthInfo (long bmu_authinfo_id ,User user, Service service, int ai_type)
	{
		cipher = new CipherGenerator ();
		
		this.bmu_authinfo_id = bmu_authinfo_id;
		this.user = user;
		this.service = service;
		this.ai_type = ai_type;
	}
	
	public AuthInfo (long id, long bmu_authinfo_id, User user, Service service, int ai_type, byte[] ai_username, byte[] ai_pwd, byte[] ai_oauth)
	{
		cipher = new CipherGenerator ();
		
		this.id = id;
		this.bmu_authinfo_id = bmu_authinfo_id;
		this.user = user;
		this.service = service;
		this.ai_type = ai_type;
		
		if (this.ai_type == TYPE_PWD)
		{
			this.ai_username = ai_username;
			this.ai_pwd = ai_pwd;
			this.ai_oauth = null;
		}
		else
		{
			this.ai_oauth = ai_oauth;
			this.ai_username = null;
			this.ai_pwd = null;
		}
	}
	
	public long getId ()
	{
		return id;
	}
	
	public long getBmuAuthinfoId ()
	{
		return bmu_authinfo_id;
	}
	
	public User getUser ()
	{
		return user;
	}

	public Service getService ()
	{
		return service;
	}
	
	public int getAi_type ()
	{
		return ai_type;
	}

	/**
	 * Returns the encrypted AuthInfo Username
	 * 
	 * @return
	 */
	public byte[] getAi_username ()
	{
		return ai_username;
	}

	/**
	 * Returns the encrypted AuthInfo Password
	 * 
	 * @return
	 */
	public byte[] getAi_pwd ()
	{
		return ai_pwd;
	}

	/**
	 * Returns the encrypted AuthInfo OAuth Token
	 * 
	 * @return
	 */
	public byte[] getAi_oauth ()
	{
		return ai_oauth;
	}
	
	/**
	 * Returns the decrypted AuthInfo Username as String (UTF-8)
	 * 
	 * @return
	 */
	public String getDecAiUsername ()
	{
		String name = cipher.decData (this.ai_username, this.user);
		
		return name;
	}
	
	/**
	 * Returns the decrypted AuthInfo Password as String (UTF-8)
	 * 
	 * @return
	 */
	public String getDecAiPwd ()
	{
		String pwd = cipher.decData (this.ai_pwd, this.user);
		
		return pwd;
	}
	
	/**
	 * Returns the decrypted AuthInfo OAuth Token as String (UTF-8)
	 * 
	 * @return
	 */
	public String getDecAiOAuth ()
	{
		String oauth = cipher.decData (this.ai_oauth, this.user);
		
		return oauth;
	}
	
	/**
	 * Sets the encrypted AuthInfo Username
	 * 
	 * @param ai_username
	 */
	public void setAi_username (byte[] ai_username)
	{
		this.ai_username = ai_username;
	}

	/**
	 * Sets the encrypted AuthInfo Password
	 * 
	 * @param ai_username
	 */
	public void setAi_pwd (byte[] ai_pwd)
	{
		this.ai_pwd = ai_pwd;
	}

	/**
	 * Sets the encrypted AuthInfo OAuth Token 
	 * 
	 * @param ai_username
	 */
	public void setAi_oauth (byte[] ai_oauth)
	{
		this.ai_oauth = ai_oauth;
	}
	
	/**
	 * Sets the AuthInfo Username in cleartext.
	 * The function encrypts the given data
	 * 
	 * @param ai_username
	 */
	public void setDecAi_username (String ai_username)
	{
		this.ai_username = cipher.encData (ai_username, this.user);
	}

	/**
	 * Sets the encrypted AuthInfo Password
	 * The function encrypts the given data
	 * 
	 * @param ai_username
	 */
	public void setDecAi_pwd (String ai_pwd)
	{
		this.ai_pwd = cipher.encData (ai_pwd, this.user);;
	}

	/**
	 * Sets the encrypted AuthInfo OAuth Token
	 * 
	 * 
	 * @param ai_username
	 */
	public void setDecAi_oauth (String ai_oauth)
	{
		this.ai_oauth = cipher.encData (ai_oauth, this.user);
	}
	
	/**
	 * Encrypts the data with the given user password.
	 * 
	 * @param data
	 * @param user
	 * @return
	 */
	private byte[] encData (String data, User user)
	{	
		// Password Salt
		SecureRandom sr = new SecureRandom ();
		byte[] salt = new byte[SALT_LENGTH];
		sr.nextBytes (salt);

		byte[] iv = null;
		byte[] encdata = null;
		
		PBEKeySpec keyspec = new PBEKeySpec (user.getPwd ().toCharArray (), salt, NUM_PWD_ITERATIONS, AES_KEY_LENGTH);
	
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
	 * @param user
	 * @return
	 */
	private String decData (byte[] encdata)
	{
		byte[] salt = new byte[SALT_LENGTH];
		byte[] iv = new byte[IV_LENGTH];
		byte[] data = new byte[encdata.length - salt.length - iv.length];
		byte[] decdata = null;
		String finaldata = null;
		
		System.arraycopy (encdata, 0, salt, 0, salt.length);
		System.arraycopy (encdata, salt.length, iv, 0, iv.length);
		System.arraycopy (encdata, salt.length + iv.length, data, 0, data.length);
		
		PBEKeySpec keyspec = new PBEKeySpec (this.user.getPwd ().toCharArray (), salt, NUM_PWD_ITERATIONS, AES_KEY_LENGTH);
		
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
		catch (Exception e)
		{
			// would never come up
			FileLogger.logException (e);
			e.printStackTrace ();
		}
		
		return finaldata;
	}
	
	public String toString ()
	{
		String helper = "<autinfo>\n";
		
		helper += service.getId () + "\n";
		helper += service.getBmuId () + "\n";
		helper += this.bmu_authinfo_id + "\n";
		helper += this.ai_type + "\n";
		
		if (this.ai_type == AuthInfo.TYPE_PWD)
		{
			if (this.getDecAiUsername () == null)
			{
				throw new RestWrongDecryptionKeyException (this.user.getBmuId ());
			}
			
			helper += this.getDecAiUsername () + "\n";
			helper += this.getDecAiPwd () + "\n";
		}
		else
		{
			if (this.getDecAiOAuth () == null)
			{
				throw new RestWrongDecryptionKeyException (this.user.getBmuId ());
			}
			
			helper += this.getDecAiOAuth () + "\n";
		}
		
		helper += "</authinfo>\n";
		
		return helper;
	}
}
