package org.backmeup.keysrv.rest;

import java.sql.SQLException;
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.backmeup.keysrv.rest.data.TokenContainer;
import org.backmeup.keysrv.rest.data.TokenDataContainer;
import org.backmeup.keysrv.rest.data.TokenRequestContainer;
import org.backmeup.keysrv.rest.exceptions.RestAuthInfoNotFoundException;
import org.backmeup.keysrv.rest.exceptions.RestServiceNotFoundException;
import org.backmeup.keysrv.rest.exceptions.RestTokenRequestNotValidException;
import org.backmeup.keysrv.rest.exceptions.RestUserNotFoundException;
import org.backmeup.keysrv.rest.exceptions.RestWrongDecryptionKeyException;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.DBManager;
import org.backmeup.keysrv.worker.FileLogger;
import org.backmeup.keysrv.worker.Service;
import org.backmeup.keysrv.worker.Token;
import org.backmeup.keysrv.worker.TokenInvalidException;
import org.backmeup.keysrv.worker.User;

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

		Token token = new Token (user, new Date (trc.getBackupdate ()));

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

		Token token = Token.decodeToken (tc.getToken (), dbm.getTokenPwd (tc.getBmu_token_id ()));

		return new TokenDataContainer (token);
	}
}
