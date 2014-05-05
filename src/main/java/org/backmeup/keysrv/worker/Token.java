package org.backmeup.keysrv.worker;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.backmeup.keyserver.config.Configuration;
import org.jboss.resteasy.util.Base64;

public class Token {
	private static final String ENCODING = Configuration.getProperty("keyserver.charset");

	// the time the token would be valid +/- in milliseconds. 600000 = 10
	// Minutes
	public static final long TIME_WINDOW_PREV = 600000;
	
	// 1 Day
	public static final long TIME_WINDOW_AFTER = 86400000;

	private long id = -1;
	private boolean extendable = false;
	private Date backupdate = null;
	private User user = null;
	private String tokenpwd = null;
	private List<AuthInfo> authInfos = null;
	private boolean reusable = false;

	public Token(User user, Date backupdate, boolean reusable) {
		authInfos = new ArrayList<AuthInfo>();

		CipherGenerator cipher = new CipherGenerator();
		tokenpwd = cipher.generatePassword();

		this.backupdate = new Date (backupdate.getTime());

		this.user = user;
		this.reusable = reusable;
	}

	public void renewTokenPwd() {
		CipherGenerator cipher = new CipherGenerator();
		String newTokenpwd = cipher.generatePassword();

		for (AuthInfo ai : authInfos) {
			ai.changePassword(tokenpwd, newTokenpwd);
		}

		this.tokenpwd = newTokenpwd;
	}

	public static Token genNewToken(Token token) {
		User user = new User(token.getUser().getId(), token.getUser()
				.getBmuId());
		Date backupdate = new Date(token.getBackupdate().getTime());
		Token newToken = new Token(user, backupdate, token.isReusable());
		user.setPwd(newToken.getTokenpwd());

		for (int i = 0; i < token.getAuthInfoCount(); i++) {
			AuthInfo ai = token.getAuthInfo(i);
			Service service = new Service(ai.getService().getId(), ai
					.getService().getBmuId());

			AuthInfo newAi = new AuthInfo(ai.getBmuAuthinfoId(), user, service);
			newAi.setDecAiData(ai.getDecAiData());
			newToken.addAuthInfo(newAi);
		}

		return newToken;
	}

	public boolean isReusable() {
		return reusable;
	}

	public void addAuthInfo(AuthInfo ai) {
		this.authInfos.add(ai);
	}

	public AuthInfo getAuthInfo(int index) {
		return authInfos.get(index);
	}

	public int getAuthInfoCount() {
		return authInfos.size();
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getToken() {
		try {
			return Base64.encodeBytes(this.tokenpwd.getBytes(ENCODING));
		} catch (UnsupportedEncodingException e) {
			// ignore would never come up
			FileLogger.logException("base64 encoding failed", e);
		}

		return null;
	}

	public void setEncTokenPwd(String encTokenpwd) {
		try {
			this.tokenpwd = new String(Base64.decode(encTokenpwd), ENCODING);
		} catch (Exception e) {
			// ignore -> should never come up
			FileLogger.logException("base64 decoding failed", e);
		}
	}

	/**
	 * @deprecated 
	 */
	@Deprecated
	public String toString() {
		
		StringBuilder tokenstring = new StringBuilder();

		tokenstring.append ("<token>\n");
		tokenstring.append ("<tokeninfo>\n");
		tokenstring.append (user.getId() + "\n");
		tokenstring.append (user.getBmuId() + "\n");
		tokenstring.append (backupdate.getTime() + "\n");
		tokenstring.append (extendable + "\n");

		tokenstring.append ("</tokeninfo>\n");
		for (int i = 0; i < authInfos.size(); i++) {
			tokenstring.append (authInfos.get(i).toString());
		}

		tokenstring.append ("</token>\n");

		return tokenstring.toString();
	}

	public String getTokenpwd() {
		return tokenpwd;
	}

	public void setTokenpwd(String tokenpwd) {
		this.tokenpwd = tokenpwd;
	}

	public boolean checkToken() {
		boolean valid = true;
		Date now = new Date();
		Date notValidBefore = new Date();
		Date notValidAfter = new Date();

		notValidBefore.setTime(this.backupdate.getTime()
				- TIME_WINDOW_PREV);
		notValidAfter.setTime(this.backupdate.getTime()
				+ TIME_WINDOW_AFTER);

		if (now.before(notValidBefore)) {
			valid = false;
		}

		if (now.after(notValidAfter)) {
			valid = false;
		}

		return valid;
	}

	public Date getBackupdate() {
		return new Date (this.backupdate.getTime());
	}

	public void setBackupdate(Date backupdate) {
		this.backupdate = new Date (backupdate.getTime());
	}

	public User getUser() {
		return this.user;
	}

	public long getId() {
		return this.id;
	}
}
