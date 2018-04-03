package io.openshift.booster;


import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

import static io.openshift.booster.TracingInterceptor.propagate;

public class SuggestionService extends AbstractVerticle {


    private WebClient albumServiceClient;
    private WebClient storeServiceClient;

    @Override
    public void start(Future<Void> done) {
        albumServiceClient = WebClient.create(vertx,
            new WebClientOptions().setDefaultHost("album-service").setDefaultPort(8080));
        storeServiceClient = WebClient.create(vertx,
            new WebClientOptions().setDefaultHost("store-service").setDefaultPort(8080));

        Router router = Router.router(vertx);
        router.route().handler(TracingInterceptor.create());
        router.get("/health").handler(rc -> rc.response().end("OK"));
        router.get("/api/suggest/serial").handler(this::serial);
        router.get("/api/suggest/parallel").handler(this::parallel);
        router.get("/*").handler(StaticHandler.create());

        vertx.createHttpServer()
            .requestHandler(router::accept)
            .rxListen(config().getInteger("http.port", 8080))
            .toCompletable()
            .subscribe(CompletableHelper.toObserver(done));
    }

    private void parallel(RoutingContext rc) {
        Single<Suggestion.Album> retrieveAlbum = propagate(albumServiceClient, rc)
            .get("/random")
            .rxSend()
            .map(HttpResponse::bodyAsJsonObject)
            .map(json -> json.mapTo(Suggestion.Album.class));

        Single<Suggestion.Store> retrieveStore = propagate(storeServiceClient, rc)
            .get("/random")
            .rxSend()
            .map(HttpResponse::bodyAsJsonObject)
            .map(json -> json.mapTo(Suggestion.Store.class));

        Single.zip(retrieveAlbum, retrieveStore, Suggestion::new)
            .map(Json::encode)
            .subscribe(
                payload -> rc.response().end(payload),
                rc::fail
            );
    }

    private void serial(RoutingContext rc) {
        propagate(albumServiceClient, rc)
            .get("/random")
            .rxSend()
            .map(HttpResponse::bodyAsJsonObject)
            .map(json -> json.mapTo(Suggestion.Album.class))
            .flatMap(album ->
                propagate(storeServiceClient, rc)
                    .get("/random")
                    .rxSend()
                    .map(HttpResponse::bodyAsJsonObject)
                    .map(json -> json.mapTo(Suggestion.Store.class))
                    .map(store -> new Suggestion(album, store))
            )
            .map(Json::encode)
            .subscribe(
                payload -> rc.response().end(payload),
                rc::fail
            );
    }
}
