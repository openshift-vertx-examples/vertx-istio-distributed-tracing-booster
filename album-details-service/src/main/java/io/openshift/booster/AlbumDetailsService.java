package io.openshift.booster;


import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

public class AlbumDetailsService extends AbstractVerticle {

    @Override
    public void start(Future<Void> done) {
        Router router = Router.router(vertx);
        router.route().handler(TracingInterceptor.create());
        router.get("/:id").handler(this::getAlbumDetails);

        vertx.createHttpServer()
            .requestHandler(router::accept)
            .rxListen(config().getInteger("http.port", 8080))
            .toCompletable()
            .subscribe(CompletableHelper.toObserver(done));

    }

    private void getAlbumDetails(RoutingContext rc) {
        String id = rc.pathParam("id");
        JsonObject json = new JsonObject()
            .put("detail1", "value-for-album-" + id)
            .put("detail2", "something-for-album-" + id);
        rc.response().end(json.encode());
    }
}
