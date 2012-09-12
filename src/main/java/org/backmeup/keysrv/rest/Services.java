package org.backmeup.keysrv.rest;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.backmeup.keyserver.dal.ServiceDao;
import org.backmeup.keysrv.dal.postgres.impl.ServiceDaoImpl;
import org.backmeup.keysrv.rest.data.ServiceContainer;
import org.backmeup.keysrv.worker.Service;

@Path ("/services")
public class Services
{
	@GET
	@Path ("{bmu_service_id}")
	@Produces ("application/json")
	public ServiceContainer getService (@PathParam ("bmu_service_id") long bmu_service_id)
	{
		ServiceDao servicedao = new ServiceDaoImpl ();
		Service service = servicedao.getService (bmu_service_id);

		return new ServiceContainer (service);
	}

	@DELETE
	@Path ("{bmu_service_id}")
	@Produces ("application/json")
	public void deleteService (@PathParam ("bmu_service_id") long bmu_service_id)
	{
		ServiceDao servicedao = new ServiceDaoImpl ();
		Service service = new Service (bmu_service_id);
		
		servicedao.deleteService (service);
	}

	@POST
	@Path ("{bmu_service_id}/register")
	@Produces ("application/json")
	public void registerUser (@PathParam ("bmu_service_id") long bmu_service_id)
	{
		ServiceDao servicedao = new ServiceDaoImpl ();
		Service service = new Service (bmu_service_id);

		servicedao.insertService (service);
	}

}
