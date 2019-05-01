package org.rm3l.router_companion.tasker.api.urlshortener.firebase.dynamiclinks.resources;

public class ShortLinksDataResponse {
    private String previewLink;
    private String shortLink;

    public String getPreviewLink() {
        return previewLink;
    }

    public ShortLinksDataResponse setPreviewLink(final String previewLink) {
        this.previewLink = previewLink;
        return this;
    }

    public String getShortLink() {
        return shortLink;
    }

    public ShortLinksDataResponse setShortLink(final String shortLink) {
        this.shortLink = shortLink;
        return this;
    }
}
