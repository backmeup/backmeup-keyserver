package org.backmeup.keyserver.dal;

import org.backmeup.keysrv.worker.Service;

public interface ServiceDao {
	void insertService(Service service);

	Service getService(long bmuServiceId);

	void deleteService(Service service);
}
