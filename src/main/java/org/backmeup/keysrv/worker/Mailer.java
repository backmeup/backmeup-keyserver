package org.backmeup.keysrv.worker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Mailer {
	private static ExecutorService service;
	private static Properties mailSettings;
	
	static {
		service = Executors.newFixedThreadPool(4);
	}
	
	private Mailer () {
	}

	public static void sendAdminMail(final String subject, final String text) {
		getMailSettings();
		send(mailSettings.getProperty("mail.admin.address"), subject, text);
	}

	public static void send(final String to, final String subject,
			final String text) {
		send(to, subject, text, "text/plain");
	}

	public static void synchronousSend(final String to, final String subject,
			final String text, final String mimeType) {
		executeSend(to, subject, text, mimeType);
	}

	private static void executeSend(final String to, final String subject,
			final String text, final String mimeType) {
		final Properties props = getMailSettings();
		try {
			// Get session
			Session session = Session.getDefaultInstance(props,
					new Authenticator() {
						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(props
									.getProperty("mail.user"), props
									.getProperty("mail.password"));
						}
					});
			// Define message
			MimeMessage message = new MimeMessage(session);

			message.setFrom(new InternetAddress(props.getProperty("mail.from")));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(
					to));
			message.setSubject(subject);
			message.setContent(text, mimeType);

			// Send message
			Transport.send(message);
		} catch (Exception e) {
			FileLogger.logException("send mail failed", e);
		}
	}

	public static void send(final String to, final String subject,
			final String text, final String mimeType) {
		// Get system properties
		service.submit(new Runnable() {
			public void run() {
				executeSend(to, subject, text, mimeType);
			}
		});
	}

	private static Properties getMailSettings() {
		synchronized (Properties.class) {
			if (mailSettings == null) {
				Properties props = new Properties();
				InputStream is = null;
				try {
					is = Mailer.class.getClassLoader().getResourceAsStream(
							"mail.properties");
					props.load(is);
					mailSettings = props;
				} catch (Exception e) {
					FileLogger.logException("reading mailettings failed", e);
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {
							FileLogger.logException("closing stream failed", e);
						}
					}
				}
			}
		}
		
		return mailSettings;
	}
}
