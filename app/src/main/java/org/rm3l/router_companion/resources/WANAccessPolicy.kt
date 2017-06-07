package org.rm3l.router_companion.resources

/**
 * Created by rm3l on 21/01/16.
 */
data class WANAccessPolicy(
  var blockedServices: List<String> = emptyList<String>(),
  var blockedWebsitesByUrl: List<String> = emptyList<String>(),
  var blockedWebsitesByKeyword: List<String> = emptyList<String>(),
  var number: Int = 0,
  var name: String? = null,
  var status: String? = null,
  var timeOfDay: String? = null,
  /**
   * S M T W T F S

   * 1 if enabled, 0 otherwise

   * 7 if "All days"
   */
  var daysPattern: String? = null,
  var denyOrFilter: String? = null)
{
  companion object {
    val DENY = "Deny"
    val FILTER = "Filter"
    val STATUS_UNKNOWN = "unknown"
  }
}
