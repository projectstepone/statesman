package io.appform.statesman.engine.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.statesman.model.Workflow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Provider;
import java.util.List;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ActionHelper {

    private final ObjectMapper mapper;
    private final Provider<ActionExecutor> actionExecutor;

    public ObjectNode executeActions(List<String> actionTemplates, Workflow workflow) {
        val response = mapper.createObjectNode();

        actionTemplates
                .forEach(actionId ->
                        actionExecutor.get()
                                .execute(actionId, workflow)
                                .filter(jsonNode -> !jsonNode.isNull() && !jsonNode.isMissingNode())
                                .ifPresent(jsonNode -> response.setAll((ObjectNode) jsonNode)));
        return response;
    }
}
