package io.vertx.blog.first;

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MyFirstVerticle extends AbstractVerticle {

    public static final String COLLECTION = "whiskies";

    private MongoClient mongo;

    private void getAll(RoutingContext context) {
        mongo.find(COLLECTION, new JsonObject(), find -> {
            List<JsonObject> objects = find.result();
            List<Whisky> whiskies = objects.stream().map(Whisky::new)
                    .collect(Collectors.toList());
            context.response()
                    .putHeader("Content-Type",
                            "application/json; charset=utf-8")
                    .end(Json.encodePrettily(whiskies));
        });

    }

    private void getOne(RoutingContext context) {
        String id = context.request().getParam("id");
        if (id == null) {
            context.response().setStatusCode(400).end();
        } else {
            mongo.findOne(COLLECTION, new JsonObject().put("_id", id), null,
                    find -> {
                        if (find.failed()) {
                            context.response().setStatusCode(404).end();
                        } else {
                            if (find.result() == null) {
                                context.response().setStatusCode(404).end();
                            } else {
                                Whisky whisky = new Whisky(find.result());
                                context.response().setStatusCode(200).putHeader(
                                        "Content-Type",
                                        "application/json; charset=utf-8")
                                        .end(Json.encodePrettily(whisky));
                            }
                        }
                    });

        }
    }

    private void addOne(RoutingContext context) {
        // Read the request's content.
        JsonObject json = context.getBodyAsJson();
        final Whisky whisky = new Whisky(json);
        mongo.insert(COLLECTION, json, insert -> {
            if (insert.failed()) {
                context.response().setStatusCode(400).end();
            } else {
                whisky.setId(insert.result());
                context.response().setStatusCode(201)
                        .putHeader("Content-Type",
                                "application/json; charset=utf-8")
                        .end(Json.encodePrettily(whisky));
            }
        });

    }

    private void deleteOne(RoutingContext context) {
        String id = context.request().getParam("id");
        if (id == null) {
            context.response().setStatusCode(400).end();
        } else {
            mongo.removeDocument(COLLECTION, new JsonObject().put("_id", id),
                    delete -> {
                        context.response().setStatusCode(204).end();
                    });

        }
    }

    private void updateOne(RoutingContext context) {
        final String id = context.request().getParam("id");
        JsonObject json = context.getBodyAsJson();

        if (id == null || json == null) {
            context.response().setStatusCode(400).end();
        } else {
            mongo.findOneAndUpdate(COLLECTION, new JsonObject().put("_id", id) // Select
                                                                               // document
            // The update syntax: {$set, the json object containing the fields
            // to update}
                    , new JsonObject().put("$set", json), update -> {
                        if (update.failed()) {
                            context.response().setStatusCode(404).end();
                        } else {
                            context.response()
                                    .putHeader("Content-Type",
                                            "application/json; charset=utf-8")
                                    .end(Json.encodePrettily(new Whisky(id,
                                            json.getString("name"),
                                            json.getString("origin"))));
                        }
                    });
        }
    }

    private void createSomeData(Handler<AsyncResult<Void>> next,
            Future<Void> fut) {
        Whisky bowmore = new Whisky("Bowmore 15 Years Laimrig",
                "Scotland, Islay");
        Whisky talisker = new Whisky("Talisker 57° North", "Scotland, Island");
        // Do we have data in the collection ?
        mongo.count(COLLECTION, new JsonObject(), count -> {
            if (count.succeeded()) {
                if (count.result() == 0) {
                    // no whiskies, insert data
                    mongo.insert(COLLECTION, bowmore.toJson(), ar -> {
                        if (ar.failed()) {
                            fut.fail(ar.cause());
                        } else {
                            mongo.insert(COLLECTION, talisker.toJson(), ar2 -> {
                                if (ar2.failed()) {
                                    fut.fail(ar2.cause());
                                } else {
                                    next.handle(Future.succeededFuture());
                                }
                            });
                        }
                    });
                } else {
                    next.handle(Future.succeededFuture());
                }
            } else {
                // report the error
                fut.fail(count.cause());
            }
        });

    }

    private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
        // Create a Router object.
        Router router = Router.router(vertx);

        // Bind "/" to our hello message - so we are still compatible.
        router.route("/").handler(context -> {
            HttpServerResponse response = context.response();
            response.putHeader("Content-Type", "text/html;charset=UTF-8")
                    .end("<h1>Hello from my first Vert.x 3 application</h1>");
        });

        // Serve static resources from the /assets directory.
        router.route("/assets/*").handler(StaticHandler.create("assets"));

        router.get("/api/whiskies").handler(this::getAll);

        router.route("/api/whiskies*").handler(BodyHandler.create());

        router.post("/api/whiskies").handler(this::addOne);

        router.get("/api/whiskies/:id").handler(this::getOne);

        router.put("/api/whiskies/:id").handler(this::updateOne);

        router.delete("/api/whiskies/:id").handler(this::deleteOne);

        // Create the HTTP server and pass the "accept" method
        // to the request handler.
        vertx.createHttpServer().requestHandler(router::accept).listen(
                // Retrieve the port from the configuration,
                // default to 8080.
                config().getInteger("http.port", 8080), next::handle);
    }

    private void completeStartup(AsyncResult<HttpServer> http,
            Future<Void> fut) {
        if (http.failed()) {
            fut.fail(http.cause());
        } else {
            fut.complete();
        }
    }

    @Override
    public void start(Future<Void> fut) {

        mongo = MongoClient.createShared(vertx, config(),
                "My-Whisky-Collection");

        createSomeData(
                (nothing) -> startWebApp((http) -> completeStartup(http, fut)),
                fut);
    }

    @Override
    public void stop() {
        mongo.close();
    }
}
