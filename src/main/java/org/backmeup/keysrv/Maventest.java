package org.backmeup.keysrv;

import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Formatter;

public class Maventest
{
	public static void main (String[] args)
	{
		/*
		 * SecureRandom sr = new SecureRandom (); byte key[] = new byte[128];
		 * sr.nextBytes (key);
		 * 
		 * RSAKeyPairGenerator rsa = new RSAKeyPairGenerator (); rsa.initialize
		 * (2048, sr); KeyPair kp = rsa.generateKeyPair ();
		 * 
		 * String message = "Meine unverschlüsselte Nachricht";
		 * 
		 * 
		 * try { Cipher cipher = Cipher.getInstance ("AES/CBC/PKCS5Padding");
		 * System.out.println (Cipher.getMaxAllowedKeyLength
		 * ("AES/CBC/PKCS5Padding"));
		 * 
		 * 
		 * KeyGenerator aeskey = KeyGenerator.getInstance("AES");
		 * aeskey.init(256);
		 * 
		 * SecretKey s = aeskey.generateKey(); byte[] raw = s.getEncoded();
		 * 
		 * SecretKeySpec sskey= new SecretKeySpec(raw, "AES/CBC/PKCS5Padding");
		 * cipher.init (Cipher.ENCRYPT_MODE, s);
		 * 
		 * System.out.println("Original: " + message.getBytes("UTF-8")[0]);
		 * byte[] iv = cipher.getIV (); IvParameterSpec ips = new
		 * IvParameterSpec(iv);
		 * 
		 * byte[] encrypted = cipher.doFinal(message.getBytes("UTF-8"));
		 * System.out.println("encrypted string: " + encrypted[0]);
		 * 
		 * cipher.init (Cipher.DECRYPT_MODE, s, ips); byte[] decrypt =
		 * cipher.doFinal (encrypted);
		 * 
		 * System.out.println("decrypted string: " + decrypt[0]); } catch
		 * (NoSuchAlgorithmException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } catch (Exception e) { // TODO Auto-generated
		 * catch block e.printStackTrace(); }
		 */

		// Maventest tdb = new Maventest ();
		// tdb.testDB ();

		DBManager dbm = null;
		
		try
		{
			dbm = new DBManager ();

			
			User user = dbm.getUser ("irgendwer");
			System.out.println (user.getName ());
			
			//user = new User ("nower");
			//dbm.insertUser (user);
			Service service = new Service ("facebook");
			//dbm.insertService (service);
			
			service = dbm.getService ("facebook");
			System.out.println (service.getName ());
			System.out.println (service.getId ());
			
			
			user.setPwd ("geheim");
			AuthInfo.encData ("secret".getBytes (), user);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace ();
		}

	}

	private void testDB ()
	{
		try
		{
			Class.forName ("org.postgresql.Driver");
		}
		catch (ClassNotFoundException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace ();
		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		String url = "jdbc:postgresql://localhost/db_keysrv";
		String user = "dbu_keysrv";
		String password = "SAQ*$X2tX1bF.,%";

		PGPKeys pgpkeys = null;

		try
		{
			pgpkeys = new PGPKeys ();
		}
		catch (IOException e)
		{
			// TODO Auto-generatedunencrypted
			e.printStackTrace ();
		}

		try
		{
			con = DriverManager.getConnection (url, user, password);
			st = con.createStatement ();
			// rs =
			// st.executeQuery("INSERT INTO users (name) VALUES (pgp_pub_encrypt ('geheimer name', dearmor('"
			// + new String (puplickey) + "')))");
			// if (rs.next())
			// {
			// System.out.println(rs.getString(1));
			// }

			/*
			 * rs = st.executeQuery("SELECT name FROM users"); if (rs.next()) {
			 * System.out.println(rs.getString("name")); }
			 */
			rs = st.executeQuery ("SELECT pgp_pub_decrypt(name, dearmor('"
					+ pgpkeys.getPrivatekey () + "')) FROM users");
			if (rs.next ())
			{
				System.out.println (rs.getString (1));
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace ();
		}
	}
}
