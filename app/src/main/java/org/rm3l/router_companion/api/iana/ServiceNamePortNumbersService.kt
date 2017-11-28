package org.rm3l.router_companion.api.iana

import org.rm3l.ddwrt.BuildConfig
import org.rm3l.router_companion.api.graphql.GraphQLQuery
import org.rm3l.router_companion.utils.retrofit.Retry
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ServiceNamePortNumbersService {

    @Retry
    @Headers("Content-Type: application/json",
            "User-Agent: ${BuildConfig.APPLICATION_ID} v ${BuildConfig.VERSION_NAME}")
    @POST("/graphql")
    fun query(@Body graphqlQuery: GraphQLQuery): Call<RecordListResponse>
}