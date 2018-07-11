package io.openshift.vertx.cache;

import io.vertx.core.Handler;
import io.vertx.ext.web.client.impl.WebClientInternal;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.WebClient;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Propagate OpenTracing HTTP headers.
 */
public class TracingInterceptor {

  //
  private static final List<String> FORWARDED_HEADER_NAMES = Arrays.asList(
    "x-request-id",
    "x-b3-traceid",
    "x-b3-spanid",
    "x-b3-parentspanid",
    "x-b3-sampled",
    "x-b3-flags",
    "x-ot-span-context"
  );

  private static final String X_TRACING_HEADERS = "X-Tracing-Headers";

  private TracingInterceptor() {
    // Avoid direct instantiation.
  }

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

  static WebClient propagate(WebClient client, RoutingContext rc) {
    WebClientInternal delegate = (WebClientInternal) client.getDelegate();
    delegate.addInterceptor(ctx -> {
      Map<String, List<String>> headers = rc.get(X_TRACING_HEADERS);
      if (headers != null) {
        System.out.println("Propagating... " + headers);
        headers.forEach((s, l) -> l.forEach(v -> ctx.request().putHeader(s, v)));
      }
      ctx.next();
    });
    return client;
  }
}
