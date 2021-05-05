package io.appform.statesman.server.dao.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.statesman.engine.MessageConfigStore;
import io.appform.statesman.model.MessageConfig;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.server.utils.MapperUtils;
import io.appform.statesman.server.utils.WorkflowUtils;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for storing and accessing message config from cache backed by DB
 */
@Slf4j
@Singleton
public class MessageConfigStoreCommand implements MessageConfigStore {

    private final LookupDao<StoredMessageConfig> messageLookupDao;
    private final LoadingCache<String, Optional<MessageConfig>> messageConfigCache;

    @Inject
    public MessageConfigStoreCommand(LookupDao<StoredMessageConfig> messageLookupDao) {
        this.messageLookupDao = messageLookupDao;
        messageConfigCache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(120, TimeUnit.MINUTES)
                .refreshAfterWrite(15, TimeUnit.MINUTES)
                .build(key -> {
                    log.debug("Loading data for action for key: {}", key);
                    return getFromDb(key);
                });
    }

    private Optional<MessageConfig> getFromDb(String messageId) {
        try {
            return messageLookupDao.get(messageId)
                    .map(config ->
                            new MessageConfig(config.getMessageId(), MapperUtils.readTree(config.getMessageConfigBody())));
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }


    @Override
    public Optional<MessageConfig> create(MessageConfig messageConfig) {
        try {
            return messageLookupDao
                    .save(WorkflowUtils.toDao(messageConfig))
                    .map(config ->
                            new MessageConfig(config.getMessageId(), MapperUtils.readTree(config.getMessageConfigBody())));

        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public Optional<MessageConfig> get(String messageConfigId) {
        try {
            return messageConfigCache.get(messageConfigId);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }
}
