package me.escoffier.demo;

import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.circuitbreaker.CircuitBreaker;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.rxjava.servicediscovery.types.HttpEndpoint;
import rx.Observable;
import rx.Single;

import static me.escoffier.demo.Shopping.*;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class MyShoppingList extends AbstractVerticle {

    private WebClient shopping;
    private WebClient pricer;
    private CircuitBreaker circuit;

    @Override
    public void start(Future<Void> future) throws Exception {

        Router router = Router.router(vertx);

        circuit = CircuitBreaker.create("circuit-breaker", vertx, new CircuitBreakerOptions()
            .setFallbackOnFailure(true)
            .setMaxFailures(3)
            .setResetTimeout(5000)
            .setTimeout(1000)
        );

        router.get("/").handler(this::getShoppingList);

        ServiceDiscovery.create(vertx, discovery -> {

            Single<WebClient> s1 = HttpEndpoint.rxGetWebClient(discovery,
                rec -> rec.getName().equals("shopping-backend"));
            Single<WebClient> s2 = HttpEndpoint.rxGetWebClient(discovery,
                rec -> rec.getName().equals("pricer-service"));

            Single.zip(s1, s2, (x, y) -> {
                shopping = x;
                pricer = y;
                return vertx.createHttpServer()
                    .requestHandler(router::accept)
                    .listen(8080);
            }).subscribe();
        });

    }

    private void getShoppingList(RoutingContext rc) {
        Single<HttpResponse<Buffer>> list = shopping.get("/shopping").rxSend();

        HttpServerResponse serverResponse = rc.response()
            .setChunked(true);

        list.subscribe(
            response -> {
                JsonObject body = response.bodyAsJsonObject();
                Observable.from(body)
                    .flatMap(entry ->
                        retrievePrice(pricer, entry).toObservable())
                    .subscribe(
                        product -> writeProductLine(serverResponse, product),
                        rc::fail,
                        serverResponse::end
                    );
            },
            rc::fail);
    }

    private void getShoppingListWithCB(RoutingContext rc) {
        Single<HttpResponse<Buffer>> list = shopping.get("/shopping").rxSend();

        HttpServerResponse serverResponse = rc.response()
            .setChunked(true);

        list.subscribe(
            response -> {
                JsonObject body = response.bodyAsJsonObject();
                Observable.from(body)
                    .flatMap(entry ->
                        circuit.executeWithFallback(
                            future -> retrievePrice(pricer, entry, future),
                            t -> getFallbackPrice(entry)
                        )
                            .rxSetHandler().toObservable())
                    .subscribe(
                        product -> writeProductLine(serverResponse, product),
                        rc::fail,
                        serverResponse::end
                    );
            },
            rc::fail);
    }


}
