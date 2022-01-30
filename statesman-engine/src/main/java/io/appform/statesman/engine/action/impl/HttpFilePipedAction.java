package io.appform.statesman.engine.action.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.appform.eventingester.client.EventPublisher;
import io.appform.statesman.engine.action.BaseAction;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.engine.http.HttpClient;
import io.appform.statesman.engine.http.HttpUtil;
import io.appform.statesman.model.ActionImplementation;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.template.HttpFilePipedActionTemplate;
import io.appform.statesman.model.exception.StatesmanError;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Data
@Singleton
@ActionImplementation(name = "HTTP_FILE_PIPED")
public class HttpFilePipedAction extends BaseAction<HttpFilePipedActionTemplate> {

    private static final String APPLICATION_JSON = "application/json";
    private HandleBarsService handleBarsService;
    private Provider<HttpClient> client;

    @Inject
    public HttpFilePipedAction(HandleBarsService handleBarsService,
                               Provider<HttpClient> client,
                               @Named("eventPublisher") final EventPublisher publisher,
                               ObjectMapper mapper) {
        super(publisher, mapper);
        this.client = client;
        this.handleBarsService = handleBarsService;
    }

    @Override
    protected JsonNode execute(HttpFilePipedActionTemplate httpFilePipedActionTemplate, Workflow workflow) {
        log.debug("Http File Piped Action triggered with Template: {} and Workflow: {}",
                httpFilePipedActionTemplate, workflow);

        val responseTranslator = httpFilePipedActionTemplate.getResponseTranslator();
        val httpActionData = transformSourcePayload(workflow, httpFilePipedActionTemplate);
        log.debug("Action call data: {}", httpActionData);

        if (httpFilePipedActionTemplate.isNoop()) {
            log.warn("Returning noop response as NoOp is configured in template");
            return httpFilePipedActionTemplate.getNoopResponse();
        }

        val file = (byte[]) fetchFile(httpActionData);
        val httpTargetActionData
                = transformTargetPayload(workflow, httpFilePipedActionTemplate, file);
        val httpResponse = handle(httpTargetActionData);
        if (httpResponse != null && !Strings.isNullOrEmpty(responseTranslator)) {
            return toJsonNode(handleBarsService.transform(responseTranslator, httpResponse));
        }
        return null;
    }

    private Object fetchFile(HttpActionData actionData) {
        try(val httpResponse = executeRequest(actionData) ) {
            return Objects.requireNonNull(httpResponse.body()).bytes();
        } catch (final Exception e) {
            throw StatesmanError.propagate(e);
        }
    }

    private JsonNode handle(HttpActionData actionData) {
        try(val httpResponse = executeRequest(actionData) ) {
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
    private Response executeRequest(HttpActionData actionData) {
        return actionData.getMethod().visit(new HttpMethod.MethodTypeVisitor<Response>() {
            private final Map<String, String> headers = actionData.getHeaders();
            private final String url = actionData.getUrl();

            @Override
            public Response visitPost() throws Exception {
                log.info("HTTP_ACTION POST Call url:{}", url);
                val payload = actionData.getPayload();
                val response = client.get().post(url, payload, headers);
                if (!isSuccessful(response, actionData)) {
                    log.error("unable to do post action, actionData: {} Response: {}",
                            actionData, HttpUtil.body(response));
                    throw new StatesmanError();
                }
                return response;
            }

            @Override
            public Response visitGet() throws Exception {
                log.info("HTTP_ACTION GET Call url:{}", url);
                val response = client.get().get(url, headers);
                if (!isSuccessful(response, actionData)) {
                    log.error("unable to do get action, actionData: {} Response: {}",
                            actionData, HttpUtil.body(response));
                    throw new StatesmanError();
                }
                return response;
            }

            @Override
            public Response visitForm() throws Exception {
                log.info("HTTP_ACTION FORM Call url:{}", url);
                val payload = actionData.getPayload();
                val file = actionData.getFile();
                val fileMeta = actionData.getFileMeta();
                val response = client.get().form(url, file, payload, fileMeta, headers);
                if (!isSuccessful(response, actionData)) {
                    log.error("unable to do form action, actionData: {} Response: {}",
                            actionData, HttpUtil.body(response));
                    throw new StatesmanError();
                }
                return response;
            }
        });
    }

    private boolean isSuccessful(Response response, HttpActionData actionData) {
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

    private HttpActionData transformTargetPayload(Workflow workflow, HttpFilePipedActionTemplate httpFilePipedActionTemplate, byte[] file) {
        val jsonNode = mapper.valueToTree(workflow);
        try {
            return HttpActionData.builder()
                    .method(HttpMethod.valueOf(httpFilePipedActionTemplate.getTargetMethod()))
                    .url(handleBarsService.transform(httpFilePipedActionTemplate.getTargetUrl(), jsonNode))
                    .headers(getheaders(jsonNode, httpFilePipedActionTemplate.getTargetHeaders()))
                    .fileMeta(getheaders(jsonNode, httpFilePipedActionTemplate.getFileMeta()))
                    .payload(mapper.readValue(handleBarsService.transform(httpFilePipedActionTemplate.getTargetPayload(), jsonNode), Map.class))
                    .acceptableCodes(httpFilePipedActionTemplate.getTargetAcceptableCodes())
                    .file(file)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private HttpActionData transformSourcePayload(Workflow workflow, HttpFilePipedActionTemplate httpFilePipedActionTemplate) {
        val jsonNode = mapper.valueToTree(workflow);
        return HttpActionData.builder()
                .method(HttpMethod.valueOf(httpFilePipedActionTemplate.getSourceMethod()))
                .url(handleBarsService.transform(httpFilePipedActionTemplate.getSourceUrl(), jsonNode))
                .headers(getheaders(jsonNode, httpFilePipedActionTemplate.getSourceHeaders()))
                .acceptableCodes(httpFilePipedActionTemplate.getSourceAcceptableCodes())
                .build();
    }

    private Map<String, String> getheaders(JsonNode workflow, String headers) {
        if (Strings.isNullOrEmpty(headers)) {
            return Collections.emptyMap();
        }
        return Splitter.on(",")
                .withKeyValueSeparator(":")
                .split(handleBarsService.transform(headers, workflow));
    }

    @Override
    public ActionType getType() {
        return ActionType.HTTP_FILE_PIPED;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class HttpActionData {

        private HttpMethod method;
        private String url;
        private Map<String, String> payload;
        private Map<String, String> fileMeta;
        private byte[] file;
        private Map<String, String> headers;
        private Set<Integer> acceptableCodes;

    }

    private enum HttpMethod {

        POST {
            @Override
            public <T> T visit(final MethodTypeVisitor<T> visitor) throws Exception {
                return visitor.visitPost();
            }
        },

        GET {
            @Override
            public <T> T visit(final MethodTypeVisitor<T> visitor) throws Exception {
                return visitor.visitGet();
            }
        },

        FORM {
            @Override
            public <T> T visit(final MethodTypeVisitor<T> visitor) throws Exception {
                return visitor.visitForm();
            }
        };

        public abstract <T> T visit(final MethodTypeVisitor<T> visitor) throws Exception;

        /**
         * Visitor
         *
         * @param <T>
         */
        public interface MethodTypeVisitor<T> {
            T visitPost() throws Exception;

            T visitGet() throws Exception;

            T visitForm() throws Exception;
        }
    }
}
