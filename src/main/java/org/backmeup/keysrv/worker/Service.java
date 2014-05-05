package org.backmeup.keysrv.worker;

public class Service {
	private long id = -1;
	private long bmuId = -1;

	public Service(long bmuId) {
		this.bmuId = bmuId;
	}

	public Service(long id, long bmuId) {
		this.id = id;
		this.bmuId = bmuId;
	}

	public long getId() {
		return id;
	}

	public long getBmuId() {
		return this.bmuId;
	}
}