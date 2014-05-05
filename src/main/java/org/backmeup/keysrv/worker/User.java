package org.backmeup.keysrv.worker;

public class User {
	private long id = -1;
	private long bmuId = -1;
	private String pwd = null;
	private String pwdHash = null;

	public User(long bmuId) {
		this.bmuId = bmuId;
	}

	public User(long id, long bmuId) {
		this.id = id;
		this.bmuId = bmuId;
	}

	public long getId() {
		return id;
	}

	public long getBmuId() {
		return bmuId;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
		this.generateHash();
	}

	public String getPwd() {
		return pwd;
	}

	public String getPwdHash() {
		return pwdHash;
	}

	public void setPwdHash(String pwdHash) {
		this.pwdHash = pwdHash;
	}

	private void generateHash() {
		HashGenerator hasher = new HashGenerator();
		this.pwdHash = hasher.calcSaltedSHA512(this.pwd);
	}

	public boolean validatePwd(String pwd) {
		HashGenerator hasher = new HashGenerator();
		return hasher.isCorrectValue(pwd, this.pwdHash);
	}
}
