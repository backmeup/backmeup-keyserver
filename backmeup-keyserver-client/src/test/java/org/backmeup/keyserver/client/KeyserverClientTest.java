package org.backmeup.keyserver.client;

import static org.junit.Assert.*;

import java.util.List;

import javax.ws.rs.core.Response;

import org.backmeup.keyserver.model.App.Approle;
import org.backmeup.keyserver.model.EntryNotFoundException;
import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.keyserver.model.TokenValue.Role;
import org.backmeup.keyserver.model.dto.AppDTO;
import org.backmeup.keyserver.model.dto.AuthResponseDTO;
import org.backmeup.keyserver.model.dto.TokenDTO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KeyserverClientTest {

    private KeyserverClient client;
    
    private static final String SERVICE_ID = "backmeup-service";
    private static final String SERVICE_SECRET = "REPLACE-ME";
    private static final String PASSWORD = "mypass";
    private static final String USERNAME = "rest-test";

    @Before
    public void setUp() throws Exception {

        client = new KeyserverClient("http://localhost:8081/backmeup-keyserver-rest", SERVICE_ID, SERVICE_SECRET);
        try {
            AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);
            client.removeUser(u.getToken());
        } catch(KeyserverException e) {
            ;
        }
    }

    @After
    public void tearDown() throws Exception {
    }
    
    @Test
    public void testAuthenticateApp() throws KeyserverException {
        AppDTO u = client.registerApp(Approle.WORKER);
        AppDTO u2 = client.authenticateApp(u.getAppId(), u.getPassword());
        assertEquals(u.getAppId(), u2.getAppId());
        assertEquals(u.getPassword(), u2.getPassword());
        assertEquals(Approle.WORKER, u.getAppRole());
        assertEquals(Approle.WORKER, u2.getAppRole());
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

    
    @Test
    public void testRegisterUser() throws KeyserverException {
        String serviceUserId = client.registerUser(USERNAME, PASSWORD);
        assertNotNull(serviceUserId);
        
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);
        client.removeUser(u.getToken());
    }
    
    @Test
    public void testAuthenticateUserWithPassword() throws KeyserverException {
        String serviceUserId = client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);
        assertEquals(serviceUserId, u.getServiceUserId());
        assertEquals(USERNAME, u.getUsername());
        assertTrue(u.getRoles().contains(Role.USER));
        assertNotNull(u.getToken());
        
        client.removeUser(u.getToken());
    }
    
    @Test
    public void testAuthenticateUserWithPasswordFail() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        
        try {
            client.authenticateUserWithPassword(USERNAME, "xxx");
        } catch(KeyserverException e) {
            assertTrue(e.isCausedByCryptoException());
        }
        
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);
        client.removeUser(u.getToken());
    }
    
    @Test
    public void testAuthenticateUserWithInternalToken() throws KeyserverException {
        String serviceUserId = client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        AuthResponseDTO u2 = client.authenticateWithInternalToken(u.getToken());
        assertEquals(serviceUserId, u2.getServiceUserId());
        assertEquals(u.getServiceUserId(), u2.getServiceUserId());
        assertEquals(u.getRoles(), u2.getRoles());
        
        client.removeUser(u.getToken());
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
        
        client.removeUser(u.getToken());
    }
    
    @Test
    public void testIndexKey() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        String indexKey = client.getIndexKey(u.getToken());
        assertNotNull(indexKey);
        
        client.removeUser(u.getToken());
    }
    
    @Test
    public void testIndexKey2() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);
        
        // simulate UI call
        String tokenHeader = u.getToken().toTokenString();
        String indexKey = client.getIndexKey(TokenDTO.fromTokenString(tokenHeader));
        assertNotNull(indexKey);
        
        client.removeUser(u.getToken());
    }
 
    @Test
    public void testRemoveUserWithLogin() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);
              
        client.removeUser(u.getToken());
        
        try {
            client.authenticateUserWithPassword(USERNAME, PASSWORD);
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.USERNAME, e.getMessage());
        }
        
        try {
            client.authenticateWithInternalToken(u.getToken());
        } catch(EntryNotFoundException e) {
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
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.USERNAME, e.getMessage());
        }
        
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u2 = client.authenticateUserWithPassword(USERNAME, PASSWORD);
        
        assertNotEquals(u.getServiceUserId(), u2.getServiceUserId());
        
        client.removeUser(u2.getToken());
    }
    
    @Test
    public void testRemoveUserByAdmin() throws KeyserverException {
        client.registerUser(USERNAME, PASSWORD);
        AuthResponseDTO u = client.authenticateUserWithPassword(USERNAME, PASSWORD);

        client.removeUserByAdmin(u.getServiceUserId(), u.getUsername());
        
        try {
            client.authenticateUserWithPassword(USERNAME, PASSWORD);
        } catch(EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.USERNAME, e.getMessage());
        }
        
        try {
            client.authenticateWithInternalToken(u.getToken());
        } catch(EntryNotFoundException e) {
            //token still exists, but user not
            assertEquals(EntryNotFoundException.TOKEN_USER_REMOVED, e.getMessage());
        }
        
        //but now it should be gone
        try {
            client.authenticateWithInternalToken(u.getToken());
        } catch(EntryNotFoundException e) {
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
        } catch(KeyserverException e) {
            assertTrue(e.isCausedByCryptoException());
        } finally {
            AuthResponseDTO u2 = client.authenticateUserWithPassword(USERNAME, "test");
            assertEquals(u.getUsername(), u2.getUsername());
            assertEquals(u.getServiceUserId(), u2.getServiceUserId());
            
            client.removeUser(u2.getToken());
        }
    }
}
