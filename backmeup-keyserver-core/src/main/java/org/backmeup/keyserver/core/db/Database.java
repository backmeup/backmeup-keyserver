package org.backmeup.keyserver.core.db;

import org.backmeup.keyserver.model.KeyserverEntry;

public interface Database {
	public void connect() throws DatabaseException;
	public void disconnect() throws DatabaseException;
	public boolean isConnected();
	public KeyserverEntry getEntry(String key) throws DatabaseException;
	public KeyserverEntry getEntry(String key, long version) throws DatabaseException;
	public void putEntry(KeyserverEntry entry) throws DatabaseException;
	public void updateTTL(KeyserverEntry entry) throws DatabaseException;
}
