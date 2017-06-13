package org.rm3l.router_companion.utils.kotlin

import android.content.res.AssetManager
import java.nio.charset.Charset

fun AssetManager.fileAsString(subdirectory: String, filename: String): String {
  return open("$subdirectory/$filename").use {
    it.readBytes().toString(Charset.defaultCharset())
  }
}
