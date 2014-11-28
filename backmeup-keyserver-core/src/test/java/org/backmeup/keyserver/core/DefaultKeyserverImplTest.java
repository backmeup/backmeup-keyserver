package org.backmeup.keyserver.core;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.backmeup.keyserver.core.DefaultKeyserverImpl;
import org.backmeup.keyserver.core.KeyserverException;
import org.backmeup.keyserver.core.config.Configuration;
import org.backmeup.keyserver.core.db.sql.SQLDatabaseImpl;
import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.Token;
import org.backmeup.keyserver.model.TokenValue;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultKeyserverImplTest {

    private static final String PASSWORD = "mypass";
    private static final String USERNAME = "wolfgang";
    
    private static DefaultKeyserverImpl ks;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ks = new DefaultKeyserverImpl();
    }
    
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        ((SQLDatabaseImpl) ks.db).cleanup();
    }
    
    @Before
    public void setUp() throws Exception {
        ((SQLDatabaseImpl) ks.db).cleanup();
    }
    
    //=========================================================================
    // User logic
    //=========================================================================

    @Test
    public void testRegisterUser() throws KeyserverException {
        String serviceUserId = ks.registerUser(USERNAME, PASSWORD);
        assertNotNull(serviceUserId);
    }
    
    @Test
    public void testAuthenticateUserWithPassword() throws KeyserverException {
        String serviceUserId = ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        assertEquals(serviceUserId, u.getServiceUserId());
        assertEquals(USERNAME, u.getUsername());
        assertTrue(u.getRoles().contains(TokenValue.Role.USER));
        assertNotNull(u.getLoginToken());
    }
    
    @Test
    public void testAuthenticateUserWithInternalToken() throws KeyserverException {
        String serviceUserId = ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);

        AuthResponse u2 = ks.authenticateWithInternalToken(u.getLoginToken());
        assertEquals(serviceUserId, u2.getServiceUserId());
        assertEquals(u.getServiceUserId(), u2.getServiceUserId());
        assertEquals(u.getRoles(), u2.getRoles());
    }
    
    @Test
    public void testGetPubKKey() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);

        byte[] accountKey = u.getAccountKey();
        assertNotNull(accountKey);
        byte[] pubkKey = ks.userLogic.getPubKKey(u.getUserId(), accountKey);
        assertNotNull(pubkKey);
    }
    
    @Test
    public void testProfile() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);

        String profile = ks.getProfile(u.getUserId(), u.getAccountKey());
        assertEquals("", profile);
        ks.setProfile(u.getUserId(), u.getAccountKey(), "Test");
        profile = ks.getProfile(u.getUserId(), u.getAccountKey());
        assertEquals("Test", profile);
    }
    
    @Test
    public void testIndexKey() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);

        String indexKey = ks.getIndexKey(u.getUserId(), u.getAccountKey());
        assertNotNull(indexKey);
        assertEquals(64, indexKey.length());
    }

    //=========================================================================
    // Token logic
    //=========================================================================
    
    @Test
    public void testRevokeToken() throws KeyserverException {
        String serviceUserId = ks.registerUser("wolfgang2", PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword("wolfgang2", PASSWORD);
        assertEquals(serviceUserId, u.getServiceUserId());
        assertNotNull(u.getLoginToken());

        ks.revokeToken(Token.Kind.INTERNAL, u.getLoginToken());
        
        try {
            ks.authenticateWithInternalToken(u.getLoginToken());
        } catch(EntryNotFoundException e) {
            assertTrue(e.getMessage().equals(EntryNotFoundException.TOKEN));
        }
    }
    
    //=========================================================================
    // App logic
    //=========================================================================

    @Test
    public void testAuthenticateApp() throws KeyserverException {
        App u = ks.registerApp(App.Approle.WORKER);
        App u2 = ks.authenticateApp(u.getAppId(), u.getPassword());
        assertEquals(u.getAppId(), u2.getAppId());
        assertEquals(u.getPassword(), u2.getPassword());
        assertEquals(App.Approle.WORKER, u.getApprole());
        assertEquals(App.Approle.WORKER, u2.getApprole());
    }

    @Test
    public void testRegisterCoreApp() throws KeyserverException {
        try {
            ks.registerApp(App.Approle.CORE);
        } catch (KeyserverException e) {
            assertTrue(e.getMessage().contains("forbidden"));
        }
    }

    @Test
    public void testAuthenticateCoreApp() throws KeyserverException {
        String appId = Configuration.getProperty("backmeup.service.id");
        String appKey = Configuration.getProperty("backmeup.service.password");
        App u = ks.authenticateApp(appId, appKey);
        assertEquals(u.getAppId(), appId);
        assertEquals(u.getPassword(), appKey);
        assertEquals(App.Approle.CORE, u.getApprole());
    }

    @Test
    public void testAuthenticateNonExistantApp() throws KeyserverException {
        try {
            ks.authenticateApp("not here", "xxx");
        } catch (EntryNotFoundException e) {
            assertTrue(e.getMessage().equals(EntryNotFoundException.APP));
        }
    }

    @Test
    public void testAuthenticateAppFail() throws KeyserverException {
        App u = ks.registerApp(App.Approle.WORKER);
        try {
            ks.authenticateApp(u.getAppId(), "xxx");
        } catch (KeyserverException e) {
            assertTrue(e.getCause().getCause() instanceof javax.crypto.BadPaddingException);
        }
    }
    
    @Test
    public void testListApp() throws KeyserverException {
        App u = ks.registerApp(App.Approle.WORKER);
        App u2 = ks.registerApp(App.Approle.INDEXER);
        
        List<App> apps = ks.listApps(ks.servicePassword);
        Collections.sort(apps, new Comparator<App>() {
            @Override
            public int compare(App o1, App o2) {
                int c = o1.getApprole().compareTo(o2.getApprole());
                if (c != 0) {
                    return c;
                }
                return o1.getAppId().compareTo(o2.getAppId());
            }
        });;
        assertEquals(3, apps.size());
     
        App a = apps.get(0);
        assertEquals(ks.serviceId, a.getAppId());
        assertNull(a.getPassword());
        assertEquals(App.Approle.CORE, a.getApprole());
        
        a = apps.get(1);
        assertEquals(u.getAppId(), a.getAppId());
        assertNull(a.getPassword());
        assertEquals(App.Approle.WORKER, a.getApprole());
        
        a = apps.get(2);
        assertEquals(u2.getAppId(), a.getAppId());
        assertNull(a.getPassword());
        assertEquals(App.Approle.INDEXER, a.getApprole());
    }

    @Test
    public void testRemoveApp() throws KeyserverException {
        App u = ks.registerApp(App.Approle.WORKER);
        ks.removeApp(u.getAppId());
        try {
            ks.authenticateApp(u.getAppId(), u.getPassword());
        } catch (EntryNotFoundException e) {
            assertTrue(e.getMessage().equals(EntryNotFoundException.APP));
        }
    }
}
