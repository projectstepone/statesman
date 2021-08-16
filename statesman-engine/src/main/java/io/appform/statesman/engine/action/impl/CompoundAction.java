package io.appform.statesman.engine.action.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.name.Named;
import io.appform.eventingester.client.EventPublisher;
import io.appform.statesman.engine.action.ActionHelper;
import io.appform.statesman.engine.action.BaseAction;
import io.appform.statesman.model.ActionImplementation;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.template.CompoundActionTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;


@Slf4j
@Data
@Singleton
@ActionImplementation(name = "COMPOUND")
public class CompoundAction extends BaseAction<CompoundActionTemplate> {

    private ActionHelper actionHelper;

    @Inject
    public CompoundAction(
            @Named("eventPublisher") final EventPublisher publisher,
            ObjectMapper mapper,
            ActionHelper actionHelper) {
        super(publisher, mapper);
        this.actionHelper = actionHelper;
    }


    @Override
    public ActionType getType() {
        return ActionType.COMPOUND;
    }

    @Override
    public JsonNode execute(CompoundActionTemplate compoundActionTemplate, Workflow workflow) {

        log.debug("Compound Action triggered with Template: {} and Workflow: {}",
                compoundActionTemplate, workflow);

        return actionHelper.executeActions(compoundActionTemplate.getActionTemplates(), workflow);
    }

}
