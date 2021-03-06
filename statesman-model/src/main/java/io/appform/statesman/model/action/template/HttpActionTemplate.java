package io.appform.statesman.model.action.template;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.statesman.model.action.ActionType;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Data
public class HttpActionTemplate extends ActionTemplate {

    @NotNull
    @NotEmpty
    private String method;

    @NotNull
    @NotEmpty
    private String url;

    private String payload;

    private String headers;

    private String responseTranslator;

    private boolean noop;

    private JsonNode noopResponse;

    private Set<Integer> acceptableCodes;

    public HttpActionTemplate() {
        super(ActionType.HTTP);
    }

    @Builder
    public HttpActionTemplate(String templateId,
                              String name,
                              boolean active,
                              String method,
                              String url,
                              String payload,
                              String headers,
                              String responseTranslator,
                              boolean noop,
                              JsonNode noopResponse,
                              Set<Integer> acceptableCodes) {
        super(ActionType.HTTP, templateId, name, active);
        this.method = method;
        this.url = url;
        this.payload = payload;
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
