package io.appform.statesman.engine.action.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import io.appform.eventingester.client.EventPublisher;
import io.appform.statesman.engine.action.BaseAction;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.engine.http.HttpClient;
import io.appform.statesman.engine.http.HttpUtil;
import io.appform.statesman.model.ActionImplementation;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.template.HttpFormActionTemplate;
import io.appform.statesman.model.exception.StatesmanError;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import okhttp3.Response;

@Slf4j
@Data
@Singleton
@ActionImplementation(name = "HTTP_FORM")
public class HttpFormAction extends BaseAction<HttpFormActionTemplate> {

    private static final String APPLICATION_JSON = "application/json";
    private HandleBarsService handleBarsService;
    private Provider<HttpClient> client;

    @Inject
    public HttpFormAction(
            HandleBarsService handleBarsService,
            Provider<HttpClient> client,
            @Named("eventPublisher") final EventPublisher publisher,
            ObjectMapper mapper) {
        super(publisher, mapper);
        this.client = client;
        this.handleBarsService = handleBarsService;
    }

    @Override
    public ActionType getType() {
        return ActionType.HTTP_FORM;
    }

    @Override
    public JsonNode execute(HttpFormActionTemplate actionTemplate, Workflow workflow) {

        log.debug("Http Form Action triggered with Template: {} and Workflow: {}",
            actionTemplate, workflow);

        val responseTranslator = actionTemplate.getResponseTranslator();
        val httpFormActionData = transformPayload(workflow, actionTemplate);
        log.debug("Action call data: {}", httpFormActionData);

        if (actionTemplate.isNoop()) {
            log.warn("Returning noop response as NoOp is configured in template");
            return actionTemplate.getNoopResponse();
        }

        val httpResponse = handle(httpFormActionData);
        if (httpResponse != null && !Strings.isNullOrEmpty(responseTranslator)) {
            return toJsonNode(handleBarsService.transform(responseTranslator, httpResponse));
        }
        return null;
    }

    private JsonNode handle(HttpFormActionData actionData) {
        try(Response httpResponse = executeRequest(actionData) ) {
            val responseBodyStr = HttpUtil.body(httpResponse);
            if (Strings.isNullOrEmpty(responseBodyStr)) {
                return NullNode.getInstance();
            }
            log.debug("HTTP Response: {}", responseBodyStr);
            val contentType = Arrays.stream(
                                                httpResponse.header("Content-Type",APPLICATION_JSON)
                                                .split(";"))
                                                .collect(Collectors.toList());
            if (contentType.stream()
                    .anyMatch(value -> value.equalsIgnoreCase(APPLICATION_JSON))) {
                return toJsonNode(responseBodyStr);
            }
            return mapper.createObjectNode()
                    .put("payload", responseBodyStr);
        } catch (final Exception e) {
            throw StatesmanError.propagate(e);
        }
    }

    @SneakyThrows
    private Response executeRequest(HttpFormActionData actionData) {

        log.info("HTTP_ACTION POST Call url:{}", actionData.getUrl());

        val response = client.get()
            .formUrlEncoded(actionData.getUrl(), actionData.getFormParams(),
                actionData.getHeaders());

        if (!isSuccessful(response, actionData)) {
            log.error("unable to do post action, actionData: {} Response: {}",
                actionData, HttpUtil.body(response));
            throw new StatesmanError();
        }

        return response;
    }

    private boolean isSuccessful(Response response, HttpFormActionData actionData) {
        return (actionData.getAcceptableCodes() != null
                && actionData.getAcceptableCodes().contains(response.code()))
                || response.isSuccessful();
    }

    private JsonNode toJsonNode(String responseBodyStr) {
        try {
            return mapper.readTree(responseBodyStr);
        } catch (Exception e) {
            log.error("Error while converting to json:" + responseBodyStr, e);
            return null;
        }
    }

    private HttpFormActionData transformPayload(Workflow workflow, HttpFormActionTemplate actionTemplate) {
        val jsonNode = mapper.valueToTree(workflow);
        return HttpFormActionData.builder()
                .url(handleBarsService.transform(actionTemplate.getUrl(), jsonNode))
                .headers(getheaders(jsonNode, actionTemplate.getHeaders()))
                .formParams(getFormParams(jsonNode, actionTemplate.getFormParams()))
                .acceptableCodes(actionTemplate.getAcceptableCodes())
                .build();
    }

    //assuming the header string in below format
    //headerStr = "key1:value1,key2:value2"
    private Map<String, String> getheaders(JsonNode workflow, String headers) {
        if (Strings.isNullOrEmpty(headers)) {
            return Collections.emptyMap();
        }
        return Splitter.on(",")
                .withKeyValueSeparator(":")
                .split(handleBarsService.transform(headers, workflow));
    }

    private Map<String, String> getFormParams(JsonNode workflow, Map<String, String> formParams) {
        if (Objects.isNull(formParams) || formParams.isEmpty()) {
            return Map.of();
        }
        return formParams.entrySet().stream()
            .collect(Collectors.toMap(
                e -> getFormParamValue(workflow, e.getKey()),
                e -> getFormParamValue(workflow, e.getValue()),
                (a, b) -> b
            ));
    }

    private String getFormParamValue(JsonNode workflow, String formValue) {
        val transformFormValue = handleBarsService.transform(formValue, workflow);
        return Objects.isNull(transformFormValue) ? "" : transformFormValue;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class HttpFormActionData {

        private String url;
        private Map<String, String> formParams;
        private Map<String, String> headers;
        private Set<Integer> acceptableCodes;
    }
}
