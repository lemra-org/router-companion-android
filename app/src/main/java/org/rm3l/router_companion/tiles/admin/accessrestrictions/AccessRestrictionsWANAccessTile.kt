package org.rm3l.router_companion.tiles.admin.accessrestrictions

import android.app.AlertDialog
import android.graphics.Point
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.ContextCompat
import android.support.v4.content.Loader
import android.support.v4.view.ViewCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.github.curioustechizen.ago.RelativeTimeTextView
import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.base.Throwables
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.find
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style
import java.util.ArrayList
import java.util.Collections
import java.util.Locale
import java.util.Properties
import org.rm3l.ddwrt.BuildConfig
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.actions.ActionManager
import org.rm3l.router_companion.actions.RouterAction
import org.rm3l.router_companion.actions.RouterActionListener
import org.rm3l.router_companion.actions.SetNVRAMVariablesAction
import org.rm3l.router_companion.actions.ToggleWANAccessPolicyRouterAction
import org.rm3l.router_companion.common.utils.ViewIDUtils
import org.rm3l.router_companion.exceptions.DDWRTCompanionException
import org.rm3l.router_companion.exceptions.DDWRTNoDataException
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException
import org.rm3l.router_companion.resources.RouterData
import org.rm3l.router_companion.resources.WANAccessPolicy
import org.rm3l.router_companion.resources.conn.NVRAMInfo
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.tiles.DDWRTTile
import org.rm3l.router_companion.utils.ColorUtils
import org.rm3l.router_companion.utils.ImageUtils
import org.rm3l.router_companion.utils.SSHUtils
import org.rm3l.router_companion.utils.Utils
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils
import org.rm3l.router_companion.widgets.RecyclerViewEmptySupport

import org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING
import org.rm3l.router_companion.actions.ToggleWANAccessPolicyRouterAction.DISABLE
import org.rm3l.router_companion.actions.ToggleWANAccessPolicyRouterAction.ENABLE_1
import org.rm3l.router_companion.actions.ToggleWANAccessPolicyRouterAction.ENABLE_2
import org.rm3l.router_companion.tiles.admin.accessrestrictions.AccessRestrictionsWANAccessTile.Companion
import org.rm3l.router_companion.utils.kotlin.isThemeLight

/**
 * WAN Access Policies tile

 * https://github.com/mirror/dd-wrt/blob/master/src/router/httpd/visuals/filters.c

 * See http://www.dd-wrt.com/phpBB2/viewtopic.php?p=460996 for instructions on how to manipulate WAN
 * Access Policies:

 * <pre>
 * Ok I have it working. Here is what I did in case some else wants to use it.
 * Set your access policy with the web interface and save it. In my case I saved it disabled to rule
 * 1.Then to enable or disable it I telnet to the router and use the following commands.
 * note: rule1 = rule1, rule2 = 2 etc.
 * STAT:1 = enable STAT:2 = disable

 * To disable access policy
 * root@DD-WRT:~# nvram set filter_rule1=\$STAT:2\$NAME:NoInet\$DENY:1\$$
 * nvram commit (if you want the change to be permanent)
 * root@DD-WRT:~# stopservice firewall
 * root@DD-WRT:~# startservice firewall

 * To enable enable access policy
 * root@DD-WRT:~# nvram set filter_rule1=\$STAT:1\$NAME:NoInet\$DENY:1\$$
 * nvram commit (if you want the change to be permanent)
 * root@DD-WRT:~# stopservice firewall
 * root@DD-WRT:~# startservice firewall
</pre> *

 * Created by rm3l on 20/01/16.
 */
