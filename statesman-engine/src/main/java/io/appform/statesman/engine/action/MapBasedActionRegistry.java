package io.appform.statesman.engine.action;

import io.appform.statesman.engine.action.impl.*;
import io.appform.statesman.model.Action;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@Singleton
public class MapBasedActionRegistry implements ActionRegistry {

    private Map<String, Action> registry;

    @Inject
    public MapBasedActionRegistry(HttpAction httpAction,
                                  HttpFilePipedAction httpFilePipedAction,
                                  HttpFormAction httpFormAction,
                                  CompoundAction compoundAction,
                                  RoutedAction routedAction,
                                  TranslatorAction translatorAction,
                                  EvaluatedAction evaluatedAction) {
        registry = new ConcurrentHashMap<>();
        register(httpAction);
        register(httpFilePipedAction);
        register(httpFormAction);
        register(compoundAction);
        register(routedAction);
        register(translatorAction);
        register(evaluatedAction);
    }

    @Override
    public void register(Action action) {
        registry.put(action.getType().name(), action);
    }

    @Override
    public Optional<Action> get(String id) {
        return Optional.ofNullable(registry.get(id));
    }

}
