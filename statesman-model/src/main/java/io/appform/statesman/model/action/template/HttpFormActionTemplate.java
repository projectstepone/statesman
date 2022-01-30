package io.appform.statesman.model.action.template;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.statesman.model.action.ActionType;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
public class HttpFormActionTemplate extends ActionTemplate {

    @NotNull
    @NotEmpty
    private String url;

    @NotEmpty
    private Map<String, String> formParams;

    private String headers;

    private String responseTranslator;

    private boolean noop;

    private JsonNode noopResponse;

    private Set<Integer> acceptableCodes;

    public HttpFormActionTemplate() {
        super(ActionType.HTTP);
    }

    @Builder
    public HttpFormActionTemplate(String templateId,
                              String name,
                              boolean active,
                              String url,
                              Map<String, String> formParams,
                              String headers,
                              String responseTranslator,
                              boolean noop,
                              JsonNode noopResponse,
                              Set<Integer> acceptableCodes) {
        super(ActionType.HTTP, templateId, name, active);
        this.url = url;
        this.formParams = formParams;
        this.headers = headers;
        this.responseTranslator = responseTranslator;
        this.noop = noop;
        this.noopResponse = noopResponse;
        this.acceptableCodes = acceptableCodes;
    }

    @Override
    public <T> T visit(ActionTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
