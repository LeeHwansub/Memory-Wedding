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

    private String model = "gemini-2.5-flash";

    /** Max photos analyzed per job. */
    private int maxPhotos = 20;

    /**
     * Minimum confidence (0.0–1.0) to include a photo in scene groups / Best Shot.
     * Below this threshold the photo is excluded from classification UI (original stays in Drive Photos).
     */
    private double minConfidence = 0.60;
}
