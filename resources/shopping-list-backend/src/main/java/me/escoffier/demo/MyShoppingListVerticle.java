package me.escoffier.demo;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.redis.RedisClient;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.RedisDataSource;

public class MyShoppingListVerticle extends AbstractVerticle {

    private RedisClient redis;

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        ServiceDiscovery.create(vertx, discovery -> {
            RedisDataSource.getRedisClient(discovery, rec -> rec.getName().equals("redis"), ar -> {
                if (ar.failed()) {
                    redis = RedisClient.create(vertx);
                } else {
                    redis = ar.result();
                }

                router.get("/shopping").handler(this::getShoppingList);
                router.route().handler(BodyHandler.create());
                router.post("/shopping").handler(this::addItem);
                router.get("/health").handler(rc -> rc.response().end("OK"));

                vertx.createHttpServer()
                    .requestHandler(router::accept)
                    .listen(8080);
            });
        });
    }

    private void addItem(RoutingContext rc) {
        String body = rc.getBodyAsString();
        if (body != null) {
            Item item = Json.decodeValue(body, Item.class);

            if (item.getQuantity() == 0) {
                redis.hdel("my-shopping-list", item.getName(), res -> {
                    if (res.failed()) {
                        rc.fail(res.cause());
                    } else {
                        getShoppingList(rc);
                    }
                });
            } else {
                redis.hset("my-shopping-list", item.getName(), Integer.toString(item.getQuantity()), res -> {
                    if (res.failed()) {
                        rc.fail(res.cause());
                    } else {
                        getShoppingList(rc);
                    }
                });
            }
        } else {
            rc.response().setStatusCode(400).end();
        }
    }

    private void getShoppingList(RoutingContext rc) {
        redis.hgetall("my-shopping-list", res -> {
            if (res.failed()) {
                rc.fail(res.cause());
            } else {
                rc.response().end(res.result().put("served-by", System.getenv("HOSTNAME")).encode());
            }
        });
    }

}
