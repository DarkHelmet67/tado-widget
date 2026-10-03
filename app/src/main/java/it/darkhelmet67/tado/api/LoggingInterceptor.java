package it.darkhelmet67.tado.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Logs one line per request: method, path (never the query string), status and duration.
 * Error response bodies are logged truncated, success bodies and headers never.
 */
public final class LoggingInterceptor implements Interceptor {
    private final NetworkLog log;

    public LoggingInterceptor(NetworkLog log) {
        this.log = log;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String what = request.method() + " " + request.url().host() + request.url().encodedPath();
        long start = System.nanoTime();
        try {
            Response response = chain.proceed(request);
            String line = what + " -> " + response.code() + " " + elapsedMs(start) + "ms";
            if (!response.isSuccessful()) {
                line += " " + response.peekBody(200).string().replace('\n', ' ');
            }
            log.log(line);
            return response;
        } catch (IOException e) {
            log.log(what + " FAILED after " + elapsedMs(start) + "ms: " + e);
            throw e;
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
