package org.backmeup.keyserver.core;

import static org.junit.Assert.*;

import org.backmeup.keyserver.core.DefaultKeyserverImpl;
import org.backmeup.keyserver.core.KeyserverException;
import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultKeyserverImplTest {

	private static DefaultKeyserverImpl ks;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ks = new DefaultKeyserverImpl();
	}

	@Test
	public void testRegisterUser() throws DatabaseException, CryptoException, KeyserverException {
		String userId = ks.registerUser("wolfgang", "mypass");
		String userId2 = ks.getUserId("wolfgang");
		//assertEquals(userId, userId2);
	}

}
