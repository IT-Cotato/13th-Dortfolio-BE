package com.itcotato.dortfolio.global.ai.generation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenerationMetadata {

    @Column(name = "prompt_version", length = 100)
    private String promptVersion;

    @Column(name = "schema_version", length = 100)
    private String schemaVersion;

    @Column(name = "generation_model", length = 200)
    private String model;

    @Column(name = "max_output_tokens")
    private Integer maxOutputTokens;

    @Column(name = "thinking_level", length = 20)
    private String thinkingLevel;

    @JsonIgnore
    public boolean isValid() {
        return StringUtils.hasText(promptVersion) && promptVersion.length() <= 100
                && StringUtils.hasText(schemaVersion) && schemaVersion.length() <= 100
                && StringUtils.hasText(model) && model.length() <= 200
                && maxOutputTokens != null && maxOutputTokens > 0
                && (thinkingLevel == null || java.util.Set.of("minimal", "low", "medium", "high").contains(thinkingLevel));
    }

    @JsonCreator
    public GenerationMetadata(
            @JsonProperty("promptVersion") String promptVersion,
            @JsonProperty("schemaVersion") String schemaVersion,
            @JsonProperty("model") String model,
            @JsonProperty("maxOutputTokens") Integer maxOutputTokens,
            @JsonProperty("thinkingLevel") String thinkingLevel
    ) {
        this.promptVersion = promptVersion;
        this.schemaVersion = schemaVersion;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.thinkingLevel = thinkingLevel;
    }
}
