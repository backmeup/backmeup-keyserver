package org.backmeup.keysrv.worker;

import java.util.HashMap;
import java.util.Map;

public class AuthInfo {
	private long id = -1;
	private long bmuAuthinfoId = -1;
	private User user = null;
	private Service service = null;

	private Map<byte[], byte[]> aiData = null;

	private CipherGenerator cipher = null;

	public AuthInfo(long bmuAuthinfoId, User user, Service service) {
		cipher = new CipherGenerator();

		this.bmuAuthinfoId = bmuAuthinfoId;
		this.user = user;
		this.service = service;
		this.aiData = new HashMap<byte[], byte[]>();
	}

	public AuthInfo(long id, long bmuAuthinfoId, User user, Service service,
			Map<byte[], byte[]> aiData) {
		cipher = new CipherGenerator();

		this.id = id;
		this.bmuAuthinfoId = bmuAuthinfoId;
		this.user = user;
		this.service = service;
		this.aiData = aiData;
	}

	public long getId() {
		return id;
	}

	public long getBmuAuthinfoId() {
		return bmuAuthinfoId;
	}

	public User getUser() {
		return user;
	}

	public Service getService() {
		return service;
	}

	public void setDecAiData(Map<String, String> aiData) {
		this.aiData = cipher.encData(aiData, this.user);
	}

	public void setAiData(Map<byte[], byte[]> aiData) {
		this.aiData = aiData;
	}

	public Map<String, String> getDecAiData() {
		return cipher.decData(this.aiData, this.user);
	}

	public Map<byte[], byte[]> getAiData() {
		return this.aiData;
	}

	public void changePassword(String oldBmuUserKeyringPwd,
			String newBmuUserKeyringPwd) {
		this.user.setPwd(oldBmuUserKeyringPwd);
		Map<String, String> decAiData = this.getDecAiData();
		this.user.setPwd(newBmuUserKeyringPwd);
		this.setDecAiData(decAiData);
	}
}
