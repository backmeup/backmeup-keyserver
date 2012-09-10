package org.backmeup.keyserver.dal;

import org.backmeup.keysrv.worker.Service;

public interface ServiceDao
{
	public void insertService (Service service);
	
	public Service getService (long bmu_service_id);
	
	public void deleteService (Service service);
}
