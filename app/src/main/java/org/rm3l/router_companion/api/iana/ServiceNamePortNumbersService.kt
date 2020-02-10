package org.rm3l.router_companion.api.iana

import com.google.gson.GsonBuilder
import org.rm3l.router_companion.api.graphql.GraphQLQuery
import org.rm3l.router_companion.utils.retrofit.Retry
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ServiceNamePortNumbersService {
    @Retry
    @Headers("Content-Type: application/json")
    @POST("/graphql")
    fun query(@Body graphqlQuery: GraphQLQuery): Call<RecordListResponse>

    companion object {

        private val gson = GsonBuilder().create()

        fun toGraphQLQuery(
            ports: Collection<Long>? = null,
            protocols: Collection<Protocol>? = null,
            services: Collection<String>? = null
        ): GraphQLQuery {
            val graphQLQuery = "{\n" +
                    "records (filter: {ports: " +
                    gson.toJson(ports ?: emptyList<Long>()) +
                    ", protocols: " +
                    gson.toJson(protocols ?: emptyList<Protocol>()).replace("\"".toRegex(), "") +
                    ", services: " +
                    gson.toJson(services ?: emptyList<String>()) +
                    "}) {\n" +
                    "serviceName\n" +
                    "portNumber\n" +
                    "transportProtocol\n" +
                    "description\n" +
                    "}\n" +
                    "}"
            return GraphQLQuery(query = graphQLQuery)
        }
    }
}

fun ServiceNamePortNumbersService.query(
    ports: Collection<Long>? = null,
    protocols: Collection<Protocol>? = null,
    services: Collection<String>? = null
) =
        this.query(ServiceNamePortNumbersService.toGraphQLQuery(ports, protocols, services))