package io.vertx.blog.first;

import io.vertx.core.json.JsonObject;

public class Whisky {

    private final int id;

    private String name;

    private String origin;

    public Whisky() {
        this.id = -1;
    }

    public Whisky(String name, String origin) {
        this.id = -1;
        this.name = name;
        this.origin = origin;
    }
    
    public Whisky(JsonObject json) {
        this.id = json.getInteger("ID");
        this.name = json.getString("NAME");
        this.origin = json.getString("ORIGIN");
    }

    public Whisky(int id, String name, String origin) {
        this.id = -1;
        this.name = name;
        this.origin = origin;
    }
    
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOrigin() {
        return origin;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

}
