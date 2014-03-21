package org.backmeup.keysrv.worker;

import java.util.HashMap;

public class AuthInfo {
	private long id = -1;
	private long bmu_authinfo_id = -1;
	private User user = null;
	private Service service = null;

	private HashMap<byte[], byte[]> ai_data = null;

	private CipherGenerator cipher = null;

	public AuthInfo(long bmu_authinfo_id, User user, Service service) {
		cipher = new CipherGenerator();

		this.bmu_authinfo_id = bmu_authinfo_id;
		this.user = user;
		this.service = service;
		this.ai_data = new HashMap<byte[], byte[]>();
	}

	public AuthInfo(long id, long bmu_authinfo_id, User user, Service service,
			HashMap<byte[], byte[]> ai_data) {
		cipher = new CipherGenerator();

		this.id = id;
		this.bmu_authinfo_id = bmu_authinfo_id;
		this.user = user;
		this.service = service;
		this.ai_data = ai_data;
	}

	public long getId() {
		return id;
	}

	public long getBmuAuthinfoId() {
		return bmu_authinfo_id;
	}

	public User getUser() {
		return user;
	}

	public Service getService() {
		return service;
	}

	public void setDecAi_data(HashMap<String, String> ai_data) {
		this.ai_data = cipher.encData(ai_data, this.user);
	}

	public void setAi_data(HashMap<byte[], byte[]> ai_data) {
		this.ai_data = ai_data;
	}

	public HashMap<String, String> getDecAi_data() {
		return cipher.decData(this.ai_data, this.user);
	}

	public HashMap<byte[], byte[]> getAi_data() {
		return this.ai_data;
	}

	public void changePassword(String old_bmu_user_keyring_pwd,
			String new_bmu_user_keyring_pwd) {
		this.user.setPwd(old_bmu_user_keyring_pwd);
		HashMap<String, String> dec_ai_data = this.getDecAi_data();
		this.user.setPwd(new_bmu_user_keyring_pwd);
		this.setDecAi_data(dec_ai_data);
	}
}
