package org.backmeup.keyserver.tests.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.backmeup.keyserver.client.CallForbiddenException;
import org.backmeup.keyserver.client.KeyserverClient;
import org.backmeup.keyserver.model.App.Approle;
import org.backmeup.keyserver.model.EntryNotFoundException;
import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.keyserver.model.KeyserverUtils;
import org.backmeup.keyserver.model.TokenValue;
import org.backmeup.keyserver.model.TokenValue.Role;
import org.backmeup.keyserver.model.dto.AppDTO;
import org.backmeup.keyserver.model.dto.AuthResponseDTO;
import org.backmeup.keyserver.model.dto.TokenDTO;
import org.backmeup.keyserver.tests.IntegrationTest;
import org.backmeup.keyserver.tests.utils.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class KeyserverClientIntegrationTest {

    private KeyserverClient client;

    private static final String BASE_URI = Configuration.getProperty("backmeup.keyserver.integration.baseuri");
    private static final String SERVICE_ID = Configuration.getProperty("backmeup.keyserver.integration.service_id");
    private static final String SERVICE_SECRET = Configuration.getProperty("backmeup.keyserver.integration.service_secret");
    private static final String PASSWORD = Configuration.getProperty("backmeup.keyserver.integration.password");
    private static final String USERNAME = Configuration.getProperty("backmeup.keyserver.integration.username");

    @Before
    public void setUp() throws Exception {

        client = new KeyserverClient(BASE_URI, SERVICE_ID, SERVICE_SECRET);
        try {
            AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);
            client.removeUser(u.getToken());
        } catch (KeyserverException e) {
            ;
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    // =========================================================================
    // App logic
    // =========================================================================

    @Test
    public void testAuthenticateApp() throws KeyserverException {
        AppDTO u = client.registerApp(Approle.WORKER);
        AppDTO u2 = client.authenticateApp(u.getAppId(), u.getPassword());
        assertEquals(u.getAppId(), u2.getAppId());
        assertEquals(u.getPassword(), u2.getPassword());
        assertEquals(Approle.WORKER, u.getAppRole());
        assertEquals(Approle.WORKER, u2.getAppRole());
        client.removeApp(u.getAppId());
    }

    @Test
    public void testRegisterCoreApp() {
        try {
            client.registerApp(Approle.CORE);
        } catch (KeyserverException e) {
            assertTrue(e.getMessage().contains("forbidden"));
        }
    }

    @Test
    public void testAuthenticateCoreApp() throws KeyserverException {
        AppDTO u = client.authenticateApp(SERVICE_ID, SERVICE_SECRET);
        assertEquals(u.getAppId(), SERVICE_ID);
        assertEquals(u.getPassword(), SERVICE_SECRET);
        assertEquals(Approle.CORE, u.getAppRole());
    }

    @Test
    public void testAuthenticateNonExistantApp() throws KeyserverException {
        try {
            client.authenticateApp("not here", "xxx");
        } catch (EntryNotFoundException e) {
            assertTrue(e.getMessage().equals(EntryNotFoundException.APP));
        }
    }

    @Test
    public void testAuthenticateAppFail() throws KeyserverException {
        AppDTO u = client.registerApp(Approle.WORKER);
        try {
            client.authenticateApp(u.getAppId(), "xxx");
        } catch (CallForbiddenException e) {
            assertEquals(Response.Status.UNAUTHORIZED, e.getStatus());
        }
        client.removeApp(u.getAppId());
    }

    @Test
    public void testListApps() throws KeyserverException {
        AppDTO u = client.registerApp(Approle.WORKER);
        AppDTO u2 = client.registerApp(Approle.INDEXER);

        List<AppDTO> apps = client.listApps();
        assertTrue(apps.size() >= 3);

        boolean foundCore = false, foundWorker = false, foundIndexer = false;

        for (AppDTO a : apps) {
            if (a.getAppRole() == Approle.CORE && a.getAppId().equals(SERVICE_ID)) {
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
        
        client.removeApp(u.getAppId());
        client.removeApp(u2.getAppId());
    }
    
    @Test
    public void testListAppsConcurrent() throws KeyserverException, InterruptedException {
        List<Thread> threads = new LinkedList<>();
        final List<Boolean> ret = new LinkedList<>();
        
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    List<AppDTO> apps = client.listApps();
                    ret.add(apps.size() >= 1);
                    
                } catch (KeyserverException e) {
                    e.printStackTrace();
                    ret.add(false);
                }
    
            }
        };
        
        int size = 30;
        for (int i=0; i<size; i++) {
            Thread t = new Thread(r);
            threads.add(t);
            t.start();
        }
        
        for(Thread t : threads) {
            t.join();
        }
        
        assertEquals(size, ret.size());
        for(Boolean b : ret) {
            assertTrue(b);
        }
    }

    @Test
    public void testRemoveApp() throws KeyserverException {
        AppDTO u = client.registerApp(Approle.WORKER);
        client.removeApp(u.getAppId());
        try {
            client.authenticateApp(u.getAppId(), u.getPassword());
        } catch (EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.APP, e.getMessage());
        }
    }

    // =========================================================================
    // User logic
    // =========================================================================

    @Test
    public void testRegisterUser() throws KeyserverException {
        String serviceUserId = client.registerUser(USERNAME, PASSWORD);
        assertNotNull(serviceUserId);

        client.authenticateUserWithPassword(USERNAME, PASSWORD);
    }

    @Test
    public void testAuthenticateUserWithPassword() throws KeyserverException {
        String serviceUserId = client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);
        assertEquals(serviceUserId, u.getServiceUserId());
        assertEquals(USERNAME, u.getUsername());
        assertTrue(u.getRoles().contains(Role.USER));
        assertNotNull(u.getToken());
    }

    @Test
    public void testAuthenticateUserWithPasswordFail() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);

        try {
            client.authenticateUserWithPassword(USERNAME, "xxx");
        } catch (KeyserverException e) {
            assertTrue(e.isCausedByCryptoException());
        }
    }

    @Test
    public void testAuthenticateUserWithInternalToken() throws KeyserverException {
        String serviceUserId = client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        AuthResponseDTO u2 = client.authenticateWithInternalToken(u.getToken());
        assertEquals(serviceUserId, u2.getServiceUserId());
        assertEquals(u.getServiceUserId(), u2.getServiceUserId());
        assertEquals(u.getRoles(), u2.getRoles());
    }

    @Test
    public void testProfile() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        String profile = client.getProfile(u.getToken());
        assertNotNull(profile);
        client.setProfile(u.getToken(), "{\"key\": \"Test\"}");
        profile = client.getProfile(u.getToken());
        assertEquals("{\"key\": \"Test\"}", profile);
    }

    @Test
    public void testIndexKey() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        String indexKey = client.getIndexKey(u.getToken());
        assertNotNull(indexKey);
    }

    @Test
    public void testIndexKey2() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        // simulate UI call
        String tokenHeader = u.getToken().toTokenString();
        String indexKey = client.getIndexKey(TokenDTO.fromTokenString(tokenHeader));
        assertNotNull(indexKey);
    }

    @Test
    public void testRemoveUserWithLogin() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        client.removeUser(u.getToken());

        try {
            client.authenticateUserWithPassword(USERNAME, PASSWORD);
        } catch (EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.USERNAME, e.getMessage());
        }

        try {
            client.authenticateWithInternalToken(u.getToken());
        } catch (EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN_USER_REMOVED, e.getMessage());
        }
    }

    @Test
    public void testQuickReRegistration() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);
        client.removeUser(u.getToken());

        try {
            client.authenticateUserWithPassword(USERNAME, PASSWORD);
        } catch (EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.USERNAME, e.getMessage());
        }

        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u2 = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        assertNotEquals(u.getServiceUserId(), u2.getServiceUserId());
    }

    @Test
    public void testRemoveUserByAdmin() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        client.removeUserByAdmin(u.getServiceUserId(), u.getUsername());

        try {
            client.authenticateUserWithPassword(USERNAME, PASSWORD);
        } catch (EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.USERNAME, e.getMessage());
        }

        try {
            client.authenticateWithInternalToken(u.getToken());
        } catch (EntryNotFoundException e) {
            // token still exists, but user not
            assertEquals(EntryNotFoundException.TOKEN_USER_REMOVED, e.getMessage());
        }

        // but now it should be gone
        try {
            client.authenticateWithInternalToken(u.getToken());
        } catch (EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN, e.getMessage());
        }
    }

    @Test
    public void testChangeUserPassword() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        client.changeUserPassword(u.getToken(), PASSWORD, "test");

        try {
            client.authenticateUserWithPassword(USERNAME, PASSWORD);
        } catch (KeyserverException e) {
            assertTrue(e.isCausedByCryptoException());
        } finally {
            AuthResponseDTO u2 = client.authenticateUserWithPassword(USERNAME, "test");
            assertEquals(u.getUsername(), u2.getUsername());
            assertEquals(u.getServiceUserId(), u2.getServiceUserId());

            client.removeUser(u2.getToken());
        }
    }

    // =========================================================================
    // PluginData logic
    // =========================================================================

    @Test
    public void testCreatePluginData() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        String pluginId = "facebook1";
        String data = "json with oauth-token";

        client.createPluginData(u.getToken(), pluginId, data);

        String data2 = client.getPluginData(u.getToken(), pluginId);
        assertEquals(data, data2);
    }

    @Test
    public void testUpdatePluginData() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        String pluginId = "facebook1";
        String data = "json with oauth-token";

        client.createPluginData(u.getToken(), pluginId, data);
        client.updatePluginData(u.getToken(), pluginId, "xxx");

        String savedData = client.getPluginData(u.getToken(), pluginId);
        assertEquals("xxx", savedData);
    }
    
    @Test
    public void testUpdateOrCreatePluginData() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        String pluginId = "facebook1";
        String data = "json with oauth-token";

        client.updatePluginData(u.getToken(), pluginId, data, true);

        String savedData = client.getPluginData(u.getToken(), pluginId);
        assertEquals(data, savedData);
    }

    @Test
    public void testRemovePluginData() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        String pluginId = "facebook1";
        String data = "json with oauth-token";

        client.createPluginData(u.getToken(), pluginId, data);

        client.removePluginData(u.getToken(), pluginId);

        try {
            client.getPluginData(u.getToken(), pluginId);
        } catch (EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.PLUGIN_KEY, e.getMessage());
        }
    }

    // =========================================================================
    // Token logic
    // =========================================================================

    @Test
    public void testRevokeToken() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        client.revokeToken(u.getToken());

        try {
            client.authenticateWithInternalToken(u.getToken());
        } catch (EntryNotFoundException e) {
            assertTrue(e.getMessage().equals(EntryNotFoundException.TOKEN));
        }
    }

    private AuthResponseDTO createUserWithPluginsAndOnetimeToken(Calendar time) throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        String[] pluginIds = { "facebook1", "dropbox1" };
        String data = "json with oauth-token";
        client.createPluginData(u.getToken(), pluginIds[0], data);
        client.createPluginData(u.getToken(), pluginIds[1], data);

        AuthResponseDTO ot = client.createOnetime(u.getToken(), pluginIds, time);
        assertEquals(u.getServiceUserId(), ot.getServiceUserId());
        assertEquals(USERNAME, ot.getUsername());
        assertTrue(ot.getRoles().contains(TokenValue.Role.BACKUP_JOB));
        assertFalse(ot.hasNext());

        return ot;
    }

    @Test
    public void testOnetimeToken() throws KeyserverException {
        AuthResponseDTO ot = this.createUserWithPluginsAndOnetimeToken(KeyserverUtils.getActTime());
        assertEquals(USERNAME, ot.getUsername());
        assertTrue(ot.getRoles().contains(TokenValue.Role.BACKUP_JOB));

        AuthResponseDTO it = client.authenticateWithOnetime(ot.getToken());
        assertEquals(ot.getServiceUserId(), it.getServiceUserId());
        assertEquals(USERNAME, it.getUsername());
        assertTrue(it.getRoles().contains(TokenValue.Role.BACKUP_JOB));

        String[] pluginIds = { "facebook1", "dropbox1" };
        String data = "json with oauth-token";
        for (String pluginId : pluginIds) {
            String pluginData = client.getPluginData(it.getToken(), pluginId);
            assertEquals(data, pluginData);
        }
    }

    @Test
    public void testOnetimeTokenTooEarly() throws KeyserverException {
        Calendar tomorrow = KeyserverUtils.getActTime();
        tomorrow.add(Calendar.DAY_OF_YEAR, +1);
        AuthResponseDTO ot = this.createUserWithPluginsAndOnetimeToken(tomorrow);

        try {
            client.authenticateWithOnetime(ot.getToken());
        } catch (EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN_USED_TO_EARLY, e.getMessage());
        }
    }

    @Test
    public void testOnetimeTokenTooLate() throws KeyserverException {
        Calendar yesterday = KeyserverUtils.getActTime();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        AuthResponseDTO ot = this.createUserWithPluginsAndOnetimeToken(yesterday);

        try {
            client.authenticateWithOnetime(ot.getToken());
        } catch (EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN, e.getMessage());
        }
    }

    @Test
    public void testOnetimeTokenDoubleUsage() throws KeyserverException {
        AuthResponseDTO ot = this.createUserWithPluginsAndOnetimeToken(KeyserverUtils.getActTime());

        client.authenticateWithOnetime(ot.getToken());
        try {
            client.authenticateWithOnetime(ot.getToken());
        } catch (EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.TOKEN, e.getMessage());
        }
    }

    @Test
    public void testOnetimeTokenReschedule() throws KeyserverException {
        Calendar tomorrow = KeyserverUtils.getActTime();
        tomorrow.add(Calendar.DAY_OF_YEAR, +1);
        AuthResponseDTO ot = this.createUserWithPluginsAndOnetimeToken(KeyserverUtils.getActTime());

        AuthResponseDTO it = client.authenticateWithOnetime(ot.getToken(), tomorrow);
        assertTrue(it.hasNext());
        AuthResponseDTO next = it.getNext();
        assertEquals(ot.getServiceUserId(), next.getServiceUserId());
        assertEquals(USERNAME, next.getUsername());
        assertTrue(next.getRoles().contains(TokenValue.Role.BACKUP_JOB));
        assertTrue(next.getToken().getTtl().after(tomorrow));
    }

    @Test
    public void testListTokens() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        List<TokenDTO> tokens = client.listTokens(u.getToken());
        assertEquals(0, tokens.size());
    }
}
