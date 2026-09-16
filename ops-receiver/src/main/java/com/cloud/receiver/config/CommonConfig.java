package com.cloud.receiver.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
@ConfigurationProperties(prefix = "base")
public class CommonConfig {


    //admin管理员密码
    private String admindPwd = "111111";
    private String agentToken = "";
    private String dashView;
    private Integer dbTableTimes = 3600000;
    private Integer heathTimes = 600000;

    private String serverUrl;


    public String getAdmindPwd() {
        return admindPwd;
    }

    public void setAdmindPwd(String admindPwd) {
        this.admindPwd = admindPwd;
    }


    public String getAgentToken() {
        return agentToken;
    }

    public void setAgentToken(String agentToken) {
        this.agentToken = agentToken;
    }

    public String getDashView() {
        if (StringUtils.isEmpty(dashView)) {
            return "true";
        }
        return dashView;
    }

    public void setDashView(String dashView) {
        this.dashView = dashView;
    }

    public Integer getDbTableTimes() {
        if (dbTableTimes == null) {
            return 3600000;
        }
        return dbTableTimes;
    }

    public void setDbTableTimes(Integer dbTableTimes) {
        this.dbTableTimes = dbTableTimes;
    }

    public Integer getHeathTimes() {
        if (heathTimes == null) {
            return 600000;
        }
        return heathTimes;
    }

    public void setHeathTimes(Integer heathTimes) {
        this.heathTimes = heathTimes;
    }
}
