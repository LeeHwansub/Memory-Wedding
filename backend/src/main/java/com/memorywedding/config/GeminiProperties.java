package com.memorywedding.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.gemini")
public class GeminiProperties {

    /** Empty = mock classifier (local/dev without key). */
    private String apiKey = "";

    private String model = "gemini-2.0-flash";

    /** Max photos analyzed per job. */
    private int maxPhotos = 20;
}
