package me.escoffier.shopping;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.redis.RedisClient;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.RedisDataSource;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class MyShoppingVerticle extends AbstractVerticle {

    private static final String KEY = "shopping-list";
    private RedisClient redis;

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);

        router.get("/shopping").handler(this::getList);
        router.route().handler(BodyHandler.create());
        router.post("/shopping").handler(this::addToList);
        router.delete("/shopping/:name").handler(this::deleteFromList);

        ServiceDiscovery.create(vertx, discovery -> {
            RedisDataSource.getRedisClient(discovery, rec -> rec.getName().equals("redis"), ar -> {
                redis = ar.result();
                vertx.createHttpServer()
                    .requestHandler(router::accept)
                    .listen(8080);
            });
        });
    }

    private void deleteFromList(RoutingContext rc) {
        String name = rc.pathParam("name");
        redis.hdel(KEY, name, x -> {
            getList(rc);
        });

    }

    private void addToList(RoutingContext rc) {
        JsonObject json = rc.getBodyAsJson();
        int quantity = json.getInteger("quantity", 1);
        if (quantity <= 0) {
            rc.response().setStatusCode(400);
            return;
        }

        redis.hset(KEY, json.getString("name"), Integer.toString(quantity), r -> {
            if (r.succeeded()) {
                getList(rc);
            } else {
                rc.fail(r.cause());
            }
        });

    }

    private void getList(RoutingContext rc) {
        redis.hgetall(KEY, ar -> {
            if (ar.succeeded()) {
                rc.response()
                    .putHeader("Content-Type", "application/json")
                    .putHeader("X-Served-By", System.getenv("HOSTNAME"))
                    .end(ar.result().encode());
            } else {
                rc.fail(ar.cause());
            }
        });
    }
}
