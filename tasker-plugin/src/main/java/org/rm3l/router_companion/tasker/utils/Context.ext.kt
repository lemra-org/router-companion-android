@file:JvmName("ContextUtils")
package org.rm3l.router_companion.tasker.utils

import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.annotation.StringRes

fun Context?.getConfigProperty(identifier: String, defaultValue: String? = null): String? {
    if (this == null) {
        return defaultValue
    }
    val id = this.resources.getIdentifier(identifier, "string", this.packageName)
    if (id == 0) {
        return defaultValue
    }
    return try {
        this.resources.getString(id)
    } catch (rnfe: Resources.NotFoundException) {
        Log.d(this.javaClass.simpleName, "Resource $identifier of type string not found in package ${this.packageName}", rnfe)
        defaultValue
    }
}

fun Context?.getConfigProperty(@StringRes identifier: Int, defaultValue: String? = null) = try {
    this?.resources?.getString(identifier) ?: defaultValue
} catch (rnfe: Resources.NotFoundException) {
    Log.d(this?.javaClass?.simpleName ?: "-", "Resource $identifier of type string not found in package ${this?.packageName}", rnfe)
    defaultValue
}
