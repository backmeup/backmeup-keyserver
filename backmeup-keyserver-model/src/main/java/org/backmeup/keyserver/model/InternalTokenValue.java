package org.backmeup.keyserver.model;

public class InternalTokenValue extends TokenValue {
    private String username;
    private byte[] accountKey;

    public InternalTokenValue() {
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "only used internally")
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public InternalTokenValue(String userId, String serviceUserId, Role role, String username, byte[] accountKey) {
        super(userId, serviceUserId, role);
        this.username = username;
        this.accountKey = accountKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "only used internally")
    public byte[] getAccountKey() {
        return this.accountKey;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "only used internally")
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public void setAccountKey(byte[] accountKey) {
        this.accountKey = accountKey;
    }
}
