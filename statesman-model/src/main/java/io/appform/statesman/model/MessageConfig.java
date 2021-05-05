package io.appform.statesman.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * REST entity for posting the message config resource to the API
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageConfig {
    @NotNull
    private String messageId;

    @NotNull
    private JsonNode messageBody;
}
