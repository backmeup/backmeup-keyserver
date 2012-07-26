package org.backmeup.keysrv.worker;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

/*
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
*/

public class Maventest
{
	/*
	private static void sendmail ()
	{
		Properties props = new Properties();
	    props.put("mail.smtp.host", "x-zimbra01.x");
	    props.put("mail.smtp.port", "25");
	    props.put("mail.from", "office@bgtech.at");
	    Session session = Session.getInstance(props, null);

	    try {
	        MimeMessage msg = new MimeMessage(session);
	        msg.setFrom();
	        msg.setRecipients(Message.RecipientType.TO, "ft@x-net.at");
	        msg.setSubject("JavaMail hello world example");
	        msg.setSentDate(new Date());
	        msg.setText("Hello, world!\n");
	        Transport.send (msg);
	    } catch (MessagingException mex) {
	        System.out.println("send failed, exception: " + mex);
	    }
	}
	*/
	
	public static void main (String[] args)
	{
		//sendmail ();
		
		
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

			
			User user = dbm.getUser (123);
			//System.out.println (user.getName ());
			
			//user = new User ("nower");
			//dbm.insertUser (user);
			
			//dbm.insertService (service);
			
			Service service = dbm.getService (234);
			//System.out.println (service.getName ());
			//System.out.println (service.getId ());
			
			
			user.setPwd ("password");
			
			AuthInfo ai = new AuthInfo (345, user, service, AuthInfo.TYPE_PWD);
			
			ai.setDecAi_username ("irgendwer");
			ai.setDecAi_pwd ("geheimespwd");
			System.out.println (ai.getDecAiUsername ());
			
			
			Token token = new Token (user, new Date (new Long ("1341575901000")));
			
			token.addAuthInfo (ai);
			
			ai = new AuthInfo (346, user, service, AuthInfo.TYPE_OAUTH);
			ai.setDecAi_oauth ("geheimer oauth");
			
			token.addAuthInfo (ai);
			
			String strtoken = token.getToken ();
			System.out.println (strtoken);
			System.out.println (token.toString ());
			System.out.println ();
			
			Token test = Token.decodeToken (strtoken, token.getTokenpwd ());
			System.out.println (test.toString ());
			
			//dbm.insertAuthInfo (ai);
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
