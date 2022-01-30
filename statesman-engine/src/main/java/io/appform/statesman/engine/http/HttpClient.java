package io.appform.statesman.engine.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;

/**
 * @author shashank.g
 */
@Slf4j
@AllArgsConstructor
public class HttpClient {

    private static final MediaType APPLICATION_JSON = MediaType.parse("application/json");
    private static final MediaType APPLICATION_PDF = MediaType.parse("application/pdf");

    public final ObjectMapper mapper;
    public final OkHttpClient client;

    public Response post(String url,
                         final Object payload,
                         final Map<String, String> headers) throws IOException {
        val httpUrl = HttpUrl.get(url);
        Request.Builder postBuilder;
        if(payload instanceof String) {
             postBuilder =  new Request.Builder()
                     .url(httpUrl)
                     .post(RequestBody.create(APPLICATION_JSON, (String)payload));
        }
        else {
            postBuilder = new Request.Builder()
                    .url(httpUrl)
                    .post(RequestBody.create(APPLICATION_JSON, mapper.writeValueAsBytes(payload)));
        }
        if (headers != null) {
            headers.forEach(postBuilder::addHeader);
        }
        val request = postBuilder.build();
        return client.newCall(request).execute();
    }

    public Response get(final String url,
                        final Map<String, String> headers) throws IOException {
        val httpUrl = HttpUrl.get(url);
        val getBuilder = new Request.Builder()
                .url(httpUrl)
                .get();
        if (headers != null) {
            headers.forEach(getBuilder::addHeader);
        }
        val request = getBuilder.build();
        return client.newCall(request).execute();
    }

    public Response form(final String url, final byte[] file,
                              final Map<String, String> payload,
                              final Map<String, String> fileMeta,
                              final Map<String, String> headers) throws IOException {
        val httpUrl = HttpUrl.get(url);
        val formBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        if(payload != null) {
            payload.forEach(formBuilder::addFormDataPart);
        }
        if(fileMeta != null) {
            for(String key : fileMeta.keySet())
                formBuilder.addFormDataPart(key, fileMeta.get(key),
                        RequestBody.create(APPLICATION_PDF, file));
        }
        val postBuilder = new Request.Builder()
                .url(httpUrl)
                .post(formBuilder.build());
        if (headers != null) {
            headers.forEach(postBuilder::addHeader);
        }
        val request = postBuilder.build();
        return client.newCall(request).execute();
    }
}
