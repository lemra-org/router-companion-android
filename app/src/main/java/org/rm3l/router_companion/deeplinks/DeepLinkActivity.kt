package org.rm3l.router_companion.deeplinks

import android.app.Activity
import android.os.Bundle
import com.airbnb.deeplinkdispatch.DeepLinkHandler

/**
 * Created by rm3l on 7/22/17.
 */
@DeepLinkHandler(AppDeepLinkModule::class)
class DeepLinkActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // DeepLinkDelegate, LibraryDeepLinkModuleLoader and AppDeepLinkModuleLoader
    // are generated at compile-time.
    val deepLinkDelegate = DeepLinkDelegate(AppDeepLinkModuleLoader())
    // Delegate the deep link handling to DeepLinkDispatch.
    // It will start the correct Activity based on the incoming Intent URI
    deepLinkDelegate.dispatchFrom(this)
    // Finish this Activity since the correct one has been just started
    finish()
  }
}