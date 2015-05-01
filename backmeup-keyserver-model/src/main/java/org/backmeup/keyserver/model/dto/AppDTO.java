package org.backmeup.keyserver.model.dto;

import javax.xml.bind.annotation.XmlRootElement;

import org.backmeup.keyserver.model.App.Approle;

@XmlRootElement
@SuppressWarnings("unused")
public class AppDTO {
    private String appId;
    private String password;
    private Approle appRole;

    public AppDTO() {

    }

    public String getAppId() {
        return appId;
    }

    public void setUserId(String appId) {
        this.appId = appId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Approle getAppRole() {
        return appRole;
    }

    public void setAppRole(Approle appRole) {
        this.appRole = appRole;
    }
    
    @Override
    public String toString() {
        return this.appId + " (" + this.appRole.toString() + ")";
    }
}
