package org.backmeup.keysrv.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.backmeup.keysrv.rest.data.TokenContainer;
import org.backmeup.keysrv.rest.data.TokenDataContainer;
import org.backmeup.keysrv.rest.data.TokenRequestContainer;
import org.backmeup.keysrv.rest.exceptions.RestTokenRequestNotValidException;
import org.backmeup.keysrv.worker.CipherGenerator;
import org.backmeup.keysrv.worker.DBManager;
import org.backmeup.keysrv.worker.FileLogger;
import org.backmeup.keysrv.worker.HashGenerator;
import org.backmeup.keysrv.worker.Service;
import org.backmeup.keysrv.worker.Token;
import org.backmeup.keysrv.worker.TokenInvalidException;
import org.backmeup.keysrv.worker.User;
import org.jboss.resteasy.util.Base64;

@Path ("/tokens")
public class Tokens
{
	@POST
	@Path ("/token")
	@Consumes ("application/json")
	@Produces ("application/json")
	public TokenContainer getToken (TokenRequestContainer trc) throws SQLException
	{
		DBManager dbm = new DBManager ();

		if (trc.validRequest () == false)
		{
			FileLogger.logMessage ("Request not Valid");
			throw new RestTokenRequestNotValidException ();
		}

		User user = dbm.getUser (trc.getBmu_user_id ());
		user.setPwd (trc.getUser_pwd ());

		Token token = new Token (user, new Date (trc.getBackupdate ()), trc.isReusable ());

		for (int i = 0; i < trc.getBmu_service_ids ().length; i++)
		{
			Service service = dbm.getService (trc.getBmu_service_ids ()[i]);
			token.addAuthInfo (dbm.getAuthInfo (trc.getBmu_authinfo_ids ()[i], user, service));
		}

		token.setId (-1);
		TokenContainer tokencontainer = new TokenContainer (token);
		
		tokencontainer.setBmu_token_id (dbm.insertToken (token));

		return tokencontainer;
	}

	@POST
	@Path ("/data")
	@Consumes ("application/json")
	@Produces ("application/json")
	public TokenDataContainer getTokenData (TokenContainer tc) throws WebApplicationException, SQLException, TokenInvalidException
	{		
		DBManager dbm = new DBManager ();
		
		String token_pwd = "";
		
		try
		{
			token_pwd = new String (Base64.decode (tc.getToken ()), "UTF-8");
		}
		catch (Exception e)
		{
			// ignore -> should never come up
			FileLogger.logException (e);
		}
		
		Token token = dbm.getTokenData (tc.getBmu_token_id (), token_pwd);
		
		if (token.checkToken () == false)
		{
			throw new RestTokenRequestNotValidException ();
		}
		
		TokenDataContainer tdc = new TokenDataContainer (token);
		if ((tc.getBackupdate () != -1) && (token.isReusable () == true))
		{
			Token new_token = Token.genNewToken (token);
			
			token.setId (-1);
			TokenContainer tokencontainer = new TokenContainer (new_token);
			
			tokencontainer.setBmu_token_id (dbm.insertToken (new_token));
			
			tdc.setNewToken (tokencontainer);
		}

		return tdc;
	}
}
