package org.backmeup.keyserver.model;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TokenValue {
    public static enum Role {
        USER, BACKUP_JOB, SHARE_SOURCE, SHARE_TARGET
    }

    private String userId;
    private String serviceUserId;
    private Set<Role> roles = new HashSet<>();
    private Map<String, Object> values = new HashMap<>();

    public TokenValue() {
    }

    public TokenValue(String userId, String serviceUserId, Role role) {
        this.userId = userId;
        this.serviceUserId = serviceUserId;
        this.roles.add(role);
    }

    public TokenValue(String userId, String serviceUserId, Set<Role> roles) {
        this.userId = userId;
        this.serviceUserId = serviceUserId;
        this.roles.addAll(roles);
    }

    public TokenValue(TokenValue value) {
       this.userId = value.userId;
       this.serviceUserId = value.serviceUserId;
       this.roles.addAll(value.getRoles());
       this.values.putAll(value.getValues());
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getServiceUserId() {
        return serviceUserId;
    }

    public void setServiceUserId(String serviceUserId) {
        this.serviceUserId = serviceUserId;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }
    
    public Map<String, Object> getValues() {
        return values;
    }
    
    public void setValues(Map<String, Object> values) {
        this.values.putAll(values);
    }

    public void putValue(String key, Object value) {
        this.values.put(key, value);
    }
    
    public boolean hasValue(String key) {
        return this.values.containsKey(key);
    }
    
    public Object getValue(String key) {
        return this.values.get(key);
    }
    
    public String getValueAsString(String key) {
        return (String) this.values.get(key);
    }
    
    public byte[] getValueAsByteArray(String key) {
        Object value = this.values.get(key);
        if (value instanceof byte[]) {
            return (byte[]) value;
        } else if (value instanceof String) {
            return KeyserverUtils.fromBase64String((String) value);
        } else {
            return null;
        }
        
    }
    
    public Calendar getValueAsCalendar(String key) {
        Object time = this.values.get(key);
        if (time instanceof Calendar) {
            return (Calendar) time;
        } else {        
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis((Long) time);
            return c;
        }
    }
}