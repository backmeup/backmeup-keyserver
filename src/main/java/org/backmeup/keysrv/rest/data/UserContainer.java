package org.backmeup.keysrv.rest.data;

import org.backmeup.keysrv.worker.User;
import org.codehaus.jackson.annotate.JsonIgnore;

public class UserContainer {
	private long user_id;
	private long bmu_user_id;

	public UserContainer() {
		this.user_id = -1;
		this.bmu_user_id = -1;
	}

	public UserContainer(User user) {
		this.user_id = user.getId();
		this.bmu_user_id = user.getBmuId();
	}

	@JsonIgnore(true)
	public Long getUser_id() {
		return this.user_id;
	}

	public long getBmu_user_id() {
		return this.bmu_user_id;
	}
}
