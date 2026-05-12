package org.studyplatform.courseservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class InternalApiProperties {

    private String internalApiKey;

    public String getInternalApiKey() {
        return internalApiKey;
    }

    public void setInternalApiKey(String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }
}
