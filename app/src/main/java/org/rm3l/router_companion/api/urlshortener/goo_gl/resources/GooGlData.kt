package org.rm3l.router_companion.api.urlshortener.goo_gl.resources

/**
 * Created by rm3l on 02/08/16.
 */
@Deprecated("goo.gl is now deprecated and returns an error = use FirebaseDynamicLinksService instead")
data class GooGlData(
        var kind: String? = null,
        var id: String? = null,
        var longUrl: String? = null,
        var status: String? = null,
        var created: String? = null
)
