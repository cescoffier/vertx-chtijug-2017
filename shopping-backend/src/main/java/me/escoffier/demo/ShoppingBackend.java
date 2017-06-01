package me.escoffier.demo;

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
public class ShoppingBackend extends AbstractVerticle {

    private RedisClient client;
    private static final String KEY = "SHOPPING";

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        router.get("/")
            .handler(rc -> rc.response()
                .end("Bonjour Tourcoing"));

        router.get("/shopping").handler(this::getList);
        router.route().handler(BodyHandler.create());
        router.post("/shopping")
            .handler(this::addToList);
        router.delete("/shopping/:name").handler(this::deleteFromList);

        ServiceDiscovery.create(vertx, discovery -> {
            RedisDataSource.getRedisClient(discovery,
                svc -> svc.getName().equals("redis"),
                ar -> {
                    if (ar.failed()) {
                        System.out.println("D'oh !");
                    } else {
                        client = ar.result();
                        vertx.createHttpServer()
                            .requestHandler(router::accept)
                            .listen(8080);
                    }
                });
        });

    }

    private void deleteFromList(RoutingContext rc) {
        String name = rc.pathParam("name");
        client.hdel(KEY, name, x -> {
            getList(rc);
        });
    }

    private void addToList(RoutingContext rc) {
        JsonObject json = rc.getBodyAsJson();
        String name = json.getString("name");
        Integer quantity = json
            .getInteger("quantity", 1);

        client.hset(KEY, name, quantity.toString(), x -> {
            getList(rc);
        });
    }

    private void getList(RoutingContext rc) {
        client.hgetall(KEY, ar -> {
            if (ar.failed()) {
                rc.fail(ar.cause());
            } else {
                rc.response()
                    .putHeader("X-Served-By",
                        System.getenv("HOSTNAME"))
                    .end(ar.result().encodePrettily());
            }
        });
    }
}
