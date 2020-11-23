package org.rm3l.router_companion.firmwares

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.rm3l.router_companion.resources.MonthlyCycleItem
import org.rm3l.router_companion.resources.conn.NVRAMInfo
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.tiles.DDWRTTile
import org.rm3l.router_companion.tiles.admin.accessrestrictions.WANAccessPoliciesRouterData
import org.rm3l.router_companion.utils.Utils
import java.lang.reflect.InvocationTargetException

class NoNewFirmwareUpdate : RuntimeException {
    constructor() : super()
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}

abstract class AbstractRouterFirmwareConnector {

    protected fun updateProgressBarViewSeparator(
        dataRetrievalListener: RemoteDataRetrievalListener?,
        progress: Int
    ) {
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

    fun <T : DDWRTTile<*>> getDataFor(
        context: Context,
        router: Router,
        tile: Class<T>,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): NVRAMInfo {
        try {
            return this.javaClass
                .getDeclaredMethod(
                    "getDataFor${tile.simpleName}", Context::class.java,
                    Router::class.java,
                    RemoteDataRetrievalListener::class.java
                )
                .invoke(this, context, router, dataRetrievalListener) as NVRAMInfo
        } catch (e: IllegalAccessException) {
            FirebaseCrashlytics.getInstance().recordException(e)
            throw IllegalStateException(e)
        } catch (e: InvocationTargetException) {
            FirebaseCrashlytics.getInstance().recordException(e)
            throw IllegalStateException(e)
        } catch (e: NoSuchMethodException) {
            FirebaseCrashlytics.getInstance().recordException(e)
            throw IllegalStateException(e)
        }
    }

    @Throws(Exception::class)
    abstract fun getDataForNetworkTopologyMapTile(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): NVRAMInfo

    @Throws(Exception::class)
    abstract fun getWanPublicIpAddress(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): String?

    @Throws(Exception::class)
    abstract fun getRouterName(context: Context, router: Router): String?

    @Throws(Exception::class)
    abstract fun getWanIpAddress(context: Context, router: Router): String?

    @Throws(Exception::class)
    abstract fun getLanIpAddress(context: Context, router: Router): String?

    @Throws(Exception::class)
    protected abstract fun goGetRouterModel(context: Context, router: Router): String?

    @Throws(Exception::class)
    abstract fun getDataForWANTotalTrafficOverviewTile(
        context: Context,
        router: Router,
        cycleItem: MonthlyCycleItem,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): NVRAMInfo

    @Throws(Exception::class)
    abstract fun getDataForUptimeTile(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): NVRAMInfo

    @Throws(Exception::class)
    abstract fun getDataForMemoryAndCpuUsageTile(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): List<Array<String>>

    @Throws(Exception::class)
    abstract fun getDataForStorageUsageTile(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): NVRAMInfo

    @Throws(Exception::class)
    abstract fun getDataForStatusRouterStateTile(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): NVRAMInfo

    abstract fun getScmChangesetUrl(changeset: String): String?

    @Throws(Exception::class)
    abstract fun getWANAccessPolicies(
        context: Context,
        router: Router,
        dataRetrievalListener: RemoteDataRetrievalListener?
    ): WANAccessPoliciesRouterData?

    abstract fun manuallyCheckForFirmwareUpdateAndReturnDownloadLink(currentFwVer: String?): FirmwareRelease?
}

abstract class FirmwareRelease protected constructor(val version: String) {
    abstract fun getDirectLink(): String
}
