package io.openshift.booster;

import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class AlbumService extends AbstractVerticle {

    private static final List<Album> ALBUMS = Arrays.asList(
        new Album(1L, "Metallica", "...And Justice for All"),
        new Album(2L, "Iron Maiden", "Brave New World"),
        new Album(3L, "Disturbed", "Immortalized")
    );

    private WebClient client;
    private final Random random = new Random();

    @Override
    public void start(Future<Void> done) {
        Router router = Router.router(vertx);
        router.route().handler(TracingInterceptor.create());
        router.get("/random").handler(this::getRandomAlbum);
        router.get("/health").handler(rc -> rc.response().end("OK"));

        client = WebClient.create(vertx,
            new WebClientOptions().setDefaultHost("album-details-service").setDefaultPort(8080));

        vertx.createHttpServer()
            .requestHandler(router::accept)
            .rxListen(config().getInteger("http.port", 8080))
            .toCompletable()
            .subscribe(CompletableHelper.toObserver(done));
    }

    private Album getRandomAlbum() {
        return ALBUMS.get(random.nextInt(ALBUMS.size()));
    }

    private void getRandomAlbum(RoutingContext rc) {
        Album album = getRandomAlbum();
        TracingInterceptor.propagate(client, rc).get("/" + album.getId())
            .rxSend()
            .map(HttpResponse::bodyAsJsonObject)
            .map(json -> json.stream().collect(Collectors.toMap(Map.Entry::getKey, Object::toString)))
            .map(album::withDetails)
            .map(Json::encode)
            .subscribe(
                content -> rc.response().end(content),
                rc::fail
            );
    }
}
