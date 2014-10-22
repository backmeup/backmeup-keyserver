package org.backmeup.keyserver.model;

import java.util.HashSet;
import java.util.Set;

public class TokenValue {
	public static enum Role {USER, BACKUP_JOB, SHARE_SOURCE, SHARE_TARGET};
	
	private String userId;
	private String serviceUserId;
	private Set<Role> roles = new HashSet<>();

	public TokenValue() {}
	
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
}