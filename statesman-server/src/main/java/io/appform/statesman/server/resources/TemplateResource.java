package io.appform.statesman.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.statesman.engine.ActionTemplateStore;
import io.appform.statesman.engine.MessageConfigStore;
import io.appform.statesman.engine.TransitionStore;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.model.MessageConfig;
import io.appform.statesman.model.StateTransition;
import io.appform.statesman.model.WorkflowTemplate;
import io.appform.statesman.model.action.template.ActionTemplate;
import io.appform.statesman.server.dao.message.IMessageConstructor;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Produces(MediaType.APPLICATION_JSON)
@Path("/v1/templates")
@Slf4j
@Api("Template related APIs")
@Singleton
public class TemplateResource {

    private final ActionTemplateStore actionTemplateStore;
    private final TransitionStore transitionStore;
    private final WorkflowProvider workflowProvider;
    private final MessageConfigStore messageConfigStore;
    private final IMessageConstructor messageConstrutor;

    @Inject
    public TemplateResource(final ActionTemplateStore actionTemplateStore,
                            final TransitionStore transitionStore,
                            final WorkflowProvider workflowProvider,
                            final MessageConfigStore messageConfigStore,
                            final IMessageConstructor messageConstrutor) {
        this.actionTemplateStore = actionTemplateStore;
        this.transitionStore = transitionStore;
        this.workflowProvider = workflowProvider;
        this.messageConfigStore = messageConfigStore;
        this.messageConstrutor = messageConstrutor;
    }


    @POST
    @Timed
    @Path("/workflow")
    @ApiOperation("Create Workflow Template")
    public Response createWorkflow(@Valid WorkflowTemplate workflowTemplate) {
        workflowTemplate.setId(null);
        Optional<WorkflowTemplate> workflowTemplateOptional =
                workflowProvider.createTemplate(workflowTemplate);

        if (!workflowTemplateOptional.isPresent()) {
            return Response.serverError()
                    .build();
        }
        return Response.ok()
                .entity(workflowTemplateOptional.get())
                .build();
    }

    @GET
    @Timed
    @Path("/workflow")
    @ApiOperation("Get all templates")
    public Response getAll() {

        Set<WorkflowTemplate> templates = workflowProvider.getAll();
        if (templates.isEmpty()) {
            return Response.noContent()
                    .build();
        }
        return Response.ok()
                .entity(templates)
                .build();
    }

    @GET
    @Timed
    @Path("/workflow/{templateId}")
    @ApiOperation("Get Workflow Template")
    public Response getWorkflow(@PathParam("templateId") String templateId) {
        Optional<WorkflowTemplate> workflowTemplateOptional =
                workflowProvider.getTemplate(templateId);

        if (!workflowTemplateOptional.isPresent()) {
            return Response.noContent()
                    .build();
        }
        return Response.ok()
                .entity(workflowTemplateOptional.get())
                .build();
    }

    @PUT
    @Timed
    @Path("/workflow")
    @ApiOperation("Update Workflow Template")
    public Response updateWorkflow(@Valid WorkflowTemplate workflowTemplate) {
        Optional<WorkflowTemplate> workflowTemplateOptional =
                workflowProvider.updateTemplate(workflowTemplate);

        if (!workflowTemplateOptional.isPresent()) {
            return Response.serverError()
                    .build();
        }
        return Response.ok()
                .entity(workflowTemplateOptional.get())
                .build();
    }


    @POST
    @Timed
    @Path("/workflow/{workflowTemplateId}/transitions")
    @ApiOperation("Create State Transitions")
    public Response createStateTransitions(@PathParam("workflowTemplateId") String workflowTemplateId,
                                          @Valid List<StateTransition> stateTransitions) {
        List<StateTransition> savedStateTransition = stateTransitions.stream().map(stateTransition ->
                transitionStore.create(workflowTemplateId, stateTransition))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        return Response.ok()
                .entity(savedStateTransition)
                .build();
    }


