package io.appform.statesman.engine;

import io.appform.statesman.model.MessageConfig;
import io.appform.statesman.model.action.template.ActionTemplate;

import java.util.Optional;

public interface MessageConfigStore {

    Optional<MessageConfig> create(MessageConfig messageConfig);

    Optional<MessageConfig> get(String messageConfigId);
}