class AccessRestrictionsWANAccessTile(parentFragment: Fragment, arguments: Bundle?, router: Router?) :
    DDWRTTile<WANAccessPoliciesRouterData>(
        parentFragment, arguments, router, R.layout.tile_admin_access_restrictions_wan_access, null),
    PopupMenu.OnMenuItemClickListener, AnkoLogger {

  private val addNewButton: FloatingActionButton

  private val mRecyclerView = layout.find<RecyclerViewEmptySupport>(
      R.id.tile_admin_access_restrictions_wan_access_ListView)
  private val mAdapter: RecyclerView.Adapter<*>
  private val mLayoutManager: RecyclerView.LayoutManager

  private var mLastSync: Long = 0
  private var mMenu: Menu? = null

  init {

    // use this setting to improve performance if you know that changes
    // in content do not change the layout size of the RecyclerView
    // allows for optimizations if all items are of the same size:
    mRecyclerView.setHasFixedSize(true)

    // use a linear layout manager
    mLayoutManager = LinearLayoutManager(mParentFragmentActivity, LinearLayoutManager.VERTICAL,
        false)
    mLayoutManager.scrollToPosition(0)
    mRecyclerView.layoutManager = mLayoutManager
    //
    val emptyView = layout.find<TextView>(R.id.empty_view)
    emptyView.textColor = ContextCompat.getColor(mParentFragmentActivity,
        if (mParentFragmentActivity?.isThemeLight()) R.color.black else R.color.white)
    mRecyclerView.setEmptyView(emptyView)

    // specify an adapter (see also next example)
    mAdapter = WANAccessRulesRecyclerViewAdapter(this)
    mAdapter.setHasStableIds(true)
    mRecyclerView.adapter = mAdapter

    val display = mParentFragmentActivity.windowManager.defaultDisplay
    val size = Point()
    display.getSize(size)
    val width = size.x
    val height = size.y
    debug("<width,height> = <$width,$height>")
    mRecyclerView.minimumHeight = size.y

    addNewButton = layout.find<FloatingActionButton>(R.id.wan_access_restriction_policy_add)

    addNewButton.setOnClickListener {
      mParentFragmentActivity.toast("TODO Add new WAN Access Policy")
    }

    //Create Options Menu
    val tileMenu = layout.find<ImageButton>(R.id.tile_admin_access_restrictions_wan_access_menu)

    if (mParentFragmentActivity?.isThemeLight()) {
      //Set menu background to white
      tileMenu.imageResource = R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark
    }

    tileMenu.setOnClickListener { v ->
      val popup = PopupMenu(mParentFragmentActivity, v)
      popup.setOnMenuItemClickListener(this@AccessRestrictionsWANAccessTile)
      val inflater = popup.menuInflater
      mMenu = popup.menu
      inflater.inflate(R.menu.tile_access_restrictions_wan_access_options, mMenu)
      popup.show()
    }

  }

  fun canChildScrollUp(): Boolean {
    val canScrollVertically = ViewCompat.canScrollVertically(mRecyclerView, -1)
    if (!canScrollVertically) {
      return canScrollVertically
    }

    //TODO ScrollView can scroll vertically,
    // but detect whether the touch was done outside of the scroll view
    // (in which case we should return false)

    return canScrollVertically
  }

  override fun isEmbeddedWithinScrollView(): Boolean {
    return false
  }

  override fun getTileHeaderViewId(): Int {
    return R.id.tile_admin_access_restrictions_wan_access_hdr
  }

  override fun getTileTitleViewId(): Int {
    return R.id.tile_admin_access_restrictions_wan_access_title
  }

  override fun getLogTag(): String? {
    return LOG_TAG
  }

  override fun getOnclickIntent(): OnClickIntent? {
    return null
  }

  override fun getLoader(id: Int,
      args: Bundle?): Loader<WANAccessPoliciesRouterData>? {
    return object : AsyncTaskLoader<WANAccessPoliciesRouterData>(this.mParentFragmentActivity) {
      override fun loadInBackground(): WANAccessPoliciesRouterData? {
        try {
          Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for "
              + AccessRestrictionsWANAccessTile::class.java
              + ": routerInfo=$mRouter / nbRunsLoader=$nbRunsLoader")

          if (mRefreshing.getAndSet(true)) {
            throw DDWRTTileAutoRefreshNotAllowedException()
          }
          nbRunsLoader++

          updateProgressBarViewSeparator(0)

          mLastSync = System.currentTimeMillis()

          val wanAccessPolicies = ArrayList<WANAccessPolicy>()

          if (Utils.isDemoRouter(mRouter)) {
            (1..10).mapTo(wanAccessPolicies) {
              WANAccessPolicy().setNumber(it).setName("myWanPolicy " + it)
              //TODO Add other properties here
            }
          } else {
            updateProgressBarViewSeparator(10)

            //1- Get all rules first

            /*
                        filter_rule10=$STAT:1$NAME:myPolicy10$DENY:1$$
                        filter_rule1=$STAT:0$NAME:myPolicy1$DENY:0$$
                        filter_rule2=$STAT:2$NAME:myPolicy2$DENY:0$$
                        filter_rule3=
                        filter_rule4=
                        filter_rule5=
                        filter_rule6=
                        filter_rule7=$STAT:1$NAME:myPolicy7$DENY:1$$

                        filter_rule1=$STAT:1$NAME:Only allow preset IP-addresses$DENY:1$$
                        filter_rule2=$STAT:1$NAME:Inget internet p▒ natten$DENY:1$$
                        filter_rule3=$STAT:1$NAME:Paus mitt p▒ dagen$DENY:1$$
                        filter_rule4=$STAT:1$NAME:Skoldag$DENY:1$$
                        filter_rule5=
                        filter_rule6=
                        filter_rule7=
                        filter_rule8=
                        filter_rule9=
                        filter_rule10=
                         */
            var nvramInfo = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                mGlobalPreferences, "filter_rule.*")
            val properties: Properties? = nvramInfo?.getData()
            if (nvramInfo == null || properties == null) {
              return null
            }

            var i = 2
            var todPattern: String
            val entries = properties.entries
            for ((key, value) in entries) {
              if (key == null || value == null) {
                continue
              }
              //Skip empty rules
              val valueStr = value.toString()
              if (Strings.isNullOrEmpty(valueStr)) {
                continue
              }
              val keyStr = key.toString()
              val keyNb = Integer.parseInt(keyStr.replace("filter_rule", "").trim { it <= ' ' })

              val wanAccessPolicy = WANAccessPolicy().setNumber(keyNb)

              val statusSplitter = Splitter.on(
                  "\$NAME:").omitEmptyStrings().trimResults().splitToList(valueStr)
              if (!statusSplitter.isEmpty()) {
                //myPolicy7$DENY:1$$
                wanAccessPolicy.setStatus(statusSplitter[0].replace("\$STAT:".toRegex(), ""))
                if (statusSplitter.size >= 2) {
                  val nameAndFollowingStr = statusSplitter[1]
                  val nameAndFollowingSplitter = Splitter.on("\$DENY:")
                      .omitEmptyStrings()
                      .trimResults()
                      .splitToList(nameAndFollowingStr)
                  if (!nameAndFollowingSplitter.isEmpty()) {
                    wanAccessPolicy.setName(nameAndFollowingSplitter[0])
                    if (nameAndFollowingSplitter.size >= 2) {
                      //1$$
                      val s = nameAndFollowingSplitter[1].replace("\\$\\$".toRegex(), "")
                      if ("0" == s) {
                        wanAccessPolicy.setDenyOrFilter(WANAccessPolicy.FILTER)
                      } else {
                        wanAccessPolicy.setDenyOrFilter(WANAccessPolicy.DENY)
                      }
                    }
                  }
                }
              } else {
                wanAccessPolicy.setStatus(WANAccessPolicy.STATUS_UNKNOWN)
              }

              //2- For each, retrieve Time of Day (TOD)
              nvramInfo = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                  mGlobalPreferences, "filter_tod_buf" + keyNb)

              updateProgressBarViewSeparator(10 + i++)

              if (nvramInfo != null && nvramInfo.getProperty("filter_tod_buf" + keyNb) != null) {

                todPattern = nvramInfo.getProperty("filter_tod_buf" + keyNb) as String
                if ("7" == todPattern) {
                  todPattern = "1 1 1 1 1 1 1"
                }
                wanAccessPolicy.setDaysPattern(todPattern)
              }

              nvramInfo = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter,
                  mGlobalPreferences, "filter_tod" + keyNb)

              updateProgressBarViewSeparator(10 + i++)

              if (nvramInfo != null && nvramInfo.getProperty("filter_tod" + keyNb) != null) {
                /*
                                filter_tod4=0:0 23:59 0-6
                                filter_tod5=0:0 23:59 0,2,6
                                filter_tod6=0:0 23:59 0-1
                                filter_tod7=6:0 18:0 0-6
                                 */
                val filterTod = nvramInfo.getProperty("filter_tod" + keyNb)
                val list = Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(
                    filterTod!!)
                if (list.size >= 2) {
                  val start = list[0]
                  val end = list[1]
                  if ("0:0" == start && "23:59" == end) {
                    wanAccessPolicy.setTimeOfDay("24 Hours")
                  } else {
                    wanAccessPolicy.setTimeOfDay(
                        String.format(Locale.US, "from %s to %s", getHourFormatted(start),
                            getHourFormatted(end)))
                  }
                }
              }

              Crashlytics.log(Log.DEBUG, LOG_TAG, "wanAccessPolicy: " + wanAccessPolicy)

              wanAccessPolicies.add(wanAccessPolicy)

              updateProgressBarViewSeparator(10 + i++)
            }

            updateProgressBarViewSeparator(80)
          }

          val routerData = WANAccessPoliciesRouterData().setData(
              wanAccessPolicies) as WANAccessPoliciesRouterData

          updateProgressBarViewSeparator(90)

          return routerData
        } catch (e: Exception) {
          e.printStackTrace()
          return WANAccessPoliciesRouterData().setException(e) as WANAccessPoliciesRouterData
        }

      }
    }
  }

  override fun onLoadFinished(loader: Loader<WANAccessPoliciesRouterData>,
      dataFromLoader: WANAccessPoliciesRouterData?) {
    var data = dataFromLoader

    try {
      //Set tiles
      Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=$loader / data=$data")

      if (data == null) {
        data = WANAccessPoliciesRouterData().setException(
            DDWRTNoDataException("No Data!")) as WANAccessPoliciesRouterData
      }

      val wanAccessPolicies = data.getData()
      var exception = data.getException()
      if (exception == null && wanAccessPolicies == null) {
        data = WANAccessPoliciesRouterData().setException(
            DDWRTNoDataException("No Data!")) as WANAccessPoliciesRouterData
      }
      exception = data.getException()

      layout.findViewById(
          R.id.tile_admin_access_restrictions_wan_access_loading_view).visibility = View.GONE

      val errorPlaceHolderView = this.layout.find<TextView>(
          R.id.tile_admin_access_restrictions_wan_access_error)

      if (exception !is DDWRTTileAutoRefreshNotAllowedException) {

        if (exception == null) {
          errorPlaceHolderView.visibility = View.GONE
        }

        (mAdapter as WANAccessRulesRecyclerViewAdapter).setWanAccessPolicies(wanAccessPolicies)
        mAdapter.notifyDataSetChanged()

        //Update last sync
        val lastSyncView = layout.findViewById(R.id.tile_last_sync) as RelativeTimeTextView
        lastSyncView.setReferenceTime(mLastSync)
        lastSyncView.prefix = "Last sync: "
      }

      if (exception != null && exception !is DDWRTTileAutoRefreshNotAllowedException) {

        val rootCause = Throwables.getRootCause(exception)
        errorPlaceHolderView.text = "Error: " + if (rootCause != null) rootCause.message else "null"
        val parentContext = this.mParentFragmentActivity
        errorPlaceHolderView.setOnClickListener {
          if (rootCause != null) {
            Toast.makeText(parentContext, rootCause.message, Toast.LENGTH_LONG).show()
          }
        }
        errorPlaceHolderView.visibility = View.VISIBLE
        updateProgressBarWithError()
      } else if (exception == null) {
        updateProgressBarWithSuccess()
      }

      Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!")
    } finally {
      mRefreshing.set(false)
      doneWithLoaderInstance(this, loader)
    }
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.tile_access_policy_add -> {
        run {
          //TODO
          mParentFragmentActivity.toast("TODO OnMenuItemClick: Add WAN Access Policy")
        }
        return true
      }
      else -> {
      }
    }
    return false
  }

  fun getRouter(): Router? {
    return this.mRouter
  }

  companion object {

    val LOG_TAG = AccessRestrictionsWANAccessTile::class.java.simpleName

    private val todHoursSplitter = Splitter.on(":").omitEmptyStrings().trimResults()

    private fun getHourFormatted(todHour: String): String {
      val stringList = todHoursSplitter.splitToList(todHour)
      if (stringList.size < 2) {
        return todHour
      }
      var hour = stringList[0]
      var minutes = stringList[1]
      if (hour.length == 1) {
        hour = "0" + hour
      }
      if (minutes.length == 1) {
        minutes = "0" + minutes
      }
      return hour + ":" + minutes
    }
  }
}

