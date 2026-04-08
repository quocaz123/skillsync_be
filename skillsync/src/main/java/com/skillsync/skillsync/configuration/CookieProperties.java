package com.skillsync.skillsync.configuration;

import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Configuration
@ConfigurationProperties(prefix = "app.cookie")
@Getter
@Setter
public class CookieProperties {
    private boolean secure;
    private String sameSite;
}
