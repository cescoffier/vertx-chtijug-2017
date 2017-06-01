package me.escoffier.demo;

import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.circuitbreaker.CircuitBreaker;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.rxjava.servicediscovery.types.HttpEndpoint;
import rx.Observable;
import rx.Single;

public class MyShoppingList extends AbstractVerticle {

    WebClient shopping, pricer;
    private CircuitBreaker circuit;

    @Override
    public void start() {
        circuit = CircuitBreaker.create("circuit-breaker", vertx,
            new CircuitBreakerOptions()
                .setFallbackOnFailure(true)
                .setMaxFailures(3)
                .setResetTimeout(5000)
                .setTimeout(1000)
        );


        Router router = Router.router(vertx);
        router.route("/health").handler(rc ->
            rc.response().end("OK"));
        router.route("/").handler(this::getShoppingList);

        ServiceDiscovery.create(vertx, discovery -> {
            // Get pricer-service
            Single<WebClient> s1 = HttpEndpoint.rxGetWebClient(discovery, svc -> svc.getName().equals("pricer-service"));

            // Get shopping-backend
            Single<WebClient> s2 = HttpEndpoint.rxGetWebClient(discovery, svc -> svc.getName().equals("shopping-backend"));

            Single.zip(s1, s2, (p, s) -> {
                pricer = p;
                shopping = s;

                return vertx.createHttpServer()
                    .requestHandler(router::accept)
                    .listen(8080);
            }).subscribe();

            // When both are done...

        });
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

        Single<JsonObject> list = shopping
            .get("/shopping")
            .rxSend()
            .map(HttpResponse::bodyAsJsonObject);

        list.subscribe(
            json -> {
                Observable.from(json)
                    .flatMapSingle(entry ->
                        circuit.rxExecuteCommandWithFallback(
                            future -> Shopping.retrievePrice(pricer, entry, future),
                            err -> Shopping.getFallbackPrice(entry)
                        )
                    )
                    .subscribe(
                        res -> {
                            System.out.println(res.encode());
                            Shopping
                                .writeProductLine
                                    (serverResponse, res);
                        },
                        t -> {
                            t.printStackTrace();
                            rc.fail(t);
                        },
                        serverResponse::end
                    );
            }
        );

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
