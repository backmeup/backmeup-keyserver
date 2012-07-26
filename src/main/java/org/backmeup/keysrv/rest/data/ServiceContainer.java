package org.backmeup.keysrv.rest.data;

import javax.xml.bind.annotation.XmlRootElement;

import org.backmeup.keysrv.worker.Service;
import org.codehaus.jackson.annotate.JsonIgnore;

@XmlRootElement
public class ServiceContainer
{
	private long service_id;
	private long bmu_service_id;
	
	public ServiceContainer ()
	{
		this.service_id = -1;
		this.bmu_service_id = -1;
	}
	
	public ServiceContainer (Service service)
	{
		this.service_id = service.getId ();
		this.bmu_service_id = service.getBmuId ();
	}
	
	@JsonIgnore(true)
	public Long getService_id ()
	{
		return this.service_id;
	}
	
	public long getBmu_service_id ()
	{
		return this.bmu_service_id;
	}
}
