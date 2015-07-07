package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.core.EncryptionUtils.fmtKey;
import static org.backmeup.keyserver.core.EncryptionUtils.generateKey;
import static org.backmeup.keyserver.core.EncryptionUtils.hashByteArrayWithPepper;
import static org.backmeup.keyserver.core.EncryptionUtils.stretchStringWithPepper;
import static org.backmeup.keyserver.model.KeyserverUtils.fromBase64String;
import static org.backmeup.keyserver.model.KeyserverUtils.toBase64String;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import org.backmeup.keyserver.core.crypto.Keyring;
import org.backmeup.keyserver.core.crypto.PepperApps;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.model.App;
import org.backmeup.keyserver.model.App.Approle;
import org.backmeup.keyserver.model.CryptoException;
import org.backmeup.keyserver.model.EntryNotFoundException;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.backmeup.keyserver.model.KeyserverException;

/**
 * Keyserver implementation module for app specific logic.
 * @author wolfgang
 *
 */
public class DefaultAppLogic {
    
    private static final MessageFormat APP_ENTRY_FORMAT = new MessageFormat("{0}." + PepperApps.APP);
    private static final MessageFormat APP_ROLE_ENTRY_FORMAT = new MessageFormat("{0}." + PepperApps.APP_ROLE);
    
    private DefaultKeyserverImpl keyserver;
    private Keyring keyring;
    protected String servicePassword;
    private byte[] serviceKey;

    public DefaultAppLogic(DefaultKeyserverImpl keyserver) {
        this.keyserver = keyserver;
        this.keyring = this.keyserver.activeKeyring;
    }
    
    private static String appKey(String appId) {
        return fmtKey(APP_ENTRY_FORMAT, appId);
    }
    
    private static String appRoleKey(String appId) {
        return fmtKey(APP_ROLE_ENTRY_FORMAT, appId);
    }

    // only for initialization/internal use
    protected void setServicePassword(String password) throws KeyserverException {
        this.servicePassword = password;
        try {
            this.serviceKey = stretchStringWithPepper(this.keyring, password, PepperApps.APP_ROLE);
        } catch (CryptoException e) {
            throw new KeyserverException(e);
        }
    }

    // only for initialization/internal use
    protected App registerDefaultApp(App app) throws KeyserverException {
        String appId = app.getAppId();
        byte[] appKey = fromBase64String(app.getPassword());
        Approle role = app.getAppRole();

        try {
            // [Hash(AppKey)].App
            byte[] payload = this.keyserver.encryptString(appKey, PepperApps.APP, role.name());
            this.keyserver.createEntry(appKey(appId), payload);

            // [Hash(AppKey)].AppRole
            payload = this.keyserver.encryptString(this.serviceKey, PepperApps.APP_ROLE, role.name());
            this.keyserver.createEntry(appRoleKey(appId), payload);
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }

        return app;
    }
    
    public App register(App.Approle role) throws KeyserverException {
        String appId = null;
        byte[] appKey = null;

        try {
            boolean collission = false;
            do {
                appKey = generateKey(this.keyring);
                appId = toBase64String(hashByteArrayWithPepper(this.keyring, appKey, PepperApps.APP));

                List<KeyserverEntry> appIds = this.keyserver.db.searchByKey(appKey(appId), true, true);
                collission = !appIds.isEmpty();
            } while (collission);

            // [Hash(AppKey)].App
            byte[] payload = this.keyserver.encryptString(appKey, PepperApps.APP, role.name());
            this.keyserver.createEntry(appKey(appId), payload);
            
            // [Hash(AppKey)].AppRole
            payload = this.keyserver.encryptString(this.serviceKey, PepperApps.APP_ROLE, role.name());
            this.keyserver.createEntry(appRoleKey(appId), payload);
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }

        return new App(appId, toBase64String(appKey), role);
    }

    public void remove(String appId) throws KeyserverException {
        try {
            List<KeyserverEntry> appEntries = this.keyserver.db.searchByKey(appId+".%", true, false);
            if (appEntries.isEmpty()) {
                throw new EntryNotFoundException(EntryNotFoundException.APP);
            }
            
            for(KeyserverEntry ke : appEntries) {
                this.keyserver.expireEntry(ke);
            }
        } catch (DatabaseException e) {
            throw new KeyserverException(e);
        }
    }
    
    public List<App> listApps(String servicePassword) throws KeyserverException {
        try {
            List<App> apps = new LinkedList<>();
            
            List<KeyserverEntry> appRoleEntries = this.keyserver.db.searchByKey(appRoleKey("%"), false, false);
            byte[] key = stretchStringWithPepper(this.keyring, servicePassword, PepperApps.APP_ROLE);
            
            for(KeyserverEntry entry : appRoleEntries) {
                String appId = entry.getKey().split("\\.")[0];
                String appRoleValue = this.keyserver.decryptString(key, PepperApps.APP_ROLE, entry.getValue());
                apps.add(new App(appId, null, App.Approle.valueOf(appRoleValue)));
            }
            return apps;
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }

    public App authenticate(String appId, String appKey) throws KeyserverException {
        try {
            KeyserverEntry appEntry = this.keyserver.checkedGetEntry(appKey(appId), EntryNotFoundException.APP);
            String appValue = this.keyserver.decryptString(fromBase64String(appKey), PepperApps.APP, appEntry.getValue());
            return new App(appId, appKey, App.Approle.valueOf(appValue));
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
}
