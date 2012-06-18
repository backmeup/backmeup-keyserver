package org.backmeup.keysrv;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AuthInfo
{
	//public static enum AI_TYPE {USERPWD, OAUTH;}
	
	private long id = -1;
	private String name = null;
	private User user = null;
	private Service service = null;
	
	private byte[] ai_username = null;
	private byte[] ai_pwd = null;
	private byte[] ai_oauth = null;
	//private AI_TYPE ai_type = null;
	

	public AuthInfo (String name, User user, Service service)
	{
		// TODO Auto-generated constructor stub
	}
	
	public AuthInfo (long id, String name, User user, Service service)
	{
		// TODO Auto-generated constructor stub
	}
	
	public byte[] getAiUsername ()
	{
		byte[] data = null;
		
		return data;
	}
	
	public byte[] getAiPwd ()
	{
		byte[] data = null;
		
		return data;
	}
	
	public byte[] getAiOAuth ()
	{
		byte[] data = null;
		
		return data;
	}
	
	public static byte[] encData (byte[] data, User user) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException
	{
		byte[] encdata = null;
		System.out.println ("start");
		
		SecureRandom sr = new SecureRandom ();
		byte[] salt = new byte[128];
		sr.nextBytes (salt);
		System.out.println ("done");
		
		Cipher cipher = Cipher.getInstance ("AES/CBC/PKCS5Padding");
		PBEKeySpec pbe = new PBEKeySpec (user.getPwd ().toCharArray (), salt, 5, 256);
		
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");  
		PBEKey key = (PBEKey) factory.generateSecret(pbe);
		SecretKey encKey = new SecretKeySpec(key.getEncoded(), "AES");
		
		System.out.println ("done");
		
		return encdata;
	}
}
