package io.openshift.booster;

import io.vertx.core.Handler;
import io.vertx.reactivex.ext.web.RoutingContext;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Propagate OpenTracing HTTP headers.
 */
public class TracingInterceptor {

    private static final List<String> FORWARDED_HEADER_NAMES = Arrays.asList(
        "x-request-id",
        "x-b3-traceid",
        "x-b3-spanid",
        "x-b3-parentspanid",
        "x-b3-sampled",
        "x-b3-flags",
        "x-ot-span-context",
        "user-agent"
    );

    private TracingInterceptor() {
        // Avoid direct instantiation.
    }

    private static final String X_TRACING_HEADERS = "X-Tracing-Headers";

    static Handler<RoutingContext> create() {
        return rc -> {
            Set<String> names = rc.request().headers().names();
            Map<String, List<String>> headers = names.stream()
                .map(String::toLowerCase)
                .filter(FORWARDED_HEADER_NAMES::contains)
                .collect(Collectors.toMap(
                    Function.identity(),
                    h -> Collections.singletonList(rc.request().getHeader(h))
                ));
            rc.put(X_TRACING_HEADERS, headers);
            rc.next();
        };
    }
}
