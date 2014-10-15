package org.backmeup.keyserver.model.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@SuppressWarnings("unused")
public class AppDTO {
    private String appId;
    private String password;

    public AppDTO() {

    }

    public AppDTO(String password) {
        this.password = password;
    }

    public String getAppId() {
        return appId;
    }

    private void setUserId(String appId) {
        this.appId = appId;
    }

    public String getPassword() {
        return password;
    }

    private void setPassword(String password) {
        this.password = password;
    }
}
