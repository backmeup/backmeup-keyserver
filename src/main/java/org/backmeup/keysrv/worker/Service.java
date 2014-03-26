package org.backmeup.keysrv.worker;

public class Service {
	private long id = -1;
	private long bmu_id = -1;

	public Service(long bmu_id) {
		this.bmu_id = bmu_id;
	}

	public Service(long id, long bmu_id) {
		this.id = id;
		this.bmu_id = bmu_id;
	}

	public long getId() {
		return id;
	}

	public long getBmuId() {
		return this.bmu_id;
	}
}