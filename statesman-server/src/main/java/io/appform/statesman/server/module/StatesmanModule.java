package io.appform.statesman.server.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.appform.statesman.engine.*;
import io.appform.statesman.engine.action.ActionExecutor;
import io.appform.statesman.engine.action.ActionExecutorImpl;
import io.appform.statesman.engine.action.ActionRegistry;
import io.appform.statesman.engine.action.MapBasedActionRegistry;
import io.appform.statesman.engine.observer.ObservableEventBus;
import io.appform.statesman.engine.observer.ObservableEventBusSubscriber;
import io.appform.statesman.engine.observer.ObservableGuavaEventBus;
import io.appform.statesman.engine.observer.observers.FoxtrotEventSender;
import io.appform.statesman.model.FoxtrotClientConfig;
import io.appform.statesman.model.HttpClientConfiguration;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.http.HttpClient;
import io.appform.statesman.publisher.http.HttpUtil;
import io.appform.statesman.publisher.impl.QueueEventPublisher;
import io.appform.statesman.publisher.impl.SyncEventPublisher;
import io.appform.statesman.publisher.model.PublisherType;
import io.appform.statesman.server.AppConfig;
import io.appform.statesman.server.dao.action.ActionTemplateStoreCommand;
import io.appform.statesman.server.dao.callback.CallbackTemplateProvider;
import io.appform.statesman.server.dao.callback.CallbackTemplateProviderCommand;
import io.appform.statesman.server.dao.message.IMessageConstructor;
import io.appform.statesman.server.dao.message.MessageConfigStoreCommand;
import io.appform.statesman.server.dao.message.MessageConstructor;
import io.appform.statesman.server.dao.transition.TransitionStoreCommand;
import io.appform.statesman.server.dao.workflow.WorkflowProviderCommand;
import io.appform.statesman.server.droppedcalldetector.DroppedCallDetector;
import io.appform.statesman.server.droppedcalldetector.HopeRuleDroppedCallDetector;
import io.appform.statesman.server.idextractor.CompoundIdExtractor;
import io.appform.statesman.server.idextractor.IdExtractor;
import io.appform.statesman.server.provider.ProviderSelectorImpl;
import io.dropwizard.setup.Environment;

import java.io.IOException;

public class StatesmanModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActionTemplateStore.class).to(ActionTemplateStoreCommand.class);
        bind(MessageConfigStore.class).to(MessageConfigStoreCommand.class);
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
        bind(IMessageConstructor.class).to(MessageConstructor.class);
    }

    @Singleton
    @Provides
    @Named("eventPublisher")
    public EventPublisher provideEventPublisher(
            AppConfig appConfig,
            Environment environment) {

        return appConfig.getEventPublisherConfig().getType().visit(
                new PublisherType.PublisherTypeVisitor<EventPublisher>() {
                    @Override
                    public EventPublisher visitSync() {
                        return new SyncEventPublisher(
                                environment.getObjectMapper(),
                                appConfig.getEventPublisherConfig(),
                                environment.metrics()
                        );
                    }

                    @Override
                    public EventPublisher visitQueued() {
                        try {
                            return new QueueEventPublisher(
                                    environment.getObjectMapper(),
                                    appConfig.getEventPublisherConfig(),
                                    environment.metrics());
                        } catch (IOException e) {
                            throw StatesmanError.propagate(e);
                        }
                    }
                });
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
