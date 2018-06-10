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
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MyFirstVerticle extends AbstractVerticle {

    private JDBCClient jdbc;
    
    private void getAll(RoutingContext context) {
        jdbc.getConnection(ar -> {
            if (ar.failed()) {
                // TODO remind us of the query failure.
                context.fail(ar.cause());
            } else {
                SQLConnection connection = ar.result();
                connection.query("select * from Whisky", select -> {
                    List<Whisky> whiskies = select.result().getRows()
                            .stream().map(Whisky::new).collect(Collectors.toList());
                    context.response()
                        .putHeader("Content-Type", "application/json;charset=utf-8")
                        .end(Json.encodePrettily(whiskies));
                    connection.close();
                });
            }
        });
    }
    
    private void getOne(RoutingContext context) {
        String id = context.request().getParam("id");
        if (id == null) {
            context.response().setStatusCode(400).end();
        } else {
            jdbc.getConnection(ar -> {
                SQLConnection connection = ar.result();
                select(id, connection, result -> {
                    if (result.failed()) {
                        context.response().setStatusCode(404).end();
                    } else {
                        context.response().setStatusCode(200)
                        .putHeader("Content-Type",
                                "application/json; charset=utf-8")
                        .end(Json.encodePrettily(result.result()));
                    }
                    connection.close();
                });
            });
        }
    }

    private void addOne(RoutingContext context) {
        // Read the request's content and create an instance of Whisky.
        final Whisky whisky = Json.decodeValue(context.getBodyAsString(),
                Whisky.class);
        jdbc.getConnection(ar -> {
            if (ar.failed()) {
                context.fail(ar.cause());
            } else {
                SQLConnection connection = ar.result();
                insert(whisky, connection, (r) -> {
                    context.response()
                    .setStatusCode(201)
                    .putHeader("Content-Type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(r.result()));

                    connection.close();
                });
            }
        });
    }

    private void deleteOne(RoutingContext context) {
        String id = context.request().getParam("id");
        if (id == null) {
            context.response().setStatusCode(400).end();
        } else {
            jdbc.getConnection(ar -> {
                if (ar.failed()) {
                    context.fail(ar.cause());
                } else {
                    SQLConnection connection = ar.result();
                    delete(id, connection, (r) -> {
                        context.response().setStatusCode(204).end();
                        connection.close();
                    });
                }
            });
        }
    }

    private void updateOne(RoutingContext context) {
        final String id = context.request().getParam("id");
        final Whisky newWhisky = Json.decodeValue(context.getBodyAsString(),
                Whisky.class);
        if (id == null || newWhisky == null) {
            context.response().setStatusCode(400).end();
        } else {
            jdbc.getConnection(ar -> {
                if (ar.failed()) {
                    context.fail(ar.cause());
                } else {
                    SQLConnection connection = ar.result();
                    update(id, newWhisky, connection, (r) -> {
                        if (r.failed()) {
                            context.response().setStatusCode(404).end();
                        } else {
                            context.response().setStatusCode(201)
                            .putHeader("Content-Type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(r.result()));
                        }
                        connection.close();
                    });
                }
            });
        }
    }

    private void startBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> fut) {
        jdbc.getConnection(ar -> {
            if (ar.failed()) {
                fut.fail(ar.cause());
            } else {
                next.handle(Future.succeededFuture(ar.result()));
            }
        });
    }
    
    private void createSomeData(AsyncResult<SQLConnection> result
            , Handler<AsyncResult<Void>> next, Future<Void> fut) {
        if (result.failed()) {
            fut.fail(result.cause());
        } else {
            SQLConnection connection = result.result();
            connection.execute(
                "Create table if not exists Whisky(id integer identity, name varchar(100)"
                + ", origin varchar(100))", ar -> {
                    if (ar.failed()) {
                        fut.fail(ar.cause());
                        connection.close();
                    } else {
                        connection.query("select * from Whisky", select -> {
                            if (select.failed()) {
                                fut.fail(select.cause());
                                connection.close();
                            } else {
                                ResultSet resultSet = select.result();
                                if (resultSet.getNumRows() == 0) {
                                    insert(
                                        new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay")
                                        , connection, (v) -> insert(new Whisky("Talisker 57 North", "Scotland, Island")
                                                , connection
                                                , (r) -> {
                                                    next.handle(Future.succeededFuture());
                                                    connection.close();
                                                }));
                                } else {
                                    next.handle(Future.succeededFuture());
                                    connection.close();
                                }
                            }
                        });
                    }
                });
        }
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
    
    private void completeStartup(AsyncResult<HttpServer> http
            , Future<Void> fut) {
        if (http.failed()) {
            fut.fail(http.cause());
        } else {
            fut.complete();
        }
    }
    
    private void select(String id, SQLConnection connection
            , Handler<AsyncResult<Whisky>> resultHandler) {
        String sql = "select * from Whisky where id = ?";
        connection.queryWithParams(sql, new JsonArray().add(id)
                , ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(Future.failedFuture("Whisky not found"));
                    } else {
                        ResultSet resultSet = ar.result();
                        if (resultSet.getNumRows() >= 1) {
                            Whisky w = new Whisky(resultSet.getRows().get(0));
                            resultHandler.handle(Future.succeededFuture(w));
                        } else {
                            resultHandler.handle(Future.failedFuture("Whisky not found"));
                        }
                    }
        });
    }
    
    private void update(String id, Whisky whisky, SQLConnection connection
            , Handler<AsyncResult<Whisky>> resultHandler) {
        String sql = "update Whisky set name = ?, origin = ? where id = ?";
        connection.updateWithParams(sql, new JsonArray().add(whisky.getName()).add(whisky.getOrigin()).add(id)
                , update -> {
                    if (update.failed()) {
                        resultHandler.handle(Future.failedFuture("Cannot update the whisky"));
                    } else {
                        UpdateResult result = update.result();
                        if (result.getUpdated() <= 0) {
                            resultHandler.handle(Future.failedFuture("Whisky not found"));
                        } else {
                            Whisky w = new Whisky(Integer.valueOf(id), whisky.getName(), whisky.getOrigin());
                            resultHandler.handle(Future.succeededFuture(w));
                        }
                    }
                });
    }
     
    private void insert(Whisky whisky, SQLConnection connection
            , Handler<AsyncResult<Whisky>> next) {
        String sql = "insert into Whisky (name, origin) values(?, ?)";
        connection.updateWithParams(sql
                , new JsonArray().add(whisky.getName()).add(whisky.getOrigin())
                , ar -> {
                    if (ar.failed()) {
                        next.handle(Future.failedFuture(ar.cause()));
                    } else {
                        UpdateResult result = ar.result();
                        // Build a new whisky instance with the generated id.
                        Whisky w = new Whisky(result.getKeys().getInteger(0), whisky.getName(), whisky.getOrigin());
                      
                        next.handle(Future.succeededFuture(w));
                    }
                });
    }
    
    private void delete(String id, SQLConnection connection
            , Handler<AsyncResult<Whisky>> next) {
        String sql = "delete from Whisky where id = ?";
        connection.updateWithParams(sql, new JsonArray().add(id)
                , ar -> {
                    if (ar.failed()) {
                        next.handle(Future.failedFuture(ar.cause()));
                    } else {
                        UpdateResult result = ar.result();
                        next.handle(Future.succeededFuture());
                    }
                });
    }
    
    @Override
    public void start(Future<Void> fut) {

        jdbc = JDBCClient.createShared(vertx, config(), "My-Whisky-Collection");
        
        startBackend(
            (connection) -> createSomeData(connection, 
                (nothing) -> startWebApp(
                    (http) -> completeStartup(http, fut)
                    ), fut
                ), fut);

    }
    
    @Override
    public void stop() {
        jdbc.close();
    }
}