internal class WANAccessRulesRecyclerViewAdapter(
    private val tile: AccessRestrictionsWANAccessTile) :
    RecyclerView.Adapter<WANAccessRulesRecyclerViewHolder>() {

  private val wanAccessPolicies = ArrayList<WANAccessPolicy>()

  override fun getItemId(position: Int): Long {
    val itemAt: WANAccessPolicy = wanAccessPolicies[position]
    return ViewIDUtils.getStableId(WANAccessPolicy::class.java,
        Integer.toString(itemAt.getNumber()))
  }

  fun getWanAccessPolicies(): List<WANAccessPolicy> {
    return wanAccessPolicies
  }

  fun setWanAccessPolicies(
      wanAccessPolicies: List<WANAccessPolicy>?): WANAccessRulesRecyclerViewAdapter {
    this.wanAccessPolicies.clear()
    if (wanAccessPolicies != null) {
      this.wanAccessPolicies.addAll(wanAccessPolicies)
      Collections.sort(this.wanAccessPolicies) { lhs, rhs ->
        Integer.valueOf(lhs.getNumber())!!.compareTo(rhs.getNumber())
      }
    }
    return this
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WANAccessRulesRecyclerViewHolder {
    // create a new view
    val v = LayoutInflater.from(parent.context)
        .inflate(R.layout.tile_admin_access_restriction, parent, false)
    // set the view's size, margins, paddings and layout parameters
    // ...
    return WANAccessRulesRecyclerViewHolder(this.tile, v)
  }

  override fun onBindViewHolder(holder: WANAccessRulesRecyclerViewHolder, position: Int) {
    // - get element from your dataset at this position
    // - replace the contents of the view with that element
    if (position < 0 || position >= wanAccessPolicies.size) {
      Crashlytics.log(Log.DEBUG, AccessRestrictionsWANAccessTile.LOG_TAG, "invalid position for WAN Access Policy Adapter")
      return
    }
    val wanAccessPolicy = wanAccessPolicies[position]

    if (this.tile.mParentFragmentActivity.isThemeLight()) {
      holder.cardView.setCardBackgroundColor(
          ContextCompat.getColor(this.tile.mParentFragmentActivity,
              R.color.cardview_light_background))
    } else {
      holder.cardView.setCardBackgroundColor(
          ContextCompat.getColor(this.tile.mParentFragmentActivity,
              R.color.cardview_dark_background))
    }

    val textDrawable = ImageUtils.getTextDrawable(wanAccessPolicy.getName())
    if (textDrawable == null) {
      holder.avatarImageView.visibility = View.GONE
    } else {
      holder.avatarImageView.visibility = View.VISIBLE
      holder.avatarImageView.setImageDrawable(textDrawable)
    }

    holder.policyNb.text = wanAccessPolicy.getNumber().toString()
    holder.policyName.text = wanAccessPolicy.getName()

    var daysPattern = wanAccessPolicy.getDaysPattern()
    if (daysPattern != null) {
      if ("7" == daysPattern) {
        daysPattern = "1 1 1 1 1 1 1" //Everyday
      }
      val timeOfDayBufferList = Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(
          daysPattern)
      for (i in timeOfDayBufferList.indices) {
        val todStatus = timeOfDayBufferList[i]
        if (i >= holder.daysTextViews.size) {
          continue
        }
        val daysTextView = holder.daysTextViews[i]
        daysTextView?.isEnabled = true
        if ("1" == todStatus) {
          daysTextView?.setBackgroundResource(R.drawable.table_border_selected)
        } else {
          daysTextView?.setBackgroundResource(R.drawable.table_border)
          if ("0" != todStatus) {
            daysTextView?.isEnabled = false
          }
        }
      }
    }

    holder.internetPolicyDuringSelectedTimeOfDay.text = wanAccessPolicy.getDenyOrFilter()

    holder.policyHours.text = wanAccessPolicy.getTimeOfDay()

    //Disable switch button listener
    holder.wanPolicyStateText.isEnabled = true
    val status = Strings.nullToEmpty(wanAccessPolicy.getStatus()).trim { it <= ' ' }
    val stateBgColor: Int?
    when (status) {
      "0", "\$STAT:0" -> {
        //Disabled
        stateBgColor = R.color.win8_red
        holder.wanPolicyStateText.text = "disabled"
      }
      "1", "\$STAT:1", "2", "\$STAT:2" -> {
        stateBgColor = R.color.win8_lime
        holder.wanPolicyStateText.text = "enabled"
      }
      else -> {
        stateBgColor = R.color.gray
        Utils.reportException(tile.mParentFragmentActivity,
            WANAccessPolicyException("status=[$status]"))
        holder.wanPolicyStateText.text = "unknown"
      }
    }
    holder.wanPolicyStateText.setBackgroundColor(
        ContextCompat.getColor(tile.mParentFragmentActivity, stateBgColor))

    holder.wanPolicyStateText.setOnClickListener { holder.menuImageButton.performClick() }

    val removeWanPolicyDialog = AlertDialog.Builder(tile.mParentFragmentActivity).setIcon(
        R.drawable.ic_action_alert_warning)
        .setTitle(String.format("Remove WAN Access Policy: '%s'?", wanAccessPolicy.getName()))
        .setMessage("Are you sure you wish to continue? ")
        .setCancelable(true)
        .setPositiveButton("Proceed!") { _, _ ->
          holder.wanPolicyStateText.isEnabled = false

          Utils.displayMessage(tile.mParentFragmentActivity,
              String.format("Deleting WAN Access Policy: '%s'...",
                  wanAccessPolicy.getName()), Style.INFO)

          val wanAccessPolicyNumber = wanAccessPolicy.getNumber()

          val nvramVarsToSet = NVRAMInfo().setProperty("filter_dport_grp" + wanAccessPolicyNumber,
              EMPTY_STRING)
              .setProperty("filter_ip_grp" + wanAccessPolicyNumber, EMPTY_STRING)
              .setProperty("filter_mac_grp" + wanAccessPolicyNumber, EMPTY_STRING)
              .setProperty("filter_p2p_grp" + wanAccessPolicyNumber, EMPTY_STRING)
              .setProperty("filter_port_grp" + wanAccessPolicyNumber, EMPTY_STRING)
              .setProperty("filter_rule" + wanAccessPolicyNumber, EMPTY_STRING)
              .setProperty("filter_tod" + wanAccessPolicyNumber, EMPTY_STRING)
              .setProperty("filter_tod_buf" + wanAccessPolicyNumber, EMPTY_STRING)
              .setProperty("filter_web_host" + wanAccessPolicyNumber, EMPTY_STRING)
              .setProperty("filter_web_url" + wanAccessPolicyNumber, EMPTY_STRING)

          ActionManager.runTasks(
              SetNVRAMVariablesAction(tile.getRouter(), tile.mParentFragmentActivity,
                  nvramVarsToSet, false, object : RouterActionListener {
                override fun onRouterActionSuccess(routerAction: RouterAction,
                    router: Router, returnData: Any) {
                  tile.mParentFragmentActivity.runOnUiThread {
                    try {
                      Utils.displayMessage(tile.mParentFragmentActivity, String.format(
                          "WAN Access Policy '%s' successfully deleted on host '%s'. ",
                          wanAccessPolicy.getName(),
                          tile.getRouter()!!.canonicalHumanReadableName), Style.CONFIRM)
                    } finally {
                      holder.wanPolicyStateText.isEnabled = true
                      wanAccessPolicies.remove(wanAccessPolicy)
                      //Success- update recycler view
                      notifyItemRemoved(holder.adapterPosition)
                    }
                  }
                }

                override fun onRouterActionFailure(routerAction: RouterAction,
                    router: Router, exception: Exception?) {
                  tile.mParentFragmentActivity.runOnUiThread {
                    try {
                      Utils.displayMessage(tile.mParentFragmentActivity, String.format(
                          "Error while trying to delete WAN Access Policy '%s' on '%s': %s",
                          wanAccessPolicy.getName(),
                          tile.getRouter()!!.canonicalHumanReadableName,
                          Utils.handleException(exception).first), Style.ALERT)
                    } finally {
                      holder.wanPolicyStateText.isEnabled = true
                    }
                  }
                }
              }, tile.mGlobalPreferences, "/sbin/stopservice firewall",
                  "/sbin/startservice firewall"))
        }
        .setNegativeButton("Cancel") { dialogInterface, i ->
          //Cancelled - nothing more to do!
        }
        .create()

    if (!ColorUtils.isThemeLight(tile.mParentFragmentActivity)) {
      //Set menu background to white
      holder.menuImageButton.setImageResource(
          R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark)
    }

    holder.menuImageButton.setOnClickListener { v ->
      val popup = PopupMenu(tile.mParentFragmentActivity, v)
      popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
        when (item.itemId) {
          R.id.tile_wan_access_policy_toggle -> {
            holder.wanPolicyStateText.performClick()
            return@OnMenuItemClickListener true
          }

          R.id.tile_wan_access_policy_enable, R.id.tile_wan_access_policy_disable -> {
            val enablePolicy = item.itemId == R.id.tile_wan_access_policy_enable
            if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
              Utils.displayUpgradeMessage(tile.mParentFragmentActivity,
                  String.format("%sable WAN Access Policy Restriction",
                      if (enablePolicy) "En" else "Dis"))
              return@OnMenuItemClickListener true
            }
            toggleWANAccessRestriction(holder, wanAccessPolicy, enablePolicy)
            return@OnMenuItemClickListener true
          }

          R.id.tile_wan_access_policy_remove -> {
            removeWanPolicyDialog.show()
            return@OnMenuItemClickListener true
          }
          R.id.tile_wan_access_policy_edit -> {
            //TODO Edit: open up edit popup or, better, a completely different setting activity
            Toast.makeText(tile.mParentFragmentActivity, "[TODO] Edit WAN Access Policy #"
                + wanAccessPolicy.getNumber()
                + " ("
                + wanAccessPolicy.getName()
                + ")", Toast.LENGTH_SHORT).show()
            return@OnMenuItemClickListener true
          }
          else -> {
          }
        }
        false
      })
      val inflater = popup.menuInflater
      val menu = popup.menu
      inflater.inflate(R.menu.tile_wan_access_policy_options, menu)
      popup.show()
    }

    holder.removeImageButton.setOnClickListener { removeWanPolicyDialog.show() }
  }

  override fun getItemCount(): Int {
    return wanAccessPolicies.size
  }

  private fun toggleWANAccessRestriction(holder: WANAccessRulesRecyclerViewHolder,
      wanAccessPolicy: WANAccessPolicy, newStatus: Boolean) {
    holder.wanPolicyStateText.isEnabled = false
    SnackbarUtils.buildSnackbar(tile.mParentFragmentActivity,
        tile.mParentFragmentActivity.findViewById(android.R.id.content),
        String.format("Going to %sable WAN Access Policy: '%s'", if (newStatus) "en" else "dis",
            wanAccessPolicy.getName()), "Undo", Snackbar.LENGTH_LONG, object : SnackbarCallback {
      @Throws(Exception::class)
      override fun onShowEvent(bundle: Bundle?) {
      }

      @Throws(Exception::class)
      override fun onDismissEventSwipe(event: Int, bundle: Bundle?) {
        //revert
      }

      @Throws(Exception::class)
      override fun onDismissEventActionClick(event: Int, bundle: Bundle?) {
        //revert
      }

      @Throws(Exception::class)
      override fun onDismissEventTimeout(event: Int, bundle: Bundle?) {
        Utils.displayMessage(tile.mParentFragmentActivity,
            String.format("%sabling WAN Access Policy: '%s'...", if (newStatus) "En" else "Dis",
                wanAccessPolicy.getName()), Style.INFO)
        val enableStatus = if (!newStatus)
          DISABLE
        else if (WANAccessPolicy.DENY == wanAccessPolicy.getDenyOrFilter())
          ENABLE_1
        else
          ENABLE_2
        ActionManager.runTasks(
            ToggleWANAccessPolicyRouterAction(tile.getRouter(), tile.mParentFragmentActivity,
                object : RouterActionListener {
                  override fun onRouterActionSuccess(routerAction: RouterAction,
                      router: Router, returnData: Any) {
                    tile.mParentFragmentActivity.runOnUiThread {
                      try {
                        Utils.displayMessage(tile.mParentFragmentActivity, String.format(
                            "WAN Access Policy '%s' successfully %s on host '%s'. ",
                            wanAccessPolicy.getName(), if (newStatus) "enabled" else "disabled",
                            tile.getRouter()!!.canonicalHumanReadableName), Style.CONFIRM)
                      } finally {
                        wanAccessPolicy.setStatus(Integer.toString(enableStatus))
                        this@WANAccessRulesRecyclerViewAdapter.notifyItemChanged(
                            holder.adapterPosition)
                      }
                    }
                  }

                  override fun onRouterActionFailure(routerAction: RouterAction,
                      router: Router, exception: Exception?) {
                    tile.mParentFragmentActivity.runOnUiThread {
                      try {
                        Utils.displayMessage(tile.mParentFragmentActivity, String.format(
                            "Error while trying to %s WAN Access Policy '%s' on '%s': %s",
                            if (newStatus) "enable" else "disable", wanAccessPolicy.getName(),
                            tile.getRouter()!!.canonicalHumanReadableName,
                            Utils.handleException(exception).first), Style.ALERT)
                      } finally {
                        //Revert
                      }
                    }
                  }
                }, tile.mGlobalPreferences, wanAccessPolicy, enableStatus))
      }

      @Throws(Exception::class)
      override fun onDismissEventManual(event: Int, bundle: Bundle?) {
        //Revert
      }

      @Throws(Exception::class)
      override fun onDismissEventConsecutive(event: Int, bundle: Bundle?) {
        //revert
      }
    }, null, true)
  }
}



