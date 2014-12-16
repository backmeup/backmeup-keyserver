package org.backmeup.keyserver.model.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@SuppressWarnings("unused")
public class AppDTO {
    private String appId;
    private String password;
    private String appRole;

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

    public String getAppRole() {
        return appRole;
    }

    public void setAppRole(String appRole) {
        this.appRole = appRole;
    }
}
