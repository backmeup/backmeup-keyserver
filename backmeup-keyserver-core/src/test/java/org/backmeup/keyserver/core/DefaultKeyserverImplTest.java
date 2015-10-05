package org.backmeup.keyserver.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.StringUtils;
import org.backmeup.keyserver.core.config.Configuration;
import org.backmeup.keyserver.crypto.AsymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.RSAEncryptionProvider;
import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.App.Approle;
import org.backmeup.keyserver.model.AuthResponse;
import org.backmeup.keyserver.model.CryptoException;
import org.backmeup.keyserver.model.EntryNotFoundException;
import org.backmeup.keyserver.model.JsonKeys;
import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.keyserver.model.KeyserverUtils;
import org.backmeup.keyserver.model.Token;
import org.backmeup.keyserver.model.TokenValue;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultKeyserverImplTest {

    private static final String PASSWORD = "mypass";
    private static final String USERNAME = "keyserver-test";
    private static String SERVICE_APPID = "backmeup-service";
    private static String SERVICE_SECRET = "REPLACE-SERVICE";
    
    private static DefaultKeyserverImpl ks;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ks = new DefaultKeyserverImpl();
        // load default apps
        List<App> apps = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            apps = mapper.readValue(Configuration.getProperty("backmeup.keyserver.defaultApps"), new TypeReference<List<App>>() {
            });
        } catch (IOException e) {
            throw new KeyserverException("cannot parse default app list from configuration", e);
        }

        // find and register core app
        for (App a : apps) {
            if (a.getAppRole().equals(Approle.SERVICE)) {
                SERVICE_APPID = a.getAppId();
                SERVICE_SECRET = a.getPassword();
                break;
            }
        }
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
            fail();
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
    public void testAlterIndexKey() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);

        String indexKey = ks.getIndexKey(u.getUserId(), u.getAccountKey());
        assertNotNull(indexKey);
        assertEquals(ks.activeKeyring.getPasswordLength(), indexKey.length());
        String newIndexKey = "myNewIndexKey";
        ks.setIndexKey(u.getUserId(), u.getAccountKey(), newIndexKey);
        String indexKey2 = ks.getIndexKey(u.getUserId(), u.getAccountKey());
        assertNotEquals(indexKey, indexKey2);
        assertEquals(newIndexKey, indexKey2);
    }
 
    @Test
    public void testRemoveUserWithLogin() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);
              
        ks.removeUser(u.getServiceUserId(), u.getUsername(), u.getAccountKey());
        
        try {
            ks.authenticateUserWithPassword(USERNAME, PASSWORD);
            fail();
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.USERNAME, e.getMessage());
        }
        
        try {
            ks.authenticateWithInternalToken(u.getB64Token());
            fail();
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
        ks.tokenLogic.createAnnotaton(t, null, u.getUserId(), u.getAccountKey());
        
        ks.removeUser(u.getServiceUserId(), u.getUsername(), u.getAccountKey());
        
        try {
            ks.authenticateUserWithPassword(USERNAME, PASSWORD);
            fail();
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.USERNAME, e.getMessage());
        }
        
        try {
            ks.authenticateWithInternalToken(u.getB64Token());
            fail();
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
            fail();
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
        ks.tokenLogic.createAnnotaton(t, null, u.getUserId(), u.getAccountKey());
        
        ks.removeUser(u.getServiceUserId(), u.getUsername());
        
        try {
            ks.authenticateUserWithPassword(USERNAME, PASSWORD);
            fail();
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.USERNAME, e.getMessage());
        }
        
        try {
            ks.authenticateWithInternalToken(u.getB64Token());
            fail();
        } catch(EntryNotFoundException e) {
            //token still exists, but user not
            assertEquals(EntryNotFoundException.TOKEN_USER_REMOVED, e.getMessage());
        }
        
        //but now it should be gone
        try {
            ks.authenticateWithInternalToken(u.getB64Token());
            fail();
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
            fail();
        } catch(KeyserverException e) {
            assertTrue(e.getCause().getCause() instanceof javax.crypto.BadPaddingException);
        }
        
        AuthResponse u2 = ks.authenticateUserWithPassword(USERNAME, "test");
        assertEquals(u.getUsername(), u2.getUsername());
        assertEquals(u.getServiceUserId(), u2.getServiceUserId());
        ks.removeUser(u2.getServiceUserId(), u2.getUsername(), u2.getAccountKey());
    }
    
    @Test
    public void testGetEncryptionKeys() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);

        byte[] accountKey = u.getAccountKey();
        assertNotNull(accountKey);
        byte[] pubKey = ks.userLogic.getPublicKey(u.getUserId());
        assertNotNull(pubKey);
        byte[] pubKey2 = ks.getPublicKeyByUsername(u.getUsername());
        assertNotNull(pubKey2);
        assertArrayEquals(pubKey, pubKey2);
        byte[] privKey = ks.userLogic.getPrivateKey(u.getUserId(), accountKey);
        assertNotNull(privKey);
        
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubKey));
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privKey));
            
            AsymmetricEncryptionProvider ep = new RSAEncryptionProvider();
            byte[] encrypted = ep.encrypt(publicKey, StringUtils.getBytesUtf8("mysecrettext"));
            String message = StringUtils.newStringUtf8(ep.decrypt(privateKey, encrypted));
            assertEquals("mysecrettext", message);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
    
    @Test
    public void testRegisterAnonymousUser() throws KeyserverException {
        ks.registerUser(USERNAME, PASSWORD);
        AuthResponse d = ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        
        AuthResponse u = ks.registerAnonymousUser(d.getServiceUserId(), d.getUserId(), d.getAccountKey());
        assertNotNull(u.getServiceUserId());
        assertNotNull(u.getUserId());
        assertNull(u.getUsername());
        assertTrue(u.getRoles().contains(TokenValue.Role.INHERITANCE));
        assertEquals(1, u.getRoles().size());
        assertNotNull(u.getB64Token());
        assertNull(u.getTtl());
        
        List<Token> l = ks.listTokens(d.getUserId(), d.getAccountKey(), Token.Kind.INTERNAL, false);
        assertEquals(1, l.size());
        assertEquals(l.get(0).getB64Token(), u.getB64Token());
        
        AuthResponse u2 = ks.authenticateWithInternalToken(u.getB64Token());
        assertNotEquals(u.getB64Token(), u2.getB64Token());
        assertEquals(u.getServiceUserId(), u2.getServiceUserId());
        assertEquals(u.getUserId(), u2.getUserId());
        
        assertTrue(u2.getRoles().contains(TokenValue.Role.INHERITANCE));
        assertTrue(u2.getRoles().contains(TokenValue.Role.USER));
        assertEquals(2, u2.getRoles().size());
        assertNotNull(u2.getTtl());
        
        ks.getPrivateKey(u2.getUserId(), u2.getAccountKey());
        ks.getPublicKey(u2.getUserId());
        ks.getIndexKey(u2.getUserId(), u2.getAccountKey());
        
        try {
            ks.getProfile(u2.getUserId(), u2.getAccountKey());
            fail();
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.PROFILE, e.getMessage());
        }
        
        ks.removeAnonymousUser(u.getServiceUserId(), u.getUserId(), u.getAccountKey());
        
        try {
            ks.authenticateWithInternalToken(u.getB64Token());
            fail();
        } catch(EntryNotFoundException e) {
            //token still exists, but user not
            assertEquals(EntryNotFoundException.TOKEN_USER_REMOVED, e.getMessage());
        }
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
            fail();
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.PLUGIN_KEY, e.getMessage());
            
        }
        
        try {
            ks.getPluginData(userId, pluginId, pluginKey);
            fail();
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
            fail();
        } catch(EntryNotFoundException e) {
            assertTrue(e.getMessage().equals(EntryNotFoundException.TOKEN));
        }
        
        ks.removeUser(u.getServiceUserId(), u.getUsername(), u.getAccountKey());
    }
    
    private AuthResponse createUserWithPluginsAndOnetimeTokenForBackup(Calendar time) throws KeyserverException {
        String serviceUserId = ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        String userId = u.getUserId();
        
        String[] pluginIds = {"facebook1", "dropbox1"};
        String data = "json with oauth-token";
        ks.createPluginData(userId, pluginIds[0], u.getAccountKey(), data);
        ks.createPluginData(userId, pluginIds[1], u.getAccountKey(), data);
        
        AuthResponse ot = ks.createOnetimeForBackup(userId, serviceUserId, u.getUsername(), u.getAccountKey(), pluginIds, time);
        assertEquals(serviceUserId, ot.getServiceUserId());
        assertEquals(USERNAME, ot.getUsername());
        assertTrue(ot.getRoles().contains(TokenValue.Role.BACKUP_JOB));
        assertTrue(ot.getToken().getValue().getValueAsCalendar(JsonKeys.EARLIEST_START_TIME).before(time));
        assertTrue(ot.getToken().getValue().getValueAsCalendar(JsonKeys.LATEST_START_TIME).after(time));
        assertFalse(ot.hasNext());
        
        return ot;
    }
    
    private AuthResponse createUserWithOnetimeTokenForAuthentication() throws KeyserverException {
        String serviceUserId = ks.registerUser(USERNAME, PASSWORD);
        AuthResponse u = ks.authenticateUserWithPassword(USERNAME, PASSWORD);
        String userId = u.getUserId();
        
        AuthResponse ot = ks.createOnetimeForAuthentication(userId, serviceUserId, u.getUsername(), u.getAccountKey());
        assertEquals(serviceUserId, ot.getServiceUserId());
        assertEquals(USERNAME, ot.getUsername());
        assertTrue(ot.getRoles().contains(TokenValue.Role.AUTHENTICATION));
        assertFalse(ot.hasNext());
        
        return ot;
    }
    
    @Test
    public void testOnetimeTokenForBackup() throws KeyserverException {
        AuthResponse ot = this.createUserWithPluginsAndOnetimeTokenForBackup(KeyserverUtils.getActTime());
        assertEquals(USERNAME, ot.getUsername());
        assertTrue(ot.getRoles().contains(TokenValue.Role.BACKUP_JOB));
        assertArrayEquals(new byte[0], ot.getAccountKey());
        
        AuthResponse it = ks.authenticateWithOnetime(ot.getB64Token(), false, null);
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
    public void testOnetimeTokenForBackupTooEarly() throws KeyserverException {
        Calendar tomorrow = KeyserverUtils.getActTime();
        tomorrow.add(Calendar.DAY_OF_YEAR, +1);
        AuthResponse ot = this.createUserWithPluginsAndOnetimeTokenForBackup(tomorrow);
        
        try {
           ks.authenticateWithOnetime(ot.getB64Token(), false, null);
           fail();
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN_USED_TO_EARLY, e.getMessage());
        }
    }
    
    @Test
    public void testOnetimeTokenForBackupTooLate() throws KeyserverException {
        Calendar yesterday = KeyserverUtils.getActTime();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        AuthResponse ot = this.createUserWithPluginsAndOnetimeTokenForBackup(yesterday);
        
        try {
           ks.authenticateWithOnetime(ot.getB64Token(), false, null);
           fail();
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN, e.getMessage());
        }        
    }
    
    @Test
    public void testOnetimeTokenForBackupDoubleUsage() throws KeyserverException {
        AuthResponse ot = this.createUserWithPluginsAndOnetimeTokenForBackup(KeyserverUtils.getActTime());
        
        ks.authenticateWithOnetime(ot.getB64Token(), false, null);
        try {
           ks.authenticateWithOnetime(ot.getB64Token(), false, null);
           fail();
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN, e.getMessage());
        }
    }
    
    @Test
    public void testOnetimeTokenForBackupReschedule() throws KeyserverException {
        Calendar tomorrow = KeyserverUtils.getActTime();
        tomorrow.add(Calendar.DAY_OF_YEAR, +1);
        AuthResponse ot = this.createUserWithPluginsAndOnetimeTokenForBackup(KeyserverUtils.getActTime());
        
        AuthResponse it = ks.authenticateWithOnetime(ot.getB64Token(), true, tomorrow);
        assertTrue(it.hasNext());
        AuthResponse next = it.getNext();
        assertEquals(ot.getServiceUserId(), next.getServiceUserId());
        assertEquals(USERNAME, next.getUsername());
        assertTrue(next.getRoles().contains(TokenValue.Role.BACKUP_JOB));
        assertTrue(next.getTtl().after(tomorrow));
    }
    
    @Test
    public void testOnetimeTokenForAuthentication() throws KeyserverException {
        AuthResponse ot = this.createUserWithOnetimeTokenForAuthentication();
        assertEquals(USERNAME, ot.getUsername());
        assertTrue(ot.getRoles().contains(TokenValue.Role.AUTHENTICATION));
        
        AuthResponse it = ks.authenticateWithOnetime(ot.getB64Token(), false, null);
        assertEquals(ot.getServiceUserId(), it.getServiceUserId());
        assertEquals(USERNAME, it.getUsername());
        assertTrue(it.getRoles().contains(TokenValue.Role.AUTHENTICATION));
    }
        
    @Test
    public void testOnetimeTokenForAuthenticationDoubleUsage() throws KeyserverException {
        AuthResponse ot = this.createUserWithOnetimeTokenForAuthentication();
        
        ks.authenticateWithOnetime(ot.getB64Token(), false, null);
        try {
           ks.authenticateWithOnetime(ot.getB64Token(), false, null);
           fail();
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN, e.getMessage());
        }
    }
    
    @Test
    public void testOnetimeTokenForAuthenticationReschedule() throws KeyserverException {
        AuthResponse ot = this.createUserWithOnetimeTokenForAuthentication();
        
        AuthResponse it = ks.authenticateWithOnetime(ot.getB64Token(), true, null);
        assertTrue(it.hasNext());
        AuthResponse next = it.getNext();
        assertEquals(ot.getServiceUserId(), next.getServiceUserId());
        assertEquals(USERNAME, next.getUsername());
        assertTrue(next.getRoles().contains(TokenValue.Role.AUTHENTICATION));
    }
    
    @Test
    public void testOnetimeTokenForAuthenticationAccessFail() throws KeyserverException {
        AuthResponse ot = this.createUserWithOnetimeTokenForAuthentication();
        
        AuthResponse it = ks.authenticateWithOnetime(ot.getB64Token(), false, null);
        try {
            ks.getIndexKey(it.getUserId(), it.getAccountKey());
            fail();
        } catch(KeyserverException e) {
            assertTrue(e.getCause().getCause() instanceof javax.crypto.BadPaddingException);
        }
    }
    
    //=========================================================================
    // App logic
    //=========================================================================

    @Test
    public void testAuthenticateApp() throws KeyserverException {
        App u = ks.registerApp(App.Approle.WORKER);
        assertEquals(App.Approle.WORKER, u.getAppRole());
        
        AuthResponse u2 = ks.authenticateApp(u.getAppId(), u.getPassword());
        assertEquals(u.getAppId(), u2.getServiceUserId());
        assertEquals(u.getPassword(), KeyserverUtils.toBase64String(u2.getAccountKey()));
        assertTrue(u2.getRoles().contains(App.Approle.WORKER));
        
        AuthResponse u3 = ks.authenticateWithInternalToken(u2.getB64Token());
        assertEquals(u.getAppId(), u3.getServiceUserId());
        assertEquals(u.getPassword(), KeyserverUtils.toBase64String(u3.getAccountKey()));
        assertTrue(u3.getRoles().contains(App.Approle.WORKER));
        
        ks.removeApp(u.getAppId());
    }

    @Test
    public void testAuthenticateCoreApp() throws KeyserverException {
        String appId = SERVICE_APPID;
        String appKey = SERVICE_SECRET;
        AuthResponse u = ks.authenticateApp(appId, appKey);
        assertEquals(u.getServiceUserId(), appId);
        assertEquals(SERVICE_SECRET, KeyserverUtils.toBase64String(u.getAccountKey()));
        assertTrue(u.getRoles().contains(App.Approle.SERVICE));
    }

    @Test
    public void testAuthenticateNonExistantApp() throws KeyserverException {
        try {
            ks.authenticateApp("not here", "xxx");
            fail();
        } catch (EntryNotFoundException e) {
            assertTrue(e.getMessage().equals(EntryNotFoundException.APP));
        }
    }

    @Test
    public void testAuthenticateAppFail() throws KeyserverException {
        App u = ks.registerApp(App.Approle.WORKER);
        try {
            ks.authenticateApp(u.getAppId(), "xxx");
            fail();
        } catch (KeyserverException e) {
            assertTrue(e.getCause().getCause() instanceof javax.crypto.BadPaddingException);
        }
        ks.removeApp(u.getAppId());
    }
    
    @Test
    public void testListApp() throws KeyserverException {
        App u = ks.registerApp(App.Approle.WORKER);
        App u2 = ks.registerApp(App.Approle.INDEXER);
        
        List<App> apps = ks.listApps(SERVICE_SECRET);
        assertTrue(apps.size() >= 3);

        boolean foundCore = false, foundWorker = false, foundIndexer = false;

        for (App a : apps) {
            if (a.getAppRole() == Approle.SERVICE && a.getAppId().equals(SERVICE_APPID)) {
                foundCore = true;
            }
            if (a.getAppRole() == u.getAppRole() && a.getAppId().equals(u.getAppId())) {
                foundWorker = true;
            }
            if (a.getAppRole() == u2.getAppRole() && a.getAppId().equals(u2.getAppId())) {
                foundIndexer = true;
            }
            assertNull(a.getPassword());
        }

        assertTrue(foundCore);
        assertTrue(foundWorker);
        assertTrue(foundIndexer);
     
        ks.removeApp(u.getAppId());
        ks.removeApp(u2.getAppId());
    }

    @Test
    public void testRemoveApp() throws KeyserverException {
        App u = ks.registerApp(App.Approle.WORKER);
        ks.removeApp(u.getAppId());
        try {
            ks.authenticateApp(u.getAppId(), u.getPassword());
            fail();
        } catch (EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.APP, e.getMessage());
        }
    }
}