internal class WANAccessRulesRecyclerViewHolder(
    private val tile: AccessRestrictionsWANAccessTile, itemView: View) :
    RecyclerView.ViewHolder(itemView) {

  val cardView = itemView.find<CardView>(R.id.access_restriction_policy_cardview)
  val policyNb = itemView.find<TextView>(R.id.access_restriction_policy_cardview_number)
  val policyName = itemView.find<TextView>(R.id.access_restriction_policy_cardview_name)
  val policyHours = itemView.find<TextView>(R.id.access_restriction_policy_cardview_hours)
  val internetPolicyDuringSelectedTimeOfDay = itemView
      .find<TextView>(R.id.access_restriction_policy_cardview_internet_policy)

  val daysTextViews = arrayOfNulls<TextView>(7)

  val wanPolicyStateText: TextView
  val menuImageButton: ImageButton
  val removeImageButton: ImageButton

  val avatarImageView = itemView.find<ImageView>(R.id.avatar)

  init {
    this.daysTextViews[0] = itemView.find<TextView>(
        R.id.access_restriction_policy_cardview_days_sunday)
    this.daysTextViews[1] = itemView.find<TextView>(
        R.id.access_restriction_policy_cardview_days_monday)
    this.daysTextViews[2] = itemView.find<TextView>(
        R.id.access_restriction_policy_cardview_days_tuesday)
    this.daysTextViews[3] = itemView.find<TextView>(
        R.id.access_restriction_policy_cardview_days_wednesday)
    this.daysTextViews[4] = itemView.find<TextView>(
        R.id.access_restriction_policy_cardview_days_thursday)
    this.daysTextViews[5] = itemView.find<TextView>(
        R.id.access_restriction_policy_cardview_days_friday)
    this.daysTextViews[6] = itemView.find<TextView>(
        R.id.access_restriction_policy_cardview_days_saturday)

    this.wanPolicyStateText = itemView.find<TextView>(
        R.id.access_restriction_policy_cardview_status)
    this.menuImageButton = itemView.find<ImageButton>(
        R.id.access_restriction_policy_cardview_menu)
    this.removeImageButton = itemView.find<ImageButton>(
        R.id.access_restriction_policy_remove_btn)
  }
}

class WANAccessPoliciesRouterData : RouterData<List<WANAccessPolicy>>()

class WANAccessPolicyException : DDWRTCompanionException {

  constructor(detailMessage: String?) : super(detailMessage)

  constructor(detailMessage: String?, throwable: Throwable?) : super(detailMessage, throwable)

  constructor(throwable: Throwable?) : super(throwable)
}
