package org.rm3l.router_companion.api.graphql

data class GraphQLQuery @JvmOverloads constructor(
    val query: String,
    val variables: String? = null /*TODO*/,
    val operationName: String? = null /*TODO*/
)