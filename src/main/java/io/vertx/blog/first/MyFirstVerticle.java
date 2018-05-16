package io.vertx.blog.first;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class MyFirstVerticle extends AbstractVerticle {
    
    @Override
    public void start(Future<Void> fut) {
        vertx.createHttpServer()
        .requestHandler(rh -> {
            rh.response().putHeader("Context-Type", "text/html");
            rh.response().end("<h1>Hello from my first "
                    + "Vert.x 3 application</h1>");
        }).listen(8080, res -> {
            if (res.succeeded()) {
                fut.complete();
            } else {
                fut.fail(res.cause());
            }
        });
    }
}
