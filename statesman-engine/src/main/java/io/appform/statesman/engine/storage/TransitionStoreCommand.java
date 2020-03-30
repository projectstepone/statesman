package io.appform.statesman.engine.storage;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.statesman.engine.TransitionStore;
import io.appform.statesman.engine.storage.data.StoredStateTransition;
import io.appform.statesman.engine.util.WorkflowUtils;
import io.appform.statesman.model.StateTransition;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class TransitionStoreCommand implements TransitionStore {


    private final RelationalDao<StoredStateTransition> stateTransitionRelationalDao;
    private final LoadingCache<StateTransitionCacheKey, List<StateTransition>> TRANSITION_CACHE;

    @Inject
    public TransitionStoreCommand(RelationalDao<StoredStateTransition> storedWorkflowTemplateRelationalDao) {
        this.stateTransitionRelationalDao = storedWorkflowTemplateRelationalDao;
        log.info("Initializing cache TRANSITION_CACHE");
        TRANSITION_CACHE = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(300, TimeUnit.SECONDS)
                .refreshAfterWrite(60, TimeUnit.SECONDS)
                .build(key -> {
                    log.debug("Loading data for transition for key: {}", key);
                    return getTransitionsFromDb(key.getWorkflowTemplateId(), key.getFromState());
                });
    }

    @Override
    public Optional<StateTransition> save(String workflowTemplateId, String fromState, StateTransition stateTransition) {
        try {
            StoredStateTransition storedStateTransition = WorkflowUtils.toDao(workflowTemplateId, fromState, stateTransition);
            return stateTransitionRelationalDao.save(workflowTemplateId, storedStateTransition)
                    .map(WorkflowUtils::toDto);
        }  catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public List<StateTransition> getTransitionFor(String workflowTemplateId, String fromState) {
        return TRANSITION_CACHE.get(stateTransitionCacheKey(workflowTemplateId, fromState));
    }

    @Override
    public List<StateTransition> allTransitions(String workflowTemplateId) {
        try {
            DetachedCriteria detachedCriteria = DetachedCriteria.forClass(StoredStateTransition.class)
                    .add(Restrictions.eq("workflowTemplateId", workflowTemplateId));
            return stateTransitionRelationalDao.select(workflowTemplateId, detachedCriteria, 0, Integer.MAX_VALUE)
                    .stream()
                    .map(WorkflowUtils::toDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }


    private List<StateTransition> getTransitionsFromDb(String workflowTemplateId, String fromState) {
        try {
            DetachedCriteria detachedCriteria = DetachedCriteria.forClass(StoredStateTransition.class)
                    .add(Restrictions.eq("workflowTemplateId", workflowTemplateId))
                    .add(Restrictions.eq("fromState", fromState));
            return stateTransitionRelationalDao.select(workflowTemplateId, detachedCriteria, 0, Integer.MAX_VALUE)
                    .stream()
                    .map(WorkflowUtils::toDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    private StateTransitionCacheKey stateTransitionCacheKey(String workflowTemplateId, String fromState) {
        return StateTransitionCacheKey.builder()
                .workflowTemplateId(workflowTemplateId)
                .fromState(fromState)
                .build();
    }

    @Data
    @Builder
    private static class StateTransitionCacheKey {
        String workflowTemplateId;
        String fromState;

    }
}
