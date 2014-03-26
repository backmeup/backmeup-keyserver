package org.backmeup.keysrv.rest.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LogContainer {
	private long bmu_user_id;
	private long bmu_service_id;
	private long bmu_authinfo_id;
	private long bmu_token_id;
	private long date;
	private String type;
	private String message;

	public LogContainer(long bmu_user_id, long bmu_service_id,
			long bmu_authinfo_id, long bmu_token_id, long date, String type,
			String message) {
		this.bmu_user_id = bmu_user_id;
		this.bmu_service_id = bmu_service_id;
		this.bmu_authinfo_id = bmu_authinfo_id;
		this.bmu_token_id = bmu_token_id;
		this.date = date;
		this.type = type;
		this.message = message;
	}

	public LogContainer() {

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

	public long getBmu_authinfo_id() {
		return bmu_authinfo_id;
	}

	public void setBmu_authinfo_id(long bmu_authinfo_id) {
		this.bmu_authinfo_id = bmu_authinfo_id;
	}

	public long getBmu_token_id() {
		return bmu_token_id;
	}

	public void setBmu_token_id(long bmu_token_id) {
		this.bmu_token_id = bmu_token_id;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
