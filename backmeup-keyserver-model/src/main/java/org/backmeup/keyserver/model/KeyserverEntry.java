package org.backmeup.keyserver.model;

import java.util.Calendar;
import java.util.TimeZone;

public class KeyserverEntry {
    protected String key;
    protected byte[] value;
    protected int keyringId;
    protected long version = 0;
    protected Calendar createdAt;
    protected Calendar lastModified;
    protected Calendar ttl;

    public KeyserverEntry(String key) {
        this.key = key;
        this.value = new byte[0];
        this.createdAt = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        this.lastModified = this.createdAt;
    }

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "only used internally")
    public KeyserverEntry(String key, byte[] value, int keyringId, long version, Calendar createdAt, Calendar lastModified, Calendar ttl) {
        this.key = key;
        this.value = value;
        this.keyringId = keyringId;
        this.version = version;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
        this.ttl = ttl;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "only used internally")
    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.setValue(value, false);
    }

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "only used internally")
    public void setValue(byte[] value, boolean preserveTTL) {
        this.value = value;
        this.lastModified = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        this.version++;
        if (!preserveTTL) {
            this.ttl = null;
        }
    }

    public int getKeyringId() {
        return keyringId;
    }

    public void setKeyringId(int keyringId) {
        this.keyringId = keyringId;
    }

    public Calendar getCreatedAt() {
        return createdAt;
    }

    public Calendar getLastModified() {
        return lastModified;
    }

    public Calendar getTTL() {
        return ttl;
    }

    public void setTTL(Calendar ttl) {
        this.ttl = ttl;
    }

    public void expire() {
        this.setTTL(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
    }

    public String getKey() {
        return key;
    }

    public long getVersion() {
        return version;
    }
}
