package org.backmeup.keysrv.rest.data;

import java.util.HashMap;
import java.util.Map;

import org.backmeup.keysrv.rest.exceptions.RestWrongDecryptionKeyException;
import org.backmeup.keysrv.worker.AuthInfo;
import org.codehaus.jackson.annotate.JsonIgnore;

public class AuthInfoContainer {
	private long authinfo_id = -1;
	private long bmu_authinfo_id = -1;
	private long bmu_user_id = -1;
	private long bmu_service_id = -1;

	private String user_pwd = "";

	Map<String, String> ai_data = null;

	public AuthInfoContainer() {
	}

	public AuthInfoContainer(AuthInfo ai) {
		this.authinfo_id = ai.getId();
		this.bmu_authinfo_id = ai.getBmuAuthinfoId();
		this.bmu_user_id = ai.getUser().getBmuId();
		this.bmu_service_id = ai.getService().getBmuId();
		this.ai_data = ai.getDecAiData();

		if (this.ai_data.isEmpty()) {
			throw new RestWrongDecryptionKeyException(this.bmu_user_id);
		}
	}

	@JsonIgnore(true)
	public long getAuthinfo_id() {
		return authinfo_id;
	}

	public void setAuthinfo_id(long authinfo_id) {
		this.authinfo_id = authinfo_id;
	}

	public long getBmu_authinfo_id() {
		return bmu_authinfo_id;
	}

	public void setBmu_authinfo_id(long bmu_authinfo_id) {
		this.bmu_authinfo_id = bmu_authinfo_id;
	}

	public long getBmu_user_id() {
		return bmu_user_id;
	}

	public void setBmu_user_id(long bmu_user_id) {
		this.bmu_user_id = bmu_user_id;
	}

	public long getBmu_service_id() {
		return bmu_service_id;
	}

	public void setBmu_service_id(long bmu_service_id) {
		this.bmu_service_id = bmu_service_id;
	}

	@JsonIgnore(true)
	public String getUser_pwd() {
		return user_pwd;
	}

	public void setUser_pwd(String user_pwd) {
		this.user_pwd = user_pwd;
	}

	public Map<String, String> getAi_data() {
		return ai_data;
	}

	public void setAi_data(HashMap<String, String> ai_data) {
		this.ai_data = ai_data;
	}
}
