package org.rm3l.router_companion.tasker.utils

import android.content.Context

class FirebaseUtils {

    companion object {

        @JvmStatic
        fun getFirebaseApiKey(context: Context) = context.getConfigProperty("FIREBASE_API_KEY", "")!!
    }
}
