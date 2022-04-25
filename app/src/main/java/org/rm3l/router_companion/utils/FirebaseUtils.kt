package org.rm3l.router_companion.utils

import android.content.Context
import org.rm3l.router_companion.utils.kotlin.getConfigProperty

class FirebaseUtils {

    companion object {

        @JvmStatic
        fun getFirebaseApiKey(context: Context) = context.getConfigProperty("FIREBASE_API_KEY", "")!!
    }
}
