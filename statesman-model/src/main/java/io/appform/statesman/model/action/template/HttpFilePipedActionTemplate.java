package io.appform.statesman.model.action.template;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.statesman.model.action.ActionType;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Data
public class HttpFilePipedActionTemplate extends ActionTemplate{

    @NotNull
    @NotEmpty
    private String sourceMethod;

    @NotNull
    @NotEmpty
    private String targetMethod;

    @NotNull
    @NotEmpty
    private String sourceUrl;

    @NotNull
    @NotEmpty
    private String targetUrl;

    private String sourcePayload;

    private String targetPayload;

    private String fileMeta;

    private String sourceHeaders;

    private String targetHeaders;

    private String responseTranslator;

    private boolean noop;

    private JsonNode noopResponse;

    private Set<Integer> sourceAcceptableCodes;

    private Set<Integer> targetAcceptableCodes;

    public HttpFilePipedActionTemplate() {
        super(ActionType.HTTP_FILE_PIPED);
    }

    @Builder
    public HttpFilePipedActionTemplate(String templateId,
                                       String name, boolean active,
                                       String sourceMethod, String targetMethod,
                                       String sourceUrl, String targetUrl,
                                       String sourcePayload, String targetPayload,
                                       String sourceHeaders, String targetHeaders,
                                       String responseTranslator,
                                       boolean noop, JsonNode noopResponse,
                                       Set<Integer> sourceAcceptableCodes, Set<Integer> targetAcceptableCodes) {
        super(ActionType.HTTP_FILE_PIPED, templateId, name, active);
        this.sourceMethod = sourceMethod;
        this.targetMethod = targetMethod;
        this.sourceUrl = sourceUrl;
        this.targetUrl = targetUrl;
        this.sourcePayload = sourcePayload;
        this.targetPayload = targetPayload;
        this.sourceHeaders = sourceHeaders;
        this.targetHeaders = targetHeaders;
        this.responseTranslator = responseTranslator;
        this.noop = noop;
        this.noopResponse = noopResponse;
        this.sourceAcceptableCodes = sourceAcceptableCodes;
        this.targetAcceptableCodes = targetAcceptableCodes;
    }

    @Override
    public <T> T visit(ActionTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
