package io.appform.statesman.model.action.template;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RuleBasedTemplate {
    public static enum Type {
        EVALUATED,
        DEFAULT
    }

    private Type type;
    private String rule;
    private List<String> actions;
}
