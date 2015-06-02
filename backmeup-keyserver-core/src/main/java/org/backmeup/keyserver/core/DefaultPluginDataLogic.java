package org.backmeup.keyserver.core;

import static org.backmeup.keyserver.core.EncryptionUtils.fmtKey;
import static org.backmeup.keyserver.core.EncryptionUtils.generateKey;

import java.text.MessageFormat;

import org.backmeup.keyserver.core.crypto.Keyring;
import org.backmeup.keyserver.core.crypto.PepperApps;
import org.backmeup.keyserver.core.db.DatabaseException;
import org.backmeup.keyserver.model.CryptoException;
import org.backmeup.keyserver.model.EntryNotFoundException;
import org.backmeup.keyserver.model.KeyserverEntry;
import org.backmeup.keyserver.model.KeyserverException;

/**
 * Keyserver implementation module for plugin (data)store specific logic.
 * @author wolfgang
 *
 */
public class DefaultPluginDataLogic {
    private static final MessageFormat PLUGIN_KEY_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.PLUGIN+"{1}.Key");
    private static final MessageFormat PLUGIN_DATA_ENTRY_FMT = new MessageFormat("{0}."+PepperApps.PLUGIN+"{1}");
    
    private DefaultKeyserverImpl keyserver;
    private Keyring keyring;

    public DefaultPluginDataLogic(DefaultKeyserverImpl keyserver) {
        this.keyserver = keyserver;
        this.keyring = this.keyserver.activeKeyring;
    }
    
    private static String keyKey(String userId, String pluginId) {
        return fmtKey(PLUGIN_KEY_ENTRY_FMT, userId, pluginId);
    }
    
    private static String dataKey(String userId, String pluginId) {
        return fmtKey(PLUGIN_DATA_ENTRY_FMT, userId, pluginId);
    }

    public void create(String userId, String pluginId, byte[] accountKey, String data) throws KeyserverException {
        try {
            // [UserId].Plugin.[Id].Key
            byte[] pluginKey = generateKey(this.keyring);
            byte[] payload = this.keyserver.encryptByteArray(accountKey, PepperApps.PLUGIN, pluginKey);
            this.keyserver.createEntry(keyKey(userId, pluginId), payload);
            
            // [UserId].Plugin.[Id]
            payload = this.keyserver.encryptString(pluginKey, PepperApps.PLUGIN, data);
            this.keyserver.createEntry(dataKey(userId, pluginId), payload);
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }

    protected byte[] getPluginKey(String userId, String pluginId, byte[] accountKey) throws KeyserverException {
        try {
            KeyserverEntry pluginKeyEntry = this.keyserver.checkedGetEntry(keyKey(userId, pluginId), EntryNotFoundException.PLUGIN_KEY);
            return this.keyserver.decryptByteArray(accountKey, PepperApps.PLUGIN, pluginKeyEntry.getValue());
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }

    public void remove(String userId, String pluginId) throws KeyserverException {
        try {   
            KeyserverEntry pluginKeyEntry = this.keyserver.checkedGetEntry(keyKey(userId, pluginId), EntryNotFoundException.PLUGIN_KEY);
            KeyserverEntry pluginEntry = this.keyserver.checkedGetEntry(dataKey(userId, pluginId), EntryNotFoundException.PLUGIN);

            this.keyserver.expireEntry(pluginKeyEntry);            
            this.keyserver.expireEntry(pluginEntry);
        } catch (DatabaseException e) {
            throw new KeyserverException(e);
        }
    }
    
    public void update(String userId, String pluginId, byte[] pluginKey, String data) throws KeyserverException {       
        try {          
            KeyserverEntry pluginEntry = this.keyserver.checkedGetEntry(dataKey(userId, pluginId), EntryNotFoundException.PLUGIN);
            
            byte[] payload = this.keyserver.encryptString(pluginKey, PepperApps.PLUGIN, data);
            this.keyserver.updateEntry(pluginEntry, payload);
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
    
    public String get(String userId, String pluginId, byte[] pluginKey) throws KeyserverException {      
        try {
            KeyserverEntry pluginEntry = this.keyserver.checkedGetEntry(dataKey(userId, pluginId), EntryNotFoundException.PLUGIN);
            return this.keyserver.decryptString(pluginKey, PepperApps.PLUGIN, pluginEntry.getValue());
        } catch (DatabaseException | CryptoException e) {
            throw new KeyserverException(e);
        }
    }
}