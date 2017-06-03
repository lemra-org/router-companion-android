package org.rm3l.router_companion.resources

/**
 * Created by rm3l on 07/09/15.
 */
class PublicIPInfo {

  private var ip: String? = null

  fun getIp(): String? {
    return ip
  }

  fun setIp(ip: String?): PublicIPInfo {
    this.ip = ip
    return this
  }

  override fun toString(): String {
    return "PublicIPInfo {ip='$ip'}"
  }

  companion object {

    val IPIFY_API_RAW = "https://api.ipify.org"
    val IPIFY_API_JSON = "https://api.ipify.org?format=json"

    val ICANHAZIP_HOST = "icanhazip.com"
    val ICANHAZIP_PORT = 80

    val ICANHAZPTR_HOST = "icanhazptr.com"
    val ICANHAZPTR_PORT = 80
  }
}
