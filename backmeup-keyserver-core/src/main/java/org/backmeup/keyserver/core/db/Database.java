package org.backmeup.keyserver.core.db;

import java.util.List;

import org.backmeup.keyserver.model.KeyserverEntry;

/**
 * Public interface a keyserver database has to provide.
 * @author wolfgang
 *
 */
public interface Database {
    void connect() throws DatabaseException;

    void disconnect() throws DatabaseException;

    boolean isConnected();

    KeyserverEntry getEntry(String key) throws DatabaseException;

    KeyserverEntry getEntry(String key, long version) throws DatabaseException;

    void putEntry(KeyserverEntry entry) throws DatabaseException;

    void updateTTL(KeyserverEntry entry) throws DatabaseException;
    
    List<KeyserverEntry> searchByKey(String key, boolean allVersions, boolean withExpired) throws DatabaseException;
}
