package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.core.KeyserverUtils.*;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import org.backmeup.keyserver.core.crypto.CryptoException;
import org.backmeup.keyserver.core.crypto.Keyring;
import org.backmeup.keyserver.core.crypto.PepperApps;
import org.backmeup.keyserver.core.db.Database;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.model.AppUser;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.backmeup.keyserver.model.Token;

public class DefaultAppLogic {
    
    private static final MessageFormat APP_ENTRY_FORMAT = new MessageFormat("{0}." + PepperApps.APP);
    private static final MessageFormat APP_ROLE_ENTRY_FORMAT = new MessageFormat("{0}." + PepperApps.APP_ROLE);
    
    private DefaultKeyserverImpl keyserver;
    private Keyring keyring;
    private Database db;

    public DefaultAppLogic(DefaultKeyserverImpl keyserver) {
        this.keyserver = keyserver;
        this.keyring = this.keyserver.activeKeyring;
        this.db = this.keyserver.db;
    }
    
    private static String appKey(String appId) {
        return fmtKey(APP_ENTRY_FORMAT, appId);
    }
    
    private static String appRoleKey(String appId) {
        return fmtKey(APP_ROLE_ENTRY_FORMAT, appId);
    }

    public AppUser register(AppUser.Approle role) throws KeyserverException {
        if (role == AppUser.Approle.CORE) {
            throw new KeyserverException("Registration of app with role core is forbidden!");
        }

        String appId = null;
        byte[] appKey = null;

        try {
            boolean collission = false;
            do {
                appKey = generateKey(this.keyring);
                appId = toBase64String(hashByteArrayWithPepper(this.keyring, appKey, PepperApps.APP));

                List<KeyserverEntry> appIds = this.db.searchByKey(appKey(appId), true, true);
                collission = !appIds.isEmpty();
            } while (collission);

            // [Hash(AppKey)].App
            KeyserverEntry ke = new KeyserverEntry(appKey(appId));
            byte[] payload = encryptString(this.keyring, hashByteArrayWithPepper(this.keyring, appKey, PepperApps.APP), role.name());
            ke.setValue(payload);
            this.db.putEntry(ke);
            
            // [Hash(AppKey)].AppRole
            ke = new KeyserverEntry(appRoleKey(appId));
            byte[] key = stretchStringWithPepper(this.keyring, this.keyserver.servicePassword, PepperApps.APP_ROLE);
            payload = encryptString(this.keyring, hashByteArrayWithPepper(this.keyring, key, PepperApps.APP_ROLE), role.name());
            ke.setValue(payload);
            this.db.putEntry(ke);
        } catch (CryptoException | DatabaseException e) {
            throw new KeyserverException(e);
        }

        return new AppUser(appId, toBase64String(appKey), role);
    }

    public void remove(String appId) throws KeyserverException {
        try {
            List<KeyserverEntry> appEntries = this.db.searchByKey(appId+".%", true, false);
            if (appEntries.isEmpty()) {
                throw new EntryNotFoundException(EntryNotFoundException.APP);
            }
            
            for(KeyserverEntry ke : appEntries) {
                ke.expire();
                this.db.updateTTL(ke);
            }
        } catch (DatabaseException e) {
            throw new KeyserverException(e);
        }
    }
    
    public List<AppUser> listApps(String servicePassword) throws KeyserverException {
        try {
            List<AppUser> apps = new LinkedList<>();
            apps.add(new AppUser(this.keyserver.serviceId, null, AppUser.Approle.CORE));
            
            List<KeyserverEntry> appRoleEntries = db.searchByKey(appRoleKey("%"), false, false);
            byte[] key = stretchStringWithPepper(this.keyring, servicePassword, PepperApps.APP_ROLE);
            
            for(KeyserverEntry entry : appRoleEntries) {
                String appId = entry.getKey().split("\\.")[0];
                
                String appRoleValue = decryptString(this.keyring, hashByteArrayWithPepper(this.keyring, key, PepperApps.APP_ROLE), entry.getValue());
                apps.add(new AppUser(appId, null, AppUser.Approle.valueOf(appRoleValue)));
            }
            return apps;
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }

    public AppUser authenticate(String appId, String appKey) throws KeyserverException {
        // workaround for core app
        if (appId.equals(this.keyserver.serviceId) && appKey.equals(this.keyserver.servicePassword)) {
            return new AppUser(appId, appKey, AppUser.Approle.CORE);
        }

        try {
            KeyserverEntry appEntry = this.db.getEntry(appKey(appId));
            if (appEntry == null) {
                throw new EntryNotFoundException(EntryNotFoundException.APP);
            }

            if (appEntry.getKeyringId() < this.keyring.getKeyringId()) {
                // TODO: migrate Entry
            }

            String appValue = decryptString(this.keyring, hashByteArrayWithPepper(this.keyring, fromBase64String(appKey), PepperApps.APP), appEntry.getValue());
            return new AppUser(appId, appKey, AppUser.Approle.valueOf(appValue));
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
}
