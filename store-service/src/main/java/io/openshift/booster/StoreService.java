package io.openshift.booster;


import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class StoreService extends AbstractVerticle {

    private static final List<Store> STORES = Arrays.asList(
        new Store("Spotify"),
        new Store("Deezer"),
        new Store("Apple Music")
    );
    private final Random random = new Random();

    @Override
    public void start(Future<Void> done) {
        Router router = Router.router(vertx);
        router.route().handler(TracingInterceptor.create());
        router.get("/random").handler(this::getRandomStore);
        router.get("/health").handler(rc -> rc.response().end("OK"));

        vertx.createHttpServer()
            .requestHandler(router::accept)
            .rxListen(config().getInteger("http.port", 8080))
            .toCompletable()
            .subscribe(CompletableHelper.toObserver(done));
    }

    private void getRandomStore(RoutingContext rc) {
        Store store = STORES.get(random.nextInt(STORES.size()));
        rc.response().end(Json.encode(store));
    }
}