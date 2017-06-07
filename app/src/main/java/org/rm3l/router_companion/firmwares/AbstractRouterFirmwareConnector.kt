package org.rm3l.router_companion.firmwares

import android.content.Context
import android.content.SharedPreferences
import com.crashlytics.android.Crashlytics
import java.lang.reflect.InvocationTargetException
import org.rm3l.router_companion.resources.MonthlyCycleItem
import org.rm3l.router_companion.resources.conn.NVRAMInfo
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.tiles.DDWRTTile
import org.rm3l.router_companion.tiles.admin.accessrestrictions.WANAccessPoliciesRouterData
import org.rm3l.router_companion.utils.Utils

/**
 * Created by rm3l on 08/01/2017.
 */
abstract class AbstractRouterFirmwareConnector {

  protected fun updateProgressBarViewSeparator(
      dataRetrievalListener: RemoteDataRetrievalListener?, progress: Int) {
    if (dataRetrievalListener == null) {
      return
    }
    dataRetrievalListener.onProgressUpdate(Math.min(Math.max(0, progress), 100))
  }

  @Throws(Exception::class)
  fun getRouterModel(context: Context, router: Router): String? {
    val routerModel = this.goGetRouterModel(context, router)
    val routerPreferences = router.getPreferences(context)
    if (routerPreferences != null) {
      routerPreferences.edit().putString(NVRAMInfo.MODEL, routerModel).apply()
      Utils.requestBackup(context)
    }
    return routerModel
  }

  fun <T : DDWRTTile<*>> getDataFor(context: Context,
      router: Router, tile: Class<T>,
      dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo {
    try {
      return this.javaClass
          .getDeclaredMethod("getDataFor${tile.simpleName}", Context::class.java,
              Router::class.java,
              RemoteDataRetrievalListener::class.java)
          .invoke(this, context, router, dataRetrievalListener) as NVRAMInfo
    } catch (e: IllegalAccessException) {
      Crashlytics.logException(e)
      throw IllegalStateException(e)
    } catch (e: InvocationTargetException) {
      Crashlytics.logException(e)
      throw IllegalStateException(e)
    } catch (e: NoSuchMethodException) {
      Crashlytics.logException(e)
      throw IllegalStateException(e)
    }

  }

  @Throws(Exception::class)
  abstract fun getDataForNetworkTopologyMapTile(context: Context,
      router: Router, dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo

  @Throws(Exception::class)
  abstract fun getWanPublicIpAddress(context: Context, router: Router,
      dataRetrievalListener: RemoteDataRetrievalListener?): String?

  @Throws(Exception::class)
  abstract fun goGetRouterModel(context: Context, router: Router): String?

  @Throws(Exception::class)
  abstract fun getDataForWANTotalTrafficOverviewTile(context: Context,
      router: Router, cycleItem: MonthlyCycleItem,
      dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo

  @Throws(Exception::class)
  abstract fun getDataForUptimeTile(context: Context, router: Router,
      dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo

  @Throws(Exception::class)
  abstract fun getDataForMemoryAndCpuUsageTile(context: Context,
      router: Router, dataRetrievalListener: RemoteDataRetrievalListener?): List<Array<String>>

  @Throws(Exception::class)
  abstract fun getDataForStorageUsageTile(context: Context,
      router: Router, dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo

  @Throws(Exception::class)
  abstract fun getDataForStatusRouterStateTile(context: Context,
      router: Router, dataRetrievalListener: RemoteDataRetrievalListener?): NVRAMInfo

  abstract fun getScmChangesetUrl(changeset: String): String?

  @Throws(Exception::class)
  abstract fun getWANAccessPolicies(context: Context,
      router: Router,
      dataRetrievalListener: RemoteDataRetrievalListener?): WANAccessPoliciesRouterData?
}
