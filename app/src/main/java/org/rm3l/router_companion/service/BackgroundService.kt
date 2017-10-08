package org.rm3l.router_companion.service

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.evernote.android.job.DailyJob
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import org.rm3l.ddwrt.BuildConfig
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.job.RouterCompanionJob
import org.rm3l.router_companion.mgmt.RouterManagementActivity
import org.rm3l.router_companion.service.tasks.AbstractBackgroundServiceTask
import org.rm3l.router_companion.service.tasks.ConnectedHostsServiceTask
import org.rm3l.router_companion.service.tasks.PublicIPChangesServiceTask
import org.rm3l.router_companion.service.tasks.RouterInfoForFeedbackServiceTask
import org.rm3l.router_companion.service.tasks.RouterModelUpdaterServiceTask
import org.rm3l.router_companion.service.tasks.RouterWebInterfaceParametersUpdaterServiceTask
import org.rm3l.router_companion.utils.Utils
import java.util.concurrent.TimeUnit

/**
 * Created by rm3l on 05/07/15.
 */
class BackgroundService(context: Context?) : DailyJob(), RouterCompanionJob {
  
  private val globalPreferences = context?.getSharedPreferences(
      RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
      Context.MODE_PRIVATE)
  private val dao = RouterManagementActivity.getDao(context)

  override fun onRunDailyJob(params: Params?): DailyJobResult {
    try {
      // check the global background data setting
      globalPreferences?.edit()
          ?.putLong(RouterCompanionAppConstants.BG_SERVICE_LAST_HANDLE, System.currentTimeMillis())
          ?.apply()

      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      if (cm.activeNetworkInfo != null) {
        /*
         * If getActiveNetworkInfo() is null, you do not have a network connection,
         * either because the device does not have a network connection,
         * or because user settings (e.g., bandwidth caps) prevent your app
         * from having a network connection.
         */
        //Do the actual work - no need to do so in a separate thread
        val tasks: MutableList<AbstractBackgroundServiceTask> = arrayListOf()
        tasks.add(RouterModelUpdaterServiceTask(context))
        tasks.add(RouterInfoForFeedbackServiceTask(context))
        tasks.add(RouterWebInterfaceParametersUpdaterServiceTask(context))
        //According to user preference
        val notificationsChoiceSet = globalPreferences?.getStringSet(
            RouterCompanionAppConstants.NOTIFICATIONS_CHOICE_PREF,
            emptySet()) ?: emptySet()
        Crashlytics.log(Log.DEBUG, TAG, "notificationsChoiceSet: " + notificationsChoiceSet)
        if (notificationsChoiceSet.contains(ConnectedHostsServiceTask::class.java.simpleName)) {
          tasks.add(ConnectedHostsServiceTask(context))
        }
        if (notificationsChoiceSet.contains(PublicIPChangesServiceTask::class.java.simpleName)) {
          tasks.add(PublicIPChangesServiceTask(context))
        }
        //Add any other tasks over here

        val allRouters = dao.allRouters
        for (router in allRouters) {
          //Execute tasks available for notifications
          for (backgroundServiceTask in tasks) {
            Crashlytics.log(Log.DEBUG, TAG,
                ">>> Running task: ${backgroundServiceTask.javaClass} on router $router")
            try {
              backgroundServiceTask.runBackgroundServiceTask(router)
            } catch (e: Exception) {
              Crashlytics.logException(e)
              //No worries
            }
          }
        }
      }
    } catch (e: Exception) {
      //No worries
      Crashlytics.logException(e)
    } finally {
      Utils.requestBackup(context)
    }
    return DailyJobResult.SUCCESS
  }

  companion object {
    @JvmField
    val TAG = BackgroundService::class.java.simpleName!!

    @JvmStatic
    fun schedule() {
      //This is a premium feature
      if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
        Crashlytics.log(Log.DEBUG, TAG, "Firmware Build Updates feature is *premium*!")
        return
      }

      if (!JobManager.instance().getAllJobRequestsForTag(TAG).isEmpty()) {
        // job already scheduled, nothing to do
        return
      }
      val builder = JobRequest.Builder(TAG).setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
      // run job between 6am and 5am
      DailyJob.schedule(builder, TimeUnit.HOURS.toMillis(6), TimeUnit.HOURS.toMillis(5))
    }
  }
}