    @GET
    @Timed
    @Path("/workflow/{workflowTemplateId}/transitions/{fromState}")
    @ApiOperation("Get State Transition")
    public Response getStateTransition(@PathParam("workflowTemplateId") String workflowTemplateId,
                                       @PathParam("fromState") String fromState) {
        List<StateTransition> stateTransitions = transitionStore.getTransitionFor(workflowTemplateId, fromState);
        return Response.ok()
                .entity(stateTransitions)
                .build();
    }

    @GET
    @Timed
    @Path("/workflow/{workflowTemplateId}/transitions")
    @ApiOperation("Get All State  Transition")
    public Response getAllStateTransitions(@PathParam("workflowTemplateId") String workflowTemplateId,
                                           @DefaultValue("true") @QueryParam("onlyActive") boolean onlyActive) {
        List<StateTransition> stateTransitions = transitionStore.getAllTransitions(workflowTemplateId);
        return Response.ok()
                .entity(onlyActive ? stateTransitions.stream().filter(StateTransition::isActive).collect(Collectors.toList()) : stateTransitions)
                .build();
    }

    @PUT
    @Timed
    @Path("/workflow/{workflowTemplateId}/transitions")
    @ApiOperation("Update State Transition")
    public Response updateStateTransition(@PathParam("workflowTemplateId") String workflowTemplateId,
                                          @Valid StateTransition stateTransition) {
        List<StateTransition> stateTransitions = transitionStore.update(workflowTemplateId, stateTransition);
        return Response.ok()
                .entity(stateTransitions)
                .build();
    }

    @POST
    @Timed
    @Path("/action")
    @ApiOperation("Create Action Template")
    public Response createAction(@Valid ActionTemplate actionTemplate) {
        Optional<ActionTemplate> actionTemplateOptional = actionTemplateStore.create(actionTemplate);
        if (!actionTemplateOptional.isPresent()) {
            return Response.serverError()
                    .build();
        }
        return Response.ok()
                .entity(actionTemplateOptional.get())
                .build();
    }


    @GET
    @Timed
    @Path("/action/get/all")
    @ApiOperation("Get All Action Template")
    public Response allAction() {
        return Response.ok()
                .entity(actionTemplateStore.all())
                .build();
    }

    @GET
    @Timed
    @Path("/action/{templateId}")
    @ApiOperation("Get Action Template")
    public Response createAction(@PathParam("templateId") String templateId) {
        Optional<ActionTemplate> actionTemplateOptional = actionTemplateStore.get(templateId);
        if (!actionTemplateOptional.isPresent()) {
            return Response.serverError()
                    .build();
        }
        return Response.ok()
                .entity(actionTemplateOptional.get())
                .build();
    }

    @PUT
    @Timed
    @Path("/action")
    @ApiOperation("Update Action Template")
    public Response updateAction(@Valid ActionTemplate actionTemplate) {
        Optional<ActionTemplate> actionTemplateOptional = actionTemplateStore.update(actionTemplate);
        if (!actionTemplateOptional.isPresent()) {
            return Response.serverError()
                    .build();
        }
        return Response.ok()
                .entity(actionTemplateOptional.get())
                .build();
    }

    @POST
    @Timed
    @Path("/messageconfig")
    @ApiOperation("Create Message Config")
    public Response createMessageConfig(@Valid MessageConfig messageConfig) {
        Optional<MessageConfig> messageConfigOptional = messageConfigStore.create(messageConfig);
        if (!messageConfigOptional.isPresent()) {
            return Response.serverError()
                    .build();
        }
        return Response.ok()
                .entity(messageConfigOptional.get())
                .build();
    }

    @GET
    @Timed
    @Path("/messageconfig/{messageId}")
    @ApiOperation("Get message config")
    public Response getAllStateTransitions(@PathParam("messageId") String messageId,
                                           @DefaultValue("default") @QueryParam("language") String language,
                                           @DefaultValue("default") @QueryParam("state") String state) {

        return Response.ok()
                .entity(messageConstrutor.constructMessage(messageId,language,state))
                .build();
    }

}
