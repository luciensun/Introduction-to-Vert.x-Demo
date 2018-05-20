package io.vertx.blog.first;

import java.util.LinkedHashMap;
import java.util.Map;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MyFirstVerticle extends AbstractVerticle {
    // Store our product
    private Map<Integer, Whisky> products = new LinkedHashMap<>();

    // Create some product
    private void createSomeProducts() {
        Whisky bowmore = new Whisky("Bowmore 15 Years Laimrig",
                "Scotland, Islay");
        products.put(bowmore.getId(), bowmore);
        Whisky talisker = new Whisky("Talisker 57 North", "Scotland, Island");
        products.put(talisker.getId(), talisker);
    }

    private void getAll(RoutingContext context) {
        context.response()
                .putHeader("Content-Type", "application/json;charset=utf-8")
                .end(Json.encodePrettily(products.values()));
    }

    private void addOne(RoutingContext context) {
        final Whisky whisky = Json.decodeValue(context.getBodyAsString(),
                Whisky.class);
        products.put(whisky.getId(), whisky);
        context.response().setStatusCode(201)
                .putHeader("Content-Type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(whisky));
    }

    private void deleteOne(RoutingContext context) {
        String id = context.request().getParam("id");
        if (id == null) {
            context.response().setStatusCode(400).end();
        } else {
            Integer idAsInteger = Integer.valueOf(id);
            products.remove(idAsInteger);
        }
        context.response().setStatusCode(204).end();
    }

    private void getOne(RoutingContext context) {
        String id = context.request().getParam("id");
        if (id == null) {
            context.response().setStatusCode(400).end();
        } else {
            Integer idAsInteger = Integer.valueOf(id);
            Whisky whisky = products.get(idAsInteger);
            if (whisky == null) {
                context.response().setStatusCode(404).end();
            } else {
                context.response().setStatusCode(200)
                        .putHeader("Content-Type",
                                "application/json; charset=utf-8")
                        .end(Json.encodePrettily(whisky));
            }
        }
    }

    private void updateOne(RoutingContext context) {
        final String id = context.request().getParam("id");
        final Whisky newWhisky = Json.decodeValue(context.getBodyAsString(),
                Whisky.class);
        if (id == null || newWhisky == null) {
            context.response().setStatusCode(400).end();
        } else {
            Integer idAsInteger = Integer.valueOf(id);
            Whisky whisky = products.get(idAsInteger);
            if (whisky == null) {
                context.response().setStatusCode(404).end();
            } else {
                products.put(idAsInteger, newWhisky);
                whisky = null;
                context.response().setStatusCode(201)
                .putHeader("Content-Type",
                        "application/json; charset=utf-8")
                .end(Json.encodePrettily(newWhisky));
            }

        }
    }

    @Override
    public void start(Future<Void> fut) {

        createSomeProducts();

        // Create a Router object.
        Router router = Router.router(vertx);

        // Bind "/" to our hello message - so we are still compatible.
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("Content-Type", "text/html;charset=UTF-8")
                    .end("<h1>Hello from my first Vert.x 3 application</h1>");
        });

        // Serve static resources from the /assets directory.
        router.route("/assets/*").handler(StaticHandler.create("assets"));

        router.get("/api/whiskies").handler(this::getAll);

        router.route("/api/whiskies*").handler(BodyHandler.create());
        router.post("/api/whiskies").handler(this::addOne);

        router.delete("/api/whiskies/:id").handler(this::deleteOne);

        router.get("/api/whiskies/:id").handler(this::getOne);

        router.put("/api/whiskies/:id").handler(this::updateOne);

        // Create the HTTP server and pass the "accept" method to the request
        // handler.
        vertx.createHttpServer().requestHandler(router::accept).listen(
                // Retrieve the port from the configuration,
                // default to 8080.
                config().getInteger("http.port", 8080), res -> {
                    if (res.succeeded()) {
                        fut.complete();
                    } else {
                        fut.fail(res.cause());
                    }
                });
    }
}
