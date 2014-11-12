package org.backmeup.keyserver.core;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

        AuthResponse u2 = ks.authenticateWithInternalToken(u.getLoginToken());
        assertEquals(u.getServiceUserId(), u2.getServiceUserId());
        assertEquals(u.getRoles(), u2.getRoles());
    }

    @Test
    public void testRevokeToken() throws KeyserverException {
        String serviceUserId = ks.registerUser("wolfgang2", "mypass");
        AuthResponse u = ks.authenticateUserWithPassword("wolfgang2", "mypass");
        assertEquals(serviceUserId, u.getServiceUserId());
        assertNotNull(u.getLoginToken());

        ks.revokeToken(Token.Kind.INTERNAL, u.getLoginToken());
        
        try {
            ks.authenticateWithInternalToken(u.getLoginToken());
        } catch(EntryNotFoundException e) {
            assertTrue(e.getMessage().equals(EntryNotFoundException.TOKEN));
        }
    }

    @Test
    public void testAuthenticateApp() throws KeyserverException {
        AppUser u = ks.registerApp(AppUser.Approle.WORKER);
        AppUser u2 = ks.authenticateApp(u.getAppId(), u.getPassword());
        assertEquals(u.getAppId(), u2.getAppId());
        assertEquals(u.getPassword(), u2.getPassword());
        assertEquals(AppUser.Approle.WORKER, u.getApprole());
        assertEquals(AppUser.Approle.WORKER, u2.getApprole());
    }

    @Test
    public void testRegisterCoreApp() throws KeyserverException {
        try {
            ks.registerApp(AppUser.Approle.CORE);
        } catch (KeyserverException e) {
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
        assertEquals(AppUser.Approle.CORE, u.getApprole());
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
        AppUser u = ks.registerApp(AppUser.Approle.WORKER);
        try {
            ks.authenticateApp(u.getAppId(), "xxx");
        } catch (KeyserverException e) {
            assertTrue(e.getCause().getCause() instanceof javax.crypto.BadPaddingException);
        }
    }
    
    @Test
    public void testListApp() throws KeyserverException {
        ks.removeApp("%"); //fix this because of security?
        
        AppUser u = ks.registerApp(AppUser.Approle.WORKER);
        AppUser u2 = ks.registerApp(AppUser.Approle.INDEXER);
        
        List<AppUser> apps = ks.listApps(ks.servicePassword);
        Collections.sort(apps, new Comparator<AppUser>() {
            @Override
            public int compare(AppUser o1, AppUser o2) {
                int c = o1.getApprole().compareTo(o2.getApprole());
                if (c != 0) {
                    return c;
                }
                return o1.getAppId().compareTo(o2.getAppId());
            }
        });;
        assertEquals(3, apps.size());
     
        AppUser a = apps.get(0);
        assertEquals(ks.serviceId, a.getAppId());
        assertNull(a.getPassword());
        assertEquals(AppUser.Approle.CORE, a.getApprole());
        
        a = apps.get(1);
        assertEquals(u.getAppId(), a.getAppId());
        assertNull(a.getPassword());
        assertEquals(AppUser.Approle.WORKER, a.getApprole());
        
        a = apps.get(2);
        assertEquals(u2.getAppId(), a.getAppId());
        assertNull(a.getPassword());
        assertEquals(AppUser.Approle.INDEXER, a.getApprole());
    }

    @Test
    public void testRemoveApp() throws KeyserverException {
        AppUser u = ks.registerApp(AppUser.Approle.WORKER);
        ks.removeApp(u.getAppId());
        try {
            ks.authenticateApp(u.getAppId(), u.getPassword());
        } catch (EntryNotFoundException e) {
            assertTrue(e.getMessage().equals(EntryNotFoundException.APP));
        }
    }
}
