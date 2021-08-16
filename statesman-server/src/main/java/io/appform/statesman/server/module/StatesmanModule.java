package io.appform.statesman.server.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.appform.eventingester.client.EventPublisher;
import io.appform.eventingester.client.EventPublishers;
import io.appform.statesman.engine.ActionTemplateStore;
import io.appform.statesman.engine.ProviderSelector;
import io.appform.statesman.engine.TransitionStore;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.engine.action.ActionExecutor;
import io.appform.statesman.engine.action.ActionExecutorImpl;
import io.appform.statesman.engine.action.ActionRegistry;
import io.appform.statesman.engine.action.MapBasedActionRegistry;
import io.appform.statesman.engine.http.HttpClient;
import io.appform.statesman.engine.http.HttpUtil;
import io.appform.statesman.engine.observer.ObservableEventBus;
import io.appform.statesman.engine.observer.ObservableEventBusSubscriber;
import io.appform.statesman.engine.observer.ObservableGuavaEventBus;
import io.appform.statesman.engine.observer.observers.FoxtrotEventSender;
import io.appform.statesman.model.FoxtrotClientConfig;
import io.appform.statesman.model.HttpClientConfiguration;
import io.appform.statesman.server.AppConfig;
import io.appform.statesman.server.dao.action.ActionTemplateStoreCommand;
import io.appform.statesman.server.dao.callback.CallbackTemplateProvider;
import io.appform.statesman.server.dao.callback.CallbackTemplateProviderCommand;
import io.appform.statesman.server.dao.transition.TransitionStoreCommand;
import io.appform.statesman.server.dao.workflow.WorkflowProviderCommand;
import io.appform.statesman.server.droppedcalldetector.DroppedCallDetector;
import io.appform.statesman.server.droppedcalldetector.HopeRuleDroppedCallDetector;
import io.appform.statesman.server.idextractor.CompoundIdExtractor;
import io.appform.statesman.server.idextractor.IdExtractor;
import io.appform.statesman.server.provider.ProviderSelectorImpl;
import io.dropwizard.setup.Environment;

public class StatesmanModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActionTemplateStore.class).to(ActionTemplateStoreCommand.class);
        bind(TransitionStore.class).to(TransitionStoreCommand.class);
        bind(WorkflowProvider.class).to(WorkflowProviderCommand.class);
        bind(CallbackTemplateProvider.class).to(CallbackTemplateProviderCommand.class);
        bind(ActionRegistry.class).to(MapBasedActionRegistry.class);
        bind(ProviderSelector.class).to(ProviderSelectorImpl.class);
        bind(ObservableEventBus.class).to(ObservableGuavaEventBus.class);
        bind(DroppedCallDetector.class).to(HopeRuleDroppedCallDetector.class);
        bind(ActionExecutor.class).to(ActionExecutorImpl.class);
        bind(ObservableEventBusSubscriber.class)
                .annotatedWith(Names.named("foxtrotEventSender"))
                .to(FoxtrotEventSender.class);
        bind(IdExtractor.class).to(CompoundIdExtractor.class);
    }

    @Singleton
    @Provides
    @Named("eventPublisher")
    public EventPublisher provideEventPublisher(
            AppConfig appConfig,
            Environment environment) {
        return EventPublishers.create(appConfig.getEventPublisherConfig(), environment.getObjectMapper());
    }

    @Singleton
    @Provides
    @Named("httpActionDefaultConfig")
    public HttpClientConfiguration provideHttpActionDefaultConfig(AppConfig config) {
        return config.getHttpActionDefaultConfig();
    }

    @Provides
    @Singleton
    public HttpClient httpClient(Environment environment, AppConfig appConfig) {
        return new HttpClient(environment.getObjectMapper(),
                              HttpUtil.defaultClient("common-http",
                                                     environment.metrics(),
                                                     appConfig.getHttpActionDefaultConfig()));
    }

    @Provides
    @Singleton
    public FoxtrotClientConfig foxtrotClientConfig(AppConfig appConfig) {
        return appConfig.getFoxtrot();
    }
}
