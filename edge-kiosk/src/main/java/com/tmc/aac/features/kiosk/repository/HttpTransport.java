package com.tmc.aac.features.kiosk.repository;

import java.net.ConnectException;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

final class HttpTransport {
    private final RestClient client;
    HttpTransport(String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofMillis(350)).build());
        factory.setReadTimeout(Duration.ofMillis(1000));
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
    <T> T get(String path, Class<T> type) { return execute(() -> client.get().uri(path).retrieve().body(type)); }
    <T> T post(String path, Object body, Class<T> type) {
        return execute(() -> client.post().uri(path).body(body).retrieve().body(type));
    }
    private <T> T execute(java.util.function.Supplier<T> action) {
        try {
            T result = action.get();
            if (result == null) throw new RemoteUnavailable(false);
            return result;
        } catch (ResourceAccessException ex) {
            Throwable cause = ex;
            while (cause != null) {
                if (cause instanceof ConnectException) throw new RemoteUnavailable(true);
                cause = cause.getCause();
            }
            throw new RemoteUnavailable(false);
        } catch (HttpStatusCodeException ex) {
            // Only the explicit mock contract certifies that authorization was not processed.
            boolean notProcessed = ex.getStatusCode().value() == 503
                    && ex.getResponseBodyAsString().replace(" ", "").equals("{\"notProcessed\":true}");
            throw new RemoteUnavailable(notProcessed);
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new RemoteUnavailable(false);
        }
    }
}
