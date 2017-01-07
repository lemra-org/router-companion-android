package org.rm3l.router_companion.tasker.api.urlshortener.goo_gl.resources;

/**
 * Created by rm3l on 02/08/16.
 */
public class GooGlData {

    private String kind;

    private String id;

    private String longUrl;

    private String status;

    private String created;

    public String getKind() {
        return kind;
    }

    public GooGlData setKind(String kind) {
        this.kind = kind;
        return this;
    }

    public String getId() {
        return id;
    }

    public GooGlData setId(String id) {
        this.id = id;
        return this;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public GooGlData setLongUrl(String longUrl) {
        this.longUrl = longUrl;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public GooGlData setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public GooGlData setCreated(String created) {
        this.created = created;
        return this;
    }
}
