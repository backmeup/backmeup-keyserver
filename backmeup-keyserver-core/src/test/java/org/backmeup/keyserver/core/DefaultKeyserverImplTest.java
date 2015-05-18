package org.backmeup.keyserver.core;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.backmeup.keyserver.core.DefaultKeyserverImpl;
import org.backmeup.keyserver.core.config.Configuration;
import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.EntryNotFoundException;
import org.backmeup.keyserver.model.JsonKeys;
import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.keyserver.model.KeyserverUtils;
import org.backmeup.keyserver.model.Token;
import org.backmeup.keyserver.model.TokenValue;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultKeyserverImplTest {

    private static final String PASSWORD = "mypass";
    private static final String USERNAME = "keyserver-test";
    
    private static DefaultKeyserverImpl ks;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ks = new DefaultKeyserverImpl();
    }
   
    @Before
    public void setUp() throws Exception {
        try {
            AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);
            ks.removeUser(u.getServiceUserId(), u.getUsername(), u.getAccountKey());
        } catch(KeyserverException e) {
            ;
        }
    }
    
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (ks.db.isConnected()) {
            ks.db.disconnect();
            assertFalse(ks.db.isConnected());
        }
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
        assertNotNull(u.getB64Token());
    }
    
    @Test
    public void testAuthenticateUserWithPasswordFail() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        
        try {
            ks.authenticateUserWithPassword(USERNAME, "xxx");
        } catch(KeyserverException e) {
            assertTrue(e.getCause().getCause() instanceof javax.crypto.BadPaddingException);
        }
    }
    
    @Test
    public void testAuthenticateUserWithInternalToken() throws KeyserverException {
        String serviceUserId = ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);

        AuthResponse u2 = ks.authenticateWithInternalToken(u.getB64Token());
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
        assertEquals(ks.defaultProfile, profile);
        ks.setProfile(u.getUserId(), u.getAccountKey(), "{\"key\": \"Test\"}");
        profile = ks.getProfile(u.getUserId(), u.getAccountKey());
        assertEquals("{\"key\": \"Test\"}", profile);
    }
    
    @Test
    public void testIndexKey() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);

        String indexKey = ks.getIndexKey(u.getUserId(), u.getAccountKey());
        assertNotNull(indexKey);
        assertEquals(ks.activeKeyring.getPasswordLength(), indexKey.length());
    }
 
    @Test
    public void testRemoveUserWithLogin() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);
              
        ks.removeUser(u.getServiceUserId(), u.getUsername(), u.getAccountKey());
        
        try {
            ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.USERNAME, e.getMessage());
        }
        
        try {
            ks.authenticateWithInternalToken(u.getB64Token());
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN_USER_REMOVED, e.getMessage());
            
        }
    }
    
    @Test
    public void testRemoveUserWithLogin2() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        
        //fake an annotation which login tokens normally don't have
        //we need this to test the difference between removeUserWithLogin2 and testRemoveUserByAdmin
        //see below
        Token t = u.getToken();
        t.setAnnotation("Test");
        ks.tokenLogic.createAnnotaton(t, null, u.getAccountKey());
        
        ks.removeUser(u.getServiceUserId(), u.getUsername(), u.getAccountKey());
        
        try {
            ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.USERNAME, e.getMessage());
        }
        
        try {
            ks.authenticateWithInternalToken(u.getB64Token());
        } catch(EntryNotFoundException e) {
            //token should be deleted
            assertEquals(EntryNotFoundException.TOKEN, e.getMessage());
            
        }
    }
    
    @Test
    public void testQuickReRegistration() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);    
        ks.removeUser(u.getServiceUserId(), u.getUsername(), u.getAccountKey());
        
        try {
            ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.USERNAME, e.getMessage());
        }
        
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u2 = ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        
        assertNotEquals(u.getServiceUserId(), u2.getServiceUserId());
        assertNotEquals(u.getUserId(), u2.getUserId());
    }
    
    @Test
    public void testRemoveUserByAdmin() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);

        //fake an annotation which login tokens normally don't have
        //we need this to test the difference between removeUserWithLogin2 and testRemoveUserByAdmin
        //see below
        Token t = u.getToken();
        t.setAnnotation("Test");
        ks.tokenLogic.createAnnotaton(t, null, u.getAccountKey());
        
        ks.removeUser(u.getServiceUserId(), u.getUsername());
        
        try {
            ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.USERNAME, e.getMessage());
        }
        
        try {
            ks.authenticateWithInternalToken(u.getB64Token());
        } catch(EntryNotFoundException e) {
            //token still exists, but user not
            assertEquals(EntryNotFoundException.TOKEN_USER_REMOVED, e.getMessage());
        }
        
        //but now it should be gone
        try {
            ks.authenticateWithInternalToken(u.getB64Token());
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN, e.getMessage());
            
        }
    }
    
    @Test
    public void testChangeUserPassword() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);

        ks.changeUserPassword(u.getUserId(), USERNAME, PASSWORD, "test");
            
        try {
            ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        } catch(KeyserverException e) {
            assertTrue(e.getCause().getCause() instanceof javax.crypto.BadPaddingException);
        }
        
        AuthResponse u2 = ks.authenticateUserWithPassword(USERNAME, "test");
        assertEquals(u.getUsername(), u2.getUsername());
        assertEquals(u.getServiceUserId(), u2.getServiceUserId());
        ks.removeUser(u2.getServiceUserId(), u2.getUsername(), u2.getAccountKey());
    }
    
    //=========================================================================
    // PluginData logic
    //=========================================================================
    
    @Test
    public void testCreatePluginData() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        
        String userId = u.getUserId();
        String pluginId = "facebook1";
        String data = "json with oauth-token";

        ks.createPluginData(userId, pluginId, u.getAccountKey(), data);
        byte[] pluginKey = ks.getPluginDataKey(userId, pluginId, u.getAccountKey());
        String savedData = ks.getPluginData(userId, pluginId, pluginKey);
        assertEquals(data, savedData);
    }
    
    @Test
    public void testUpdatePluginData() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        
        String userId = u.getUserId();
        String pluginId = "facebook1";
        String data = "json with oauth-token";

        ks.createPluginData(userId, pluginId, u.getAccountKey(), data);
        byte[] pluginKey = ks.getPluginDataKey(userId, pluginId, u.getAccountKey());
        String savedData = ks.getPluginData(userId, pluginId, pluginKey);
        assertEquals(data, savedData);
        
        ks.updatePluginData(userId, pluginId, pluginKey, "xxx");
        savedData = ks.getPluginData(userId, pluginId, pluginKey);
        assertEquals("xxx", savedData);
    }
    
    @Test
    public void testRemovePluginData() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        
        String userId = u.getUserId();
        String pluginId = "facebook1";
        
        ks.createPluginData(userId, pluginId, u.getAccountKey(), "xxx");
        byte[] pluginKey = ks.getPluginDataKey(userId, pluginId, u.getAccountKey());
        
        ks.removePluginData(userId, pluginId);

        try {
            ks.getPluginDataKey(userId, pluginId, u.getAccountKey());
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.PLUGIN_KEY, e.getMessage());
            
        }
        
        try {
            ks.getPluginData(userId, pluginId, pluginKey);
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.PLUGIN, e.getMessage());
            
        }
    }

    //=========================================================================
    // Token logic
    //=========================================================================
    
    @Test
    public void testRevokeToken() throws KeyserverException {
        String serviceUserId = ks.registerUser("wolfgang2", PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword("wolfgang2", PASSWORD);
        assertEquals(serviceUserId, u.getServiceUserId());
        assertNotNull(u.getB64Token());

        ks.revokeToken(Token.Kind.INTERNAL, u.getB64Token());
        
        try {
            ks.authenticateWithInternalToken(u.getB64Token());
        } catch(EntryNotFoundException e) {
            assertTrue(e.getMessage().equals(EntryNotFoundException.TOKEN));
        }
        
        ks.removeUser(u.getServiceUserId(), u.getUsername(), u.getAccountKey());
    }
    
    private AuthResponse createUserWithPluginsAndOnetimeToken(Calendar time) throws KeyserverException {
        String serviceUserId = ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        String userId = u.getUserId();
        
        String[] pluginIds = {"facebook1", "dropbox1"};
        String data = "json with oauth-token";
        ks.createPluginData(userId, pluginIds[0], u.getAccountKey(), data);
        ks.createPluginData(userId, pluginIds[1], u.getAccountKey(), data);
        
        AuthResponse ot = ks.createOnetime(userId, serviceUserId, u.getUsername(), u.getAccountKey(), pluginIds, time);
        assertEquals(serviceUserId, ot.getServiceUserId());
        assertEquals(USERNAME, ot.getUsername());
        assertTrue(ot.getRoles().contains(TokenValue.Role.BACKUP_JOB));
        assertTrue(ot.getToken().getValue().getValueAsCalendar(JsonKeys.EARLIEST_START_TIME).before(time));
        assertTrue(ot.getToken().getValue().getValueAsCalendar(JsonKeys.LATEST_START_TIME).after(time));
        assertFalse(ot.hasNext());
        
        return ot;
    }
    
    @Test
    public void testOnetimeToken() throws KeyserverException {
        AuthResponse ot = this.createUserWithPluginsAndOnetimeToken(KeyserverUtils.getActTime());
        assertEquals(USERNAME, ot.getUsername());
        assertTrue(ot.getRoles().contains(TokenValue.Role.BACKUP_JOB));
        assertArrayEquals(new byte[0], ot.getAccountKey());
        
        AuthResponse it = ks.authenticateWithOnetime(ot.getB64Token());
        assertEquals(ot.getServiceUserId(), it.getServiceUserId());
        assertEquals(USERNAME, it.getUsername());
        assertTrue(it.getRoles().contains(TokenValue.Role.BACKUP_JOB));
        assertArrayEquals(new byte[0], it.getAccountKey());
        
        String[] pluginIds = {"facebook1", "dropbox1"};
        String data = "json with oauth-token";
        @SuppressWarnings("unchecked")
        Map<String, String> pluginKeys = (Map<String, String>) it.getToken().getValue().getValue(JsonKeys.PLUGIN_KEYS);
        for(String pluginId : pluginIds) {
            byte[] pluginKey = KeyserverUtils.fromBase64String(pluginKeys.get(pluginId));
            String pluginData = ks.getPluginData(it.getUserId(), pluginId, pluginKey);
            assertEquals(data, pluginData);
        }
    }
    
    @Test
    public void testOnetimeTokenTooEarly() throws KeyserverException {
        Calendar tomorrow = KeyserverUtils.getActTime();
        tomorrow.add(Calendar.DAY_OF_YEAR, +1);
        AuthResponse ot = this.createUserWithPluginsAndOnetimeToken(tomorrow);
        
        try {
           ks.authenticateWithOnetime(ot.getB64Token());
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN_USED_TO_EARLY, e.getMessage());
        }
    }
    
    @Test
    public void testOnetimeTokenTooLate() throws KeyserverException {
        Calendar yesterday = KeyserverUtils.getActTime();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        AuthResponse ot = this.createUserWithPluginsAndOnetimeToken(yesterday);
        
        try {
           ks.authenticateWithOnetime(ot.getB64Token());
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN, e.getMessage());
        }        
    }
    
    @Test
    public void testOnetimeTokenDoubleUsage() throws KeyserverException {
        AuthResponse ot = this.createUserWithPluginsAndOnetimeToken(KeyserverUtils.getActTime());
        
        ks.authenticateWithOnetime(ot.getB64Token());
        try {
           ks.authenticateWithOnetime(ot.getB64Token());
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN, e.getMessage());
        }
    }
    
    @Test
    public void testOnetimeTokenReschedule() throws KeyserverException {
        Calendar tomorrow = KeyserverUtils.getActTime();
        tomorrow.add(Calendar.DAY_OF_YEAR, +1);
        AuthResponse ot = this.createUserWithPluginsAndOnetimeToken(KeyserverUtils.getActTime());
        
        AuthResponse it = ks.authenticateWithOnetime(ot.getB64Token(), tomorrow);
        assertTrue(it.hasNext());
        AuthResponse next = it.getNext();
        assertEquals(ot.getServiceUserId(), next.getServiceUserId());
        assertEquals(USERNAME, next.getUsername());
        assertTrue(next.getRoles().contains(TokenValue.Role.BACKUP_JOB));
        assertTrue(next.getTtl().after(tomorrow));
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
        assertEquals(App.Approle.WORKER, u.getAppRole());
        assertEquals(App.Approle.WORKER, u2.getAppRole());
        ks.removeApp(u.getAppId());
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
        assertEquals(App.Approle.CORE, u.getAppRole());
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
        ks.removeApp(u.getAppId());
    }
    
    @Test
    public void testListApp() throws KeyserverException {
        App u = ks.registerApp(App.Approle.WORKER);
        App u2 = ks.registerApp(App.Approle.INDEXER);
        
        List<App> apps = ks.listApps(ks.servicePassword);
        Collections.sort(apps, new Comparator<App>() {
            @Override
            public int compare(App o1, App o2) {
                int c = o1.getAppRole().compareTo(o2.getAppRole());
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
        assertEquals(App.Approle.CORE, a.getAppRole());
        
        a = apps.get(1);
        assertEquals(u.getAppId(), a.getAppId());
        assertNull(a.getPassword());
        assertEquals(App.Approle.WORKER, a.getAppRole());
        
        a = apps.get(2);
        assertEquals(u2.getAppId(), a.getAppId());
        assertNull(a.getPassword());
        assertEquals(App.Approle.INDEXER, a.getAppRole());
        
        ks.removeApp(u.getAppId());
        ks.removeApp(u2.getAppId());
    }

    @Test
    public void testRemoveApp() throws KeyserverException {
        App u = ks.registerApp(App.Approle.WORKER);
        ks.removeApp(u.getAppId());
        try {
            ks.authenticateApp(u.getAppId(), u.getPassword());
        } catch (EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.APP, e.getMessage());
        }
    }
}
