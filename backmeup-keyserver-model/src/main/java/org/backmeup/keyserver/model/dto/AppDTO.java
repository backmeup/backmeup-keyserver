package org.backmeup.keyserver.model.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@SuppressWarnings("unused")
public class AppDTO {
    private String appId;
    private String password;

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
}
