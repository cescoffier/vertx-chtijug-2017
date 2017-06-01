package me.escoffier.demo;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.stream.Collectors;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class MyShoppingFrontend extends AbstractVerticle {

    private WebClient client;

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);

        FreeMarkerTemplateEngine engine = FreeMarkerTemplateEngine.create();

        router.get("/shopping").handler(this::getList);
        router.get("/").handler(rc -> {
            engine.render(rc, "templates/index.ftl", ar -> {
                if (ar.succeeded()) {
                    rc.response().end(ar.result());
                } else {
                    rc.response().end("Fail to render template");
                }
            });
        });


        ServiceDiscovery.create(vertx, discovery -> {
            HttpEndpoint.getWebClient(discovery, rec -> rec.getName().equals("shopping-list-backend"), client -> {
                this.client = client.result();
                vertx.createHttpServer().requestHandler(router::accept).listen(8080);
            });
        });

    }

    private void getList(RoutingContext rc) {
        this.client.get("/shopping")
            .send(resp -> {
                if (resp.failed()) {
                    rc.response().end("Unable to call service: " + resp.cause());
                } else {
                    JsonObject object = resp.result().bodyAsJsonObject();
                    rc.response()
                        .putHeader("X-INTERCEPTED", System.getenv("HOSTNAME"))
                        .end(object.stream().map(entry ->
                            "[ ] " + entry.getKey() + " : " + entry.getValue()
                        ).collect(Collectors.joining("\n")));
                }
            });
    }
}
