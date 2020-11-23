@file:JvmName("JsonElementUtils")
package org.rm3l.router_companion.utils.kotlin

import com.google.gson.Gson
import com.google.gson.JsonElement

inline fun <reified T> JsonElement?.parseAs(): T? where T : Any {
    return this?.parseAs(T::class.java)
}

fun <T> JsonElement?.parseAs(type: Class<T>): T? where T : Any {
    val jsonElement = this ?: return null
    return Gson().fromJson(jsonElement.asJsonObject.toString(), type)
}
