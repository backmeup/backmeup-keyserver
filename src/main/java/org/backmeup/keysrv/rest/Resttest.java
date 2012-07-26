package org.backmeup.keysrv.rest;

import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.backmeup.keysrv.rest.data.UserContainer;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.DBManager;
import org.backmeup.keysrv.worker.Service;
import org.backmeup.keysrv.worker.Token;
import org.backmeup.keysrv.worker.User;

@Path ("/message")
public class Resttest
{

	@GET
	@Path ("/{param}")
	@Produces("application/json")
	public UserContainer printMessage (@PathParam ("param") String msg)
	{
		String result = "Restful example : " + msg;
		
		/*
		try
		{
			DBManager dbm = new DBManager ();

			User user = dbm.getUser (123);
			
			Service service = dbm.getService (234);
			
			user.setPwd ("password");
			
			AuthInfo ai = new AuthInfo (user, service, AuthInfo.TYPE_PWD);
			
			ai.setDecAi_username ("irgendwer");
			ai.setDecAi_pwd ("geheimespwd");
			System.out.println (ai.getDecAiUsername ());
			
			
			Token token = new Token (user, new Date ());
			
			token.addAuthInfo (ai);
			
			ai = new AuthInfo (user, service, AuthInfo.TYPE_OAUTH);
			ai.setDecAi_oauth ("geheimer oauth");
			
			token.addAuthInfo (ai);
			
			String strtoken = token.getToken ();
			System.out.println (strtoken);
			System.out.println (token.toString ());
			System.out.println ();
			
			Token test = Token.decodeToken (strtoken, token.getTokenpwd ());
			System.out.println (test.toString ());
			
			//return Response.status (200).entity (test.toString ()).build ();
			return new UserContainer (user);
		}
		catch (Exception e)
		{
			return new UserContainer ();
			//return Response.status (200).entity (e.getMessage ()).build ();
		}
		*/
		
		return new UserContainer ();
		
		//return Response.status (200).entity (result).build ();
	}
}
