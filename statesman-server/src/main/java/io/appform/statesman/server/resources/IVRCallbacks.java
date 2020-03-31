package io.appform.statesman.server.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;
import io.appform.statesman.engine.StateTransitionEngine;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.model.*;
import io.appform.statesman.model.dataaction.impl.MergeDataAction;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.server.callbacktransformation.CallbackTransformationTemplates;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import io.appform.statesman.server.callbacktransformation.TransformationTemplateVisitor;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.appform.statesman.server.callbacktransformation.impl.StepByStepTransformationTemplate;
import io.appform.statesman.server.evaluator.WorkflowTemplateSelector;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 *
 */
@Path("/callbacks/ivr")
@Slf4j
public class IVRCallbacks {
    private final CallbackTransformationTemplates transformationTemplates;
    private final ObjectMapper mapper;
    private final HandleBarsService handleBarsService;
    private final Provider<StateTransitionEngine> engine;
    private final Provider<WorkflowProvider> workflowProvider;
    private final Provider<WorkflowTemplateSelector> templateSelector;
    private final HopeLangEngine hopeLangEngine;

    @Inject
    public IVRCallbacks(
            CallbackTransformationTemplates transformationTemplates,
            final ObjectMapper mapper,
            HandleBarsService handleBarsService,
            Provider<StateTransitionEngine> engine,
            Provider<WorkflowProvider> workflowProvider,
            Provider<WorkflowTemplateSelector> templateSelector) {
        this.transformationTemplates = transformationTemplates;
        this.mapper = mapper;
        this.handleBarsService = handleBarsService;
        this.engine = engine;
        this.workflowProvider = workflowProvider;
        this.templateSelector = templateSelector;
        hopeLangEngine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
    }

    @GET
    @Path("/final/${ivrProvider}")
    public Response finalIVRCallback(
            @PathParam("ivrProvider") final String ivrProvider,
            @Context final UriInfo uriInfo) throws IOException {
        val queryParams = uriInfo.getQueryParameters();
        val node = mapper.valueToTree(queryParams);
        val transformationTemplate = transformationTemplates.getTemplates().get(ivrProvider);
        if (null == transformationTemplate) {
            throw new StatesmanError("No matching translation template found for context: " + node,
                                     ResponseCode.INVALID_OPERATION);
        }
        val tmpl = transformationTemplate.accept(new TransformationTemplateVisitor<OneShotTransformationTemplate>() {
            @Override
            public OneShotTransformationTemplate visit(OneShotTransformationTemplate oneShotTransformationTemplate) {
                return oneShotTransformationTemplate;
            }

            @Override
            public OneShotTransformationTemplate visit(StepByStepTransformationTemplate stepByStepTransformationTemplate) {
                return null;
            }
        });
        Preconditions.checkNotNull(tmpl);
        val stdPayload = handleBarsService.transform(tmpl.getTemplate(), node);
        val context = mapper.readTree(stdPayload);
        val wfTemplate = templateSelector.get()
                .determineTemplate(context)
                .orElse(null);
        if (null == wfTemplate) {
            throw new StatesmanError("No matching workflow template found for context: " + stdPayload,
                                     ResponseCode.INVALID_OPERATION);
        }
        val wfId = extractWorkflowId(node, transformationTemplate);
        val date = new Date();
        workflowProvider.get()
                .saveWorkflow(new Workflow(wfId,
                                           wfTemplate.getId(),
                                           new DataObject(mapper.createObjectNode(),
                                                          wfTemplate.getStartState(),
                                                          date,
                                                          date)));
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(new DataUpdate(wfId, node, new MergeDataAction()));
        log.debug("Workflow: {} with template: {} went through transitions: {}",
                  wfId, wfTemplate.getId(), appliedTransitions.getTransitions());
        return Response.ok()
                .build();
    }

    @GET
    @Path("/step/{ivrProvider}")
    public Response stepIVRCallback(
            @PathParam("ivrProvider") final String ivrProvider,
            @Context final UriInfo uriInfo) throws IOException {
        val queryParams = uriInfo.getQueryParameters();
        val node = mapper.valueToTree(queryParams);
        val transformationTemplate = transformationTemplates.getTemplates().get(ivrProvider);
        if (null == transformationTemplate) {
            throw new StatesmanError("No matching translation template found for context: " + node,
                                     ResponseCode.INVALID_OPERATION);
        }
        val tmpl = transformationTemplate.accept(new TransformationTemplateVisitor<StepByStepTransformationTemplate>() {
            @Override
            public StepByStepTransformationTemplate visit(OneShotTransformationTemplate oneShotTransformationTemplate) {
                return null;
            }

            @Override
            public StepByStepTransformationTemplate visit(StepByStepTransformationTemplate stepByStepTransformationTemplate) {
                return stepByStepTransformationTemplate;
            }
        });
        Preconditions.checkNotNull(tmpl);
        val date = new Date();
        val selectedStep = selectStep(node, tmpl);
        Preconditions.checkNotNull(selectedStep);
        val stdPayload = handleBarsService.transform(selectedStep.getTemplate(), node);
        val context = mapper.readTree(stdPayload);
        final WorkflowTemplate wfTemplate;
        val wfIdNode = node.at(transformationTemplate.getIdPath());
        final String wfId;
        final Workflow wf;
        if (isValid(wfIdNode)) {
            //We found ID node .. so we have to reuse
            wfId = extractWorkflowId(node, transformationTemplate);
            wf = workflowProvider.get()
                    .getWorkflow(wfId)
                    .orElse(null);
            Preconditions.checkNotNull(wf);
            wfTemplate = workflowProvider.get()
                    .getTemplate(wf.getTemplateId())
                    .orElse(null);
            Preconditions.checkNotNull(wfTemplate);
        }
        else {
            //First time .. create workflow
            wfTemplate = templateSelector.get()
                    .determineTemplate(context)
                    .orElse(null);
            if (null == wfTemplate) {
                throw new StatesmanError("No matching workflow template found for context: " + stdPayload,
                                         ResponseCode.INVALID_OPERATION);
            }
            wfId = UUID.randomUUID().toString();
            workflowProvider.get()
                    .saveWorkflow(new Workflow(wfId, wfTemplate.getId(),
                                               new DataObject(mapper.createObjectNode(),
                                                              wfTemplate.getStartState(),
                                                              date,
                                                              date)));
            wf = workflowProvider.get()
                    .getWorkflow(wfId)
                    .orElse(null);
            Preconditions.checkNotNull(wf);
        }
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(new DataUpdate(wfId, node, new MergeDataAction()));
        log.debug("Workflow: {} with template: {} went through transitions: {}",
                  wfId, wfTemplate.getId(), appliedTransitions.getTransitions());
        return Response.ok()
                .build();
    }

    private String extractWorkflowId(JsonNode node, TransformationTemplate transformationTemplate) {
        val wfIdNode = node.at(transformationTemplate.getIdPath());
        return !isValid(node)
               ? UUID.randomUUID().toString()
               : wfIdNode.asText();
    }

    private boolean isValid(final JsonNode node) {
        return node != null
                && !node.isNull()
                && !node.isMissingNode();
    }

    final StepByStepTransformationTemplate.StepSelection selectStep(
            JsonNode node,
            StepByStepTransformationTemplate template) {
        return template.getTemplates()
                .stream()
                .filter(tmpl -> hopeLangEngine.evaluate(tmpl.getSelectionRule(), node))
                .findFirst()
                .orElse(null);
    }
}
