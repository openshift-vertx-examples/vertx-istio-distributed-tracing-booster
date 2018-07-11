package io.openshift.vertx.cache;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

public class GreetingVerticle extends AbstractVerticle {

  private WebClient client;

  @Override
  public void start(Future<Void> future) {

    // Access to Cute name service.
    client = WebClient.create(vertx, new WebClientOptions()
      .setDefaultHost("vertx-istio-distributed-tracing-cute-name-service")
      .setDefaultPort(8080)
    );

    Router router = Router.router(vertx);
    router.route().handler(rc -> {
      System.out.println(rc.request().method() + " " + rc.request().path());
      System.out.println(rc.request().headers().names());
      rc.next();
    });
    router.route().handler(TracingInterceptor.create());
    router.route().handler(BodyHandler.create());
    router.get("/api/greeting").handler(this::greeting);
    router.get("/health").handler(rc -> rc.response().end("OK"));
    router.get("/*").handler(StaticHandler.create());

    vertx
      .createHttpServer()
      .requestHandler(router::accept)
      .rxListen(config().getInteger("http.port", 8080))
      .toCompletable()
      .subscribe(CompletableHelper.toObserver(future));
  }

  private void greeting(RoutingContext rc) {
    TracingInterceptor.propagate(client, rc)
      .get("/api/name")
      .rxSend()
      .map(HttpResponse::bodyAsJsonObject)
      .map(j -> j.getString("name"))
      .map(name -> new JsonObject().put("message", "Hello " + name))
      .onErrorReturn(t -> new JsonObject().put("message", "Unable to call the service: " + t.getMessage()))
      .map(JsonObject::encode)
      .subscribe(
        message -> rc.response()
          .putHeader("content-type", "application/json")
          .end(message),
        rc::fail
      );
  }


}
