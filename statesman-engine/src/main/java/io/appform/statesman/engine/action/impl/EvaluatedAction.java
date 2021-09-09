package io.appform.statesman.engine.action.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.inject.name.Named;
import io.appform.eventingester.client.EventPublisher;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;
import io.appform.statesman.engine.action.ActionHelper;
import io.appform.statesman.engine.action.BaseAction;
import io.appform.statesman.model.ActionImplementation;
import io.appform.statesman.model.DataObject;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.template.EvaluatedActionTemplate;
import io.appform.statesman.model.action.template.RuleBasedTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Data
@Singleton
@ActionImplementation(name = "EVALUATED")
public class EvaluatedAction extends BaseAction<EvaluatedActionTemplate> {

    private ActionHelper actionHelper;

    private final HopeLangEngine hopeLangEngine = HopeLangEngine.builder()
            .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
            .build();

    private final LoadingCache<String, Evaluatable> evalCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .build(hopeLangEngine::parse);

    @Inject
    public EvaluatedAction(
            @Named("eventPublisher") final EventPublisher publisher,
            ObjectMapper mapper,
            ActionHelper actionHelper) {
        super(publisher, mapper);
        this.actionHelper = actionHelper;
    }

    @Override
    public ActionType getType() {
        return ActionType.EVALUATED;
    }

    @Override
    protected JsonNode execute(EvaluatedActionTemplate evaluatedActionTemplate, Workflow workflow) {

        log.debug("Evaluated Action triggered with Template: {} and Workflow: {}",
                evaluatedActionTemplate, workflow);

        Preconditions.checkNotNull(workflow);
        final DataObject dataObject = workflow.getDataObject();
        val evalNode = mapper.createObjectNode();
        evalNode.putObject("data").setAll((ObjectNode) dataObject.getData());

        val selectedAction = evaluatedActionTemplate.getRuleBasedTemplates().stream()
                .filter(template -> template.getType().equals(RuleBasedTemplate.Type.EVALUATED))
                .filter(template -> hopeLangEngine.evaluate(evalCache.get(template.getRule()), evalNode))
                .findFirst()
                .orElse(defaultAction(evaluatedActionTemplate));

        if (selectedAction == null || selectedAction.getActions() == null) {
            log.error("No action selected for given data for template {} and workflow {}",
                    evaluatedActionTemplate, workflow);
            return mapper.createObjectNode();
        }

        return actionHelper.executeActions(selectedAction.getActions(), workflow);
    }

    private RuleBasedTemplate defaultAction(EvaluatedActionTemplate evaluatedActionTemplate) {
        return evaluatedActionTemplate.getRuleBasedTemplates().stream()
                .filter(template -> template.getType().equals(RuleBasedTemplate.Type.DEFAULT))
                .findFirst()
                .orElse(null);
    }
}
