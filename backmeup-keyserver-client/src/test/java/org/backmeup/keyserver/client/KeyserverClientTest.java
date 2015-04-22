package org.backmeup.keyserver.client;

import static org.junit.Assert.*;

import java.util.List;

import javax.ws.rs.core.Response;

import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.EntryNotFoundException;
import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.keyserver.model.dto.AppDTO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KeyserverClientTest {

    private KeyserverClient client;
    private String serviceId = "backmeup-service";
    private String serviceSecret = "REPLACE-ME";

    @Before
    public void setUp() throws Exception {

        client = new KeyserverClient("http://localhost:8081/backmeup-keyserver-rest", serviceId, serviceSecret);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAuthenticateApp() throws KeyserverException {
        AppDTO u = client.registerApp(App.Approle.WORKER);
        AppDTO u2 = client.authenticateApp(u.getAppId(), u.getPassword());
        assertEquals(u.getAppId(), u2.getAppId());
        assertEquals(u.getPassword(), u2.getPassword());
        assertEquals(App.Approle.WORKER, u.getAppRole());
        assertEquals(App.Approle.WORKER, u2.getAppRole());
    }

    @Test
    public void testRegisterCoreApp() {
        try {
            client.registerApp(App.Approle.CORE);
        } catch (KeyserverException e) {
            assertTrue(e.getMessage().contains("forbidden"));
        }
    }

    @Test
    public void testAuthenticateCoreApp() throws KeyserverException {
        AppDTO u = client.authenticateApp(serviceId, serviceSecret);
        assertEquals(u.getAppId(), serviceId);
        assertEquals(u.getPassword(), serviceSecret);
        assertEquals(App.Approle.CORE, u.getAppRole());
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
        AppDTO u = client.registerApp(App.Approle.WORKER);
        try {
            client.authenticateApp(u.getAppId(), "xxx");
        } catch (CallForbiddenException e) {
            assertEquals(Response.Status.UNAUTHORIZED, e.getStatus());
        }
    }

    @Test
    public void testListApps() throws KeyserverException {
        AppDTO u = client.registerApp(App.Approle.WORKER);
        AppDTO u2 = client.registerApp(App.Approle.INDEXER);

        List<AppDTO> apps = client.listApps();
        assertTrue(apps.size() >= 3);

        boolean foundCore = false, foundWorker = false, foundIndexer = false;

        for (AppDTO a : apps) {
            if (a.getAppRole() == App.Approle.CORE && a.getAppId().equals(this.serviceId)) {
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
        AppDTO u = client.registerApp(App.Approle.WORKER);
        client.removeApp(u.getAppId());
        try {
            client.authenticateApp(u.getAppId(), u.getPassword());
        } catch (EntryNotFoundException e) {
            assertEquals(EntryNotFoundException.APP, e.getMessage());
        }
    }
}
