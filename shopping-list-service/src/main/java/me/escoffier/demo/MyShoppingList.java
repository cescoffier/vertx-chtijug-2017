package me.escoffier.demo;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import rx.Observable;
import rx.Single;

public class MyShoppingList extends AbstractVerticle {

    WebClient shopping, pricer;

    @Override
    public void start() {
        Router router = Router.router(vertx);
        router.route("/health").handler(rc ->
            rc.response().end("OK"));
        router.route("/").handler(this::getShoppingList);

        ServiceDiscovery.create(vertx, discovery -> {
            // Get pricer-service

            // Get shopping-backend

            // When both are done...

        });

        vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(8080);

    }

    private void getShoppingList(RoutingContext rc) {
        HttpServerResponse serverResponse =
            rc.response().setChunked(true);

       /*
         +--> Retrieve shopping list
           +
           +-->  for each item, call the pricer, concurrently
                    +
                    |
                    +-->  For each completed evaluation (line),
                          write it to the HTTP response
         */
        

         /*
                               shopping          pricer
                               backend
                 +                +                +
                 |                |                |
                 +--------------> |                |
                 |                |                |
                 |                |                |
                 +-------------------------------> |
                 |                |                |
                 +-------------------------------> |
        write <--|                |                |
                 +-------------------------------> |
        write <--|                +                +
                 |
        write <--|
                 |
          end <--|
         */
    }

}
