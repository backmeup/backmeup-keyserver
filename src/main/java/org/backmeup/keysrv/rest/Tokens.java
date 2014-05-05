package org.backmeup.keysrv.rest;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.backmeup.keyserver.config.Configuration;
import org.backmeup.keyserver.dal.AuthInfoDao;
import org.backmeup.keyserver.dal.ServiceDao;
import org.backmeup.keyserver.dal.TokenDao;
import org.backmeup.keyserver.dal.UserDao;
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

@Path("/tokens")
public class Tokens {
	@POST
	@Path("/token")
	@Consumes("application/json")
	@Produces("application/json")
	public TokenContainer getToken(TokenRequestContainer trc) {
		UserDao userdao = DataManager.getUserDao();
		ServiceDao servicedao = DataManager.getServiceDao();
		AuthInfoDao authinfodoa = DataManager.getAuthInfoDao();
		TokenDao tokendao = DataManager.getTokenDao();

		if (!trc.validRequest()) {
			FileLogger.logMessage("Request not Valid");
			throw new RestTokenRequestNotValidException();
		}

		User user = userdao.getUser(trc.getBmu_user_id());
		user.setPwd(trc.getUser_pwd());

		Token token = new Token(user, new Date(trc.getBackupdate()),
				trc.isReusable());

		for (int i = 0; i < trc.getBmu_service_ids().length; i++) {
			Service service = servicedao
					.getService(trc.getBmu_service_ids()[i]);
			token.addAuthInfo(authinfodoa.getAuthInfo(
					trc.getBmu_authinfo_ids()[i], user, service));
		}

		// Store encryption password in an AuthInfo and in token
		if (trc.getEncryption_pwd() != null) {
			Service encryptionPwdService = null;
			try {
				encryptionPwdService = servicedao.getService(-2);
			} catch (RestServiceNotFoundException e) {
				encryptionPwdService = new Service(-2);
				servicedao.insertService(encryptionPwdService);
			}

			AuthInfo encpwd = new AuthInfo(-2, user, encryptionPwdService);
			Map<String, String> encpwd_data = new HashMap<String, String>();
			encpwd_data.put("encryption_pwd", trc.getEncryption_pwd());
			encpwd.setDecAiData(encpwd_data);
			token.addAuthInfo(encpwd);
		}

		token.setId(-1);
		TokenContainer tokencontainer = new TokenContainer(token);

		tokencontainer.setBmu_token_id(tokendao.insertToken(token));

		token.setId(tokencontainer.getBmu_token_id());
		DBLogger.logCreateToken(user, token);

		return tokencontainer;
	}

	@POST
	@Path("/data")
	@Consumes("application/json")
	@Produces("application/json")
	public TokenDataContainer getTokenData(TokenContainer tc) {
		TokenDao tokendao = DataManager.getTokenDao();

		String tokenPwd = "";

		try {
			tokenPwd = new String(Base64.decode(tc.getToken()), Configuration.getProperty("keyserver.charset"));
		} catch (Exception e) {
			// ignore -> should never come up
			FileLogger.logException("failed to decode password", e);
		}

		Token token = tokendao.getTokenData(tc.getBmu_token_id(), tokenPwd);

		if (!token.checkToken()) {
			Mailer.sendAdminMail("Token exploited",
					"Token with id " + token.getId()
							+ " was used under a not valid condition!");
			throw new RestTokenRequestNotValidException();
		}

		DBLogger.logUseToken(token.getUser(), token);

		TokenDataContainer tdc = new TokenDataContainer(token);
		if ((tc.getBackupdate() != -1) && (token.isReusable())) {
			Token newToken = Token.genNewToken(token);

			token.setId(-1);
			TokenContainer tokencontainer = new TokenContainer(newToken);

			tokencontainer.setBmu_token_id(tokendao.insertToken(newToken));

			tdc.setNewToken(tokencontainer);

			newToken.setId(tokencontainer.getBmu_token_id());
			DBLogger.logCreateToken(token.getUser(), newToken);
		}

		// get out encryption password
		for (AuthInfoContainer aic : tdc.getAuthinfos()) {
			if (aic.getBmu_authinfo_id() == -2) {
				tdc.setEncryption_pwd(aic.getAi_data().get("encryption_pwd"));
				tdc.getAuthinfos().remove(aic);
				break;
			}
		}

		return tdc;
	}
}
