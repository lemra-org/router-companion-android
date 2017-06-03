package org.rm3l.router_companion.resources

import java.util.ArrayList

/**
 * Created by rm3l on 21/01/16.
 */
class WANAccessPolicy {
  val blockedServices: List<String?>? = ArrayList()
  val blockedWebsitesByUrl: List<String?>? = ArrayList()
  val blockedWebsitesByKeyword: List<String?>? = ArrayList()
  private var number: Int = 0
  private var name: String? = null
  private var status: String? = null
  private var timeOfDay: String? = null
  /**
   * S M T W T F S

   * 1 if enabled, 0 otherwise

   * 7 if "All days"
   */
  private var daysPattern: String? = null
  private var denyOrFilter: String? = null

  fun getNumber(): Int {
    return number
  }

  fun setNumber(number: Int): WANAccessPolicy {
    this.number = number
    return this
  }

  fun getName(): String? {
    return name
  }

  fun setName(name: String?): WANAccessPolicy {
    this.name = name
    return this
  }

  fun getStatus(): String? {
    return status
  }

  fun setStatus(status: String?): WANAccessPolicy {
    this.status = status
    return this
  }

  fun getTimeOfDay(): String? {
    return timeOfDay
  }

  fun setTimeOfDay(timeOfDay: String?): WANAccessPolicy {
    this.timeOfDay = timeOfDay
    return this
  }

  fun getDaysPattern(): String? {
    return daysPattern
  }

  fun setDaysPattern(daysPattern: String?): WANAccessPolicy {
    this.daysPattern = daysPattern
    return this
  }

  fun getDenyOrFilter(): String? {
    return denyOrFilter
  }

  fun setDenyOrFilter(denyOrFilter: String?): WANAccessPolicy {
    this.denyOrFilter = denyOrFilter
    return this
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false

    val that = other as WANAccessPolicy?

    if (number != that!!.number) return false
    if (if (name != null) name != that.name else that.name != null) return false
    if (if (status != null) status != that.status else that.status != null) return false
    if (if (timeOfDay != null) timeOfDay != that.timeOfDay else that.timeOfDay != null) {
      return false
    }
    if (if (daysPattern != null) daysPattern != that.daysPattern else that.daysPattern != null) {
      return false
    }
    if (if (denyOrFilter != null)
      denyOrFilter != that.denyOrFilter
    else
      that.denyOrFilter != null) {
      return false
    }
    if (if (blockedServices != null)
      blockedServices != that.blockedServices
    else
      that.blockedServices != null) {
      return false
    }
    if (if (blockedWebsitesByUrl != null)
      blockedWebsitesByUrl != that.blockedWebsitesByUrl
    else
      that.blockedWebsitesByUrl != null) {
      return false
    }
    return if (blockedWebsitesByKeyword != null)
      blockedWebsitesByKeyword == that.blockedWebsitesByKeyword
    else
      that.blockedWebsitesByKeyword == null
  }

  override fun hashCode(): Int {
    var result = number
    result = 31 * result + if (name != null) name!!.hashCode() else 0
    result = 31 * result + if (status != null) status!!.hashCode() else 0
    result = 31 * result + if (timeOfDay != null) timeOfDay!!.hashCode() else 0
    result = 31 * result + if (daysPattern != null) daysPattern!!.hashCode() else 0
    result = 31 * result + if (denyOrFilter != null) denyOrFilter!!.hashCode() else 0
    result = 31 * result + (blockedServices?.hashCode() ?: 0)
    result = 31 * result + (blockedWebsitesByUrl?.hashCode() ?: 0)
    result = 31 * result + (blockedWebsitesByKeyword?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "WANAccessPolicy{number=$number, name='$name', status='$status', timeOfDay='$timeOfDay', daysPattern='$daysPattern', denyOrFilter='$denyOrFilter', blockedServices=$blockedServices, blockedWebsitesByUrl=$blockedWebsitesByUrl, blockedWebsitesByKeyword=$blockedWebsitesByKeyword}"
  }

  companion object {

    val DENY = "Deny"
    val FILTER = "Filter"

    val STATUS_UNKNOWN = "unknown"
  }
}
