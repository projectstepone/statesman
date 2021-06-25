package io.appform.statesman.publisher.http;

import com.codahale.metrics.MetricRegistry;
import com.raskasa.metrics.okhttp.InstrumentedOkHttpClients;
import io.appform.statesman.model.HttpClientConfiguration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

/**
 * @author shashank.g
 */
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpUtil {

    public static OkHttpClient defaultClient(final String clientName,
                                             final MetricRegistry registry,
                                             final HttpClientConfiguration configuration) {


        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            int connections = configuration.getConnections();
            connections = connections == 0 ? 10 : connections;

            int idleTimeOutSeconds = configuration.getIdleTimeOutSeconds();
            idleTimeOutSeconds = idleTimeOutSeconds == 0 ? 30 : idleTimeOutSeconds;

            int connTimeout = configuration.getConnectTimeoutMs();
            connTimeout = connTimeout == 0 ? 10000 : connTimeout;

            int opTimeout = configuration.getOpTimeoutMs();
            opTimeout = opTimeout == 0 ? 10000 : opTimeout;

            final Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequests(connections);
            dispatcher.setMaxRequestsPerHost(connections);

            final OkHttpClient.Builder clientBuilder = (new OkHttpClient.Builder())
                    .connectionPool(new ConnectionPool(connections, (long) idleTimeOutSeconds, TimeUnit.SECONDS))
                    .connectTimeout((long) connTimeout, TimeUnit.MILLISECONDS)
                    .readTimeout((long) opTimeout, TimeUnit.MILLISECONDS)
                    .writeTimeout((long) opTimeout, TimeUnit.MILLISECONDS)
                    .dispatcher(dispatcher)
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0])
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String s, SSLSession sslSession) {
                            return true;
                        }
                    });

            return registry != null
                    ? InstrumentedOkHttpClients.create(
                    registry, clientBuilder.build(), clientName + System.currentTimeMillis())
                    : clientBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String body(Response response) {
        try {
            if(null != response.body()) {
                return response.body().string();
            }
        }
        catch (IOException e) {
            log.error("Error reading response: ", e);
        }
        return "";
    }
}
