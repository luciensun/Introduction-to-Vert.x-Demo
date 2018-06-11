package io.vertx.blog.first;

import io.vertx.core.json.JsonObject;

public class Whisky {

    private String id;

    private String name;

    private String origin;

    public Whisky() {
        this.id = "";
    }

    public Whisky(String name, String origin) {
        this.id = "";
        this.name = name;
        this.origin = origin;
    }

    public Whisky(JsonObject json) {
        this.id = json.getString("_id");
        this.name = json.getString("name");
        this.origin = json.getString("origin");
    }

    public Whisky(String id, String name, String origin) {
        this.id = id;
        this.name = name;
        this.origin = origin;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject().put("name", name).put("origin",
                origin);
        if (id != null && !id.isEmpty()) {
            json.put("_id", id);
        }
        return json;
    }

    public String getId() {
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

    public void setId(String id) {
        this.id = id;
    }

}
