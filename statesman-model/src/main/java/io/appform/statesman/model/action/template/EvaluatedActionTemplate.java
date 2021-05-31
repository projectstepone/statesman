package io.appform.statesman.model.action.template;

import io.appform.statesman.model.action.ActionType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class EvaluatedActionTemplate extends ActionTemplate {

    private List<RuleBasedTemplate> ruleBasedTemplates;

    public EvaluatedActionTemplate() {
        super(ActionType.EVALUATED);
    }

    @Builder
    public EvaluatedActionTemplate(String templateId, String name, boolean active, List<RuleBasedTemplate> ruleBasedTemplates) {
        super(ActionType.EVALUATED, templateId, name, active);
        this.ruleBasedTemplates = ruleBasedTemplates;
    }

    @Override
    public <T> T visit(ActionTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
