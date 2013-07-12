package org.backmeup.keysrv.rest;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.backmeup.keyserver.dal.AuthInfoDao;
import org.backmeup.keyserver.dal.ServiceDao;
import org.backmeup.keyserver.dal.TokenDao;
import org.backmeup.keyserver.dal.UserDao;
import org.backmeup.keysrv.dal.postgres.impl.AuthInfoDaoImpl;
import org.backmeup.keysrv.dal.postgres.impl.ServiceDaoImpl;
import org.backmeup.keysrv.dal.postgres.impl.TokenDaoImpl;
import org.backmeup.keysrv.dal.postgres.impl.UserDaoImpl;
import org.backmeup.keysrv.rest.data.AuthInfoContainer;
import org.backmeup.keysrv.rest.data.TokenContainer;
import org.backmeup.keysrv.rest.data.TokenDataContainer;
import org.backmeup.keysrv.rest.data.TokenRequestContainer;
import org.backmeup.keysrv.rest.exceptions.RestServiceNotFoundException;
import org.backmeup.keysrv.rest.exceptions.RestTokenRequestNotValidException;
import org.backmeup.keysrv.worker.AuthInfo;
import org.backmeup.keysrv.worker.DBLogger;
import org.backmeup.keysrv.worker.DataManager;
import org.backmeup.keysrv.worker.FileLogger;
import org.backmeup.keysrv.worker.Mailer;
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
		UserDao userdao = DataManager.getUserDao ();
		ServiceDao servicedao = DataManager.getServiceDao ();
		AuthInfoDao authinfodoa = DataManager.getAuthInfoDao ();
		TokenDao tokendao = DataManager.getTokenDao ();

		if (trc.validRequest () == false)
		{
			FileLogger.logMessage ("Request not Valid");
			throw new RestTokenRequestNotValidException ();
		}

		User user = userdao.getUser (trc.getBmu_user_id ());
		user.setPwd (trc.getUser_pwd ());

		Token token = new Token (user, new Date (trc.getBackupdate ()), trc.isReusable ());

		for (int i = 0; i < trc.getBmu_service_ids ().length; i++)
		{
			Service service = servicedao.getService (trc.getBmu_service_ids ()[i]);
			token.addAuthInfo (authinfodoa.getAuthInfo (trc.getBmu_authinfo_ids ()[i], user, service));
		}
		
		// Store encryption password in an AuthInfo and in token
		if (trc.getEncryption_pwd () != null)
		{
			Service encryption_pwd_service = null;
			try
			{
				encryption_pwd_service = servicedao.getService (-2);
			}
			catch (RestServiceNotFoundException e)
			{
				encryption_pwd_service = new Service (-2);
				servicedao.insertService (encryption_pwd_service);
			}
			
			AuthInfo encpwd = new AuthInfo (-2, user, encryption_pwd_service);
			HashMap<String, String> encpwd_data = new HashMap<String, String> ();
			encpwd_data.put ("encryption_pwd", trc.getEncryption_pwd ());
			encpwd.setDecAi_data (encpwd_data);
			token.addAuthInfo (encpwd);
		}
		
		token.setId (-1);
		TokenContainer tokencontainer = new TokenContainer (token);
		
		tokencontainer.setBmu_token_id (tokendao.insertToken (token));
		
		token.setId (tokencontainer.getBmu_token_id ());
		DBLogger.logCreateToken (user, token);

		return tokencontainer;
	}

	@POST
	@Path ("/data")
	@Consumes ("application/json")
	@Produces ("application/json")
	public TokenDataContainer getTokenData (TokenContainer tc) throws WebApplicationException, SQLException, TokenInvalidException
	{
		TokenDao tokendao = DataManager.getTokenDao ();
		
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
		
		Token token = tokendao.getTokenData (tc.getBmu_token_id (), token_pwd);
		
		if (token.checkToken () == false)
		{
			Mailer.sendAdminMail ("Token exploited", "Token with id " + token.getId () + " was used under a not valid condition!");
			throw new RestTokenRequestNotValidException ();
		}
		
		DBLogger.logUseToken (token.getUser (), token);
		
		TokenDataContainer tdc = new TokenDataContainer (token);
		if ((tc.getBackupdate () != -1) && (token.isReusable () == true))
		{
			Token new_token = Token.genNewToken (token);
			
			token.setId (-1);
			TokenContainer tokencontainer = new TokenContainer (new_token);
			
			tokencontainer.setBmu_token_id (tokendao.insertToken (new_token));
			
			tdc.setNewToken (tokencontainer);
			
			new_token.setId (tokencontainer.getBmu_token_id ());
			DBLogger.logCreateToken (token.getUser (), new_token);
		}
		
		// get out encryption password
		for (AuthInfoContainer aic: tdc.getAuthinfos ())
		{
			if (aic.getBmu_authinfo_id () == -2)
			{
				tdc.setEncryption_pwd (aic.getAi_data ().get ("encryption_pwd"));
				tdc.getAuthinfos ().remove (aic);
				break;
			}
		}

		return tdc;
	}
}
