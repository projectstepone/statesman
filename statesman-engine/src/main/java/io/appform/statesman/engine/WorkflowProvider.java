package io.appform.statesman.engine;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.WorkflowTemplate;

import java.util.Optional;
import java.util.Set;

/**
 *
 */
public interface WorkflowProvider {
    Optional<WorkflowTemplate> createTemplate(WorkflowTemplate workflowTemplate);

    Optional<WorkflowTemplate> updateTemplate(WorkflowTemplate workflowTemplate);

    Optional<WorkflowTemplate> getTemplate(String workflowTemplateId);

    Set<WorkflowTemplate> getAll();

    Optional<Workflow> createWorkflow(String templateId, JsonNode initialData);

    Optional<Workflow> getWorkflow(String workflowId);

    void saveWorkflow(final Workflow workflow);

    void updateWorkflow(final Workflow workflow);

    boolean workflowExists(final String workflowId);

}
