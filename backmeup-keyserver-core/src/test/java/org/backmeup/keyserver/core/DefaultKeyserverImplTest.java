package org.backmeup.keyserver.core;

import static org.junit.Assert.*;

import org.backmeup.keyserver.core.DefaultKeyserverImpl;
import org.backmeup.keyserver.core.KeyserverException;
import org.backmeup.keyserver.core.config.Configuration;
import org.backmeup.keyserver.model.AppUser;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.Token;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultKeyserverImplTest {

	private static DefaultKeyserverImpl ks;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ks = new DefaultKeyserverImpl();
	}

	@Test
	public void testRegisterUser() throws KeyserverException {
		String serviceUserId = ks.registerUser("wolfgang", "mypass");
		AuthResponse u = ks.authenticateUserWithPassword("wolfgang", "mypass");
		assertEquals(serviceUserId, u.getServiceUserId());
		assertNotNull(u.getLoginToken());
		ks.tokenLogic.retrieveTokenValue(new Token(Token.TokenKind.INTERNAL, u.getLoginToken()));
	}
	
	@Test
	public void testAuthenticateApp() throws KeyserverException {
		AppUser u = ks.registerApp(AppUser.Approle.Worker);
		AppUser u2 = ks.authenticateApp(u.getAppId(), u.getPassword());
		assertEquals(u.getAppId(), u2.getAppId());
		assertEquals(u.getPassword(), u2.getPassword());
		assertEquals(AppUser.Approle.Worker, u.getApprole());
		assertEquals(AppUser.Approle.Worker, u2.getApprole());
	}
	
	@Test
	public void testRegisterCoreApp() throws KeyserverException {
		try {
			ks.registerApp(AppUser.Approle.Core);
		} catch(KeyserverException e) {
			assertTrue(e.getMessage().contains("forbidden"));
		}
	}
	
	@Test
	public void testAuthenticateCoreApp() throws KeyserverException {
		String appId = Configuration.getProperty("backmeup.service.id");
		String appKey = Configuration.getProperty("backmeup.service.password");
		AppUser u = ks.authenticateApp(appId, appKey);
		assertEquals(u.getAppId(), appId);
		assertEquals(u.getPassword(), appKey);
		assertEquals(AppUser.Approle.Core, u.getApprole());
	}
	
	@Test
	public void testAuthenticateNonExistantApp() throws KeyserverException {
		try {
			ks.authenticateApp("not here", "xxx");
		} catch(KeyserverException e) {
			assertTrue(e.getMessage().equals("appId not found"));
		}
	}
	
	@Test
	public void testAuthenticateAppFail() throws KeyserverException {
		AppUser u = ks.registerApp(AppUser.Approle.Worker);
		try {
			ks.authenticateApp(u.getAppId(), "xxx");
		} catch(KeyserverException e) {
			assertTrue(e.getCause().getCause() instanceof javax.crypto.BadPaddingException);
		}
	}
	
	@Test
	public void testRemoveApp() throws KeyserverException {
		AppUser u = ks.registerApp(AppUser.Approle.Worker);
		ks.removeApp(u.getAppId());
		try {
			ks.authenticateApp(u.getAppId(), u.getPassword());
		} catch(KeyserverException e) {
			assertTrue(e.getMessage().equals("appId not found"));
		}
	}
}
