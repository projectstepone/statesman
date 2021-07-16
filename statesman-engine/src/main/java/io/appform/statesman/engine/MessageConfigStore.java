package io.appform.statesman.engine;

import io.appform.statesman.model.MessageConfig;
import io.appform.statesman.model.action.template.ActionTemplate;

import java.util.Optional;

/**
 * Interface for storing and accessing message config from cache backed by DB
 */
public interface MessageConfigStore {

    Optional<MessageConfig> create(MessageConfig messageConfig);

    Optional<MessageConfig> get(String messageConfigId);
}
