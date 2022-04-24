package org.rm3l.router_companion.actions

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.util.Pair
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.common.base.Predicate
import com.google.common.base.Strings
import com.google.common.base.Strings.isNullOrEmpty
import com.google.common.base.Strings.nullToEmpty
import com.google.common.collect.FluentIterable
import needle.UiRelatedTask
import org.json.JSONObject
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.RouterCompanionAppConstants
import org.rm3l.router_companion.exceptions.StorageException
import org.rm3l.router_companion.main.DDWRTMainActivity.IMPORT_ALIASES_FRAGMENT_TAG
import org.rm3l.router_companion.main.DDWRTMainActivity.MAIN_ACTIVITY_ACTION
import org.rm3l.router_companion.mgmt.RouterManagementActivity
import org.rm3l.router_companion.multithreading.MultiThreadingManager
import org.rm3l.router_companion.resources.MACOUIVendor
import org.rm3l.router_companion.resources.RecyclerViewRefreshCause
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.tiles.status.wireless.WirelessClientsTile
import org.rm3l.router_companion.utils.ColorUtils
import org.rm3l.router_companion.utils.ImageUtils
import org.rm3l.router_companion.utils.PermissionsUtils
import org.rm3l.router_companion.utils.StorageUtils
import org.rm3l.router_companion.utils.Utils
import org.rm3l.router_companion.utils.Utils.fromHtml
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style.ALERT
import org.rm3l.router_companion.widgets.RecyclerViewEmptySupport
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class ManageRouterAliasesActivity :
    AppCompatActivity(),
    View.OnClickListener,
    SwipeRefreshLayout.OnRefreshListener,
    SearchView.OnQueryTextListener,
    SnackbarCallback {

    private var addNewButton: FloatingActionButton? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mIsThemeLight: Boolean = false
    private var mLayoutManager: RecyclerView.LayoutManager? = null
    private var mRecyclerView: RecyclerViewEmptySupport? = null
    private var mRouter: Router? = null
    private var mRouterPreferences: SharedPreferences? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mToolbar: Toolbar? = null
    private var optionsMenu: Menu? = null

    class AddOrUpdateRouterAliasDialogFragment : DialogFragment() {

        private var mAlias: CharSequence? = null

        private var mMacAddr: CharSequence? = null

        private var onClickListener: DialogInterface.OnClickListener? = null

        private var routerPreferences: SharedPreferences? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val arguments = arguments
            this.mMacAddr = arguments!!.getCharSequence(MAC_ADDRESS)
            this.mAlias = arguments.getCharSequence(ALIAS)

            val fragmentActivity = activity

            val routerUuid = arguments.getString(RouterManagementActivity.ROUTER_SELECTED)
            val router: Router? = if (routerUuid?.isBlank() == false) {
                RouterManagementActivity.getDao(fragmentActivity!!).getRouter(routerUuid)
            } else {
                null
            }
            if (router == null) {
                Toast.makeText(fragmentActivity, "Invalid Router", Toast.LENGTH_SHORT).show()
                dismiss()
                return
            }

            routerPreferences = fragmentActivity!!.getSharedPreferences(routerUuid, Context.MODE_PRIVATE)

            ColorUtils.setAppTheme(fragmentActivity, router.routerFirmware, false)
        }

        override fun onStart() {
            super.onStart() // super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point

            val d = dialog as AlertDialog?
            if (d != null) {
                val mMacAddrView = d.findViewById<View>(R.id.add_or_edit_router_alias_mac) as EditText
                mMacAddrView.setText(this.mMacAddr, TextView.BufferType.EDITABLE)

                val mAliasView = d.findViewById<View>(R.id.add_or_edit_router_alias_value) as EditText
                mAliasView.setText(this.mAlias, TextView.BufferType.EDITABLE)

                d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(
                    View.OnClickListener { view ->
                        // Validate data
                        val macEditText = d.findViewById<View>(R.id.add_or_edit_router_alias_mac) as EditText
                        val macValue = macEditText.text

                        val macValueToPersist = nullToEmpty(macValue.toString())?.toLowerCase()

                        if (Strings.isNullOrEmpty(macValueToPersist)) {
                            // Crouton
                            Utils.displayMessage(
                                activity!!, "MAC Address is required", ALERT,
                                d.findViewById<View>(
                                    R.id.add_or_edit_router_alias_notification_viewgroup
                                ) as ViewGroup
                            )
                            macEditText.requestFocus()
                            // Open Keyboard
                            val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(macEditText, 0)
                            return@OnClickListener
                        }
                        if (!Utils.MAC_ADDRESS.matcher(macValueToPersist).matches()) {
                            Utils.displayMessage(
                                activity!!, "MAC Address format required", ALERT,
                                d.findViewById<View>(
                                    R.id.add_or_edit_router_alias_notification_viewgroup
                                ) as ViewGroup
                            )
                            macEditText.requestFocus()
                            // Open Keyboard
                            val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(macEditText, 0)
                            return@OnClickListener
                        }
                        val aliasEditText = d.findViewById<View>(R.id.add_or_edit_router_alias_value) as EditText

                        if (TextUtils.isEmpty(aliasEditText.text)) {
                            // Crouton
                            Utils.displayMessage(
                                activity!!, "Alias cannot be blank", ALERT,
                                d.findViewById<View>(
                                    R.id.add_or_edit_router_alias_notification_viewgroup
                                ) as ViewGroup
                            )
                            aliasEditText.requestFocus()
                            // Open Keyboard
                            val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(aliasEditText, 0)
                            return@OnClickListener
                        }

                        routerPreferences!!.edit()
                            .putString(macValueToPersist, nullToEmpty(aliasEditText.text.toString()))
                            .apply()

                        if (onClickListener != null) {
                            onClickListener!!.onClick(d, view.id)
                        }

                        d.dismiss()
                    }
                )
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity = activity

            val builder = AlertDialog.Builder(activity)

            // Get the layout inflater
            val inflater = activity!!.layoutInflater

            val view = inflater.inflate(R.layout.add_or_edit_router_alias, null)
            val isNewAlias = Strings.isNullOrEmpty(
                if (this.mMacAddr != null) this.mMacAddr!!.toString() else null
            )
            builder.setTitle((if (isNewAlias) "Set" else "Update") + " Device Alias")
                .setMessage(
                    "Note that the Alias you define here is stored locally only, not on the router."
                )
                .setIcon(android.R.drawable.stat_sys_warning)
                .setView(view)
                // Add action buttons
                .setPositiveButton(
                    if (isNewAlias) "Set Alias" else "Update Alias"
                ) { dialog, id ->
                    // Do nothing here because we override this button later to change the close behaviour.
                    // However, we still need this because on older versions of Android unless we
                    // pass a handler the button doesn't get instantiated
                }
                .setNegativeButton(R.string.cancel) { dialog, id -> getDialog()!!.cancel() }

            return builder.create()
        }

        companion object {

            const val MAC_ADDRESS = "macAddress"

            const val ALIAS = "alias"

            fun newInstance(
                router: Router,
                mMacAddr: CharSequence?,
                mAlias: CharSequence?,
                onClickListener: DialogInterface.OnClickListener
            ): AddOrUpdateRouterAliasDialogFragment {
                val addOrUpdateRouterAliasDialogFragment = AddOrUpdateRouterAliasDialogFragment()
                val args = Bundle()
                args.putCharSequence(MAC_ADDRESS, mMacAddr)
                args.putCharSequence(ALIAS, mAlias)
                args.putString(RouterManagementActivity.ROUTER_SELECTED, router.uuid)

                addOrUpdateRouterAliasDialogFragment.arguments = args
                addOrUpdateRouterAliasDialogFragment.onClickListener = onClickListener

                return addOrUpdateRouterAliasDialogFragment
            }
        }
    }

    internal class RouterAliasesListRecyclerViewAdapter(
        private val context: ManageRouterAliasesActivity?,
        private val mRouter: Router
    ) : RecyclerView.Adapter<RouterAliasesListRecyclerViewAdapter.ViewHolder>(), Filterable {

        private var aliasesColl: List<Pair<String, String>>? = null

        private val mFilter: Filter

        private val mPreferences: SharedPreferences = this.context!!.getSharedPreferences(this.mRouter.uuid, Context.MODE_PRIVATE)

        internal class ViewHolder(myItemView: View) : RecyclerView.ViewHolder(myItemView) {
            val alias: TextView = this.itemView.findViewById(R.id.router_alias_alias)
            val aliasMenu: ImageButton = this.itemView.findViewById(R.id.router_alias_menu)
            val avatarImageView: ImageView = this.itemView.findViewById(R.id.avatar)
            val containerView: View = this.itemView.findViewById(R.id.router_alias_detail_container)
            val macAddress: TextView = this.itemView.findViewById(R.id.router_alias_mac_addr)
            val oui: TextView = this.itemView.findViewById(R.id.router_alias_oui)
            val ouiLoadingSpinner: ProgressBar = this.itemView.findViewById(R.id.router_alias_oui_loading)
            val removeButton: ImageButton = this.itemView.findViewById(R.id.router_alias_remove_btn)
        }

        init {
            this.aliasesColl = FluentIterable.from(Router.getAliases(this.context, this.mRouter)).toList()

            this.mFilter = object : Filter() {
                override fun performFiltering(constraint: CharSequence): FilterResults {
                    val oReturn = FilterResults()
                    val aliases = FluentIterable.from(
                        this@RouterAliasesListRecyclerViewAdapter.mRouter.getAliases(
                            this@RouterAliasesListRecyclerViewAdapter.context
                        )
                    ).toList()

                    if (aliases.isEmpty()) {
                        return oReturn
                    }

                    if (TextUtils.isEmpty(constraint)) {
                        oReturn.values = aliases
                    } else {
                        // Filter aliases list
                        oReturn.values = FluentIterable.from(aliases).filter(
                            Predicate { input ->
                                if (input == null) {
                                    return@Predicate false
                                }
                                val macAddr = input.first
                                val alias = input.second

                                if (macAddr == null || alias == null) {
                                    return@Predicate false
                                }

                                val constraintLowerCase = constraint.toString().toLowerCase()
                                val containsIgnoreCase = macAddr.toLowerCase().contains(
                                    constraintLowerCase
                                ) || alias.toLowerCase().contains(constraintLowerCase)
                                // final boolean containsIgnoreCase =
                                //    containsIgnoreCase(macAddr, constraint) || containsIgnoreCase(alias,
                                //        constraint);

                                if (containsIgnoreCase) {
                                    return@Predicate true
                                }

                                // Otherwise check OUI
                                var macouiVendor: MACOUIVendor? = null
                                try {
                                    macouiVendor = WirelessClientsTile.mMacOuiVendorLookupCache
                                        .getIfPresent(macAddr)
                                } catch (e: Exception) {
                                    // No worries
                                }

                                if (macouiVendor == null) {
                                    return@Predicate false
                                }
                                val company = macouiVendor.company ?: return@Predicate false
                                company.toLowerCase().contains(constraint.toString().toLowerCase())
                                // return (macouiVendor != null && containsIgnoreCase(macouiVendor.getCompany(),
                                //    constraint));
                            }
                        ).toList()
                    }

                    return oReturn
                }

                override fun publishResults(constraint: CharSequence, results: FilterResults) {
                    val values = results.values
                    if (values is List<*>) {

                        setAliasesColl(values as List<Pair<String, String>>)
                        notifyDataSetChanged()
                    }
                }
            }
        }

        override fun getFilter(): Filter {
            return mFilter
        }

        override fun getItemCount(): Int {
            return aliasesColl!!.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position < 0 || position >= aliasesColl!!.size) {
                Utils.reportException(null, IllegalStateException())
                Toast.makeText(context, "Internal Error. Please try again later", Toast.LENGTH_SHORT)
                    .show()
                return
            }
            val aliasPairAt = aliasesColl!![position]
            val mac = aliasPairAt.first
            val aliasStr = aliasPairAt.second
            holder.macAddress.text = mac
            holder.alias.text = aliasStr

            val avatarTextDrawable = ImageUtils.getTextDrawable(aliasStr)
            if (avatarTextDrawable != null) {
                holder.avatarImageView.visibility = View.VISIBLE
                holder.avatarImageView.setImageDrawable(avatarTextDrawable)
            } else {
                holder.avatarImageView.visibility = View.GONE
            }

            // Update OUI in a background thread - as this is likely to infer network call
            MultiThreadingManager.getResolutionTasksExecutor().execute(object : UiRelatedTask<MACOUIVendor>() {

                override fun doWork(): MACOUIVendor? {
                    try {
                        return WirelessClientsTile.mMacOuiVendorLookupCache.get(mac!!)
                    } catch (e: Exception) {
                        // No worries
                        return null
                    }
                }

                override fun thenDoUiRelatedWork(macouiVendor: MACOUIVendor?) {
                    // Hide loading wheel
                    holder.ouiLoadingSpinner.visibility = View.GONE
                    if (macouiVendor != null) {
                        holder.oui.text = macouiVendor.company
                        holder.oui.visibility = View.VISIBLE
                    } else {
                        holder.oui.visibility = View.INVISIBLE
                    }
                }
            })

            val removeAliasEntryDialog = AlertDialog.Builder(context).setIcon(R.drawable.ic_action_alert_warning)
                .setTitle(String.format("Drop Alias for '%s'?", mac))
                .setMessage("Are you sure you wish to continue? ")
                .setCancelable(true)
                .setPositiveButton("Proceed!") { _, _ ->
                    mPreferences.edit().remove(mac).apply()
                    setAliasesColl(FluentIterable.from(mRouter.getAliases(context)).toList())
                    notifyItemRemoved(position)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    // Cancelled - nothing more to do!
                }
                .create()

            holder.removeButton.setOnClickListener { removeAliasEntryDialog.show() }

            holder.containerView.setOnClickListener {
                displayRouterAliasDialog(
                    context!!, mac, aliasStr,
                    DialogInterface.OnClickListener { _, _ ->
                        context.doRefreshRoutersListWithSpinner(RecyclerViewRefreshCause.DATA_SET_CHANGED, null)
                    }
                )
            }

            holder.containerView.setOnLongClickListener { v ->
                showRouterAliasPopupMenu(v, mac, aliasStr, removeAliasEntryDialog)
                true
            }

            val isThemeLight = ColorUtils.isThemeLight(this.context)

            if (!isThemeLight) {
                // Set menu background to white
                holder.aliasMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark)
            }

            holder.aliasMenu.setOnClickListener { v ->
                showRouterAliasPopupMenu(
                    v,
                    mac,
                    aliasStr,
                    removeAliasEntryDialog
                )
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // create a new view
            val v = LayoutInflater.from(parent.context).inflate(R.layout.router_alias, parent, false)
            // set the view's size, margins, paddings and layout parameters
            // ...
            val cardView = v.findViewById<View>(R.id.router_alias_item_cardview) as CardView
            if (ColorUtils.isThemeLight(context)) {
                // Light
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context!!, R.color.cardview_light_background)
                )
            } else {
                // Default is Dark
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context!!, R.color.cardview_dark_background)
                )
            }

            //        return new ViewHolder(this.context,
            //                RippleViewCreator.addRippleToView(v));
            return ViewHolder(v)
        }

        fun setAliasesColl(aliasesColl: List<Pair<String, String>>) {
            this.aliasesColl = aliasesColl
        }

        private fun showRouterAliasPopupMenu(
            v: View,
            mac: String?,
            aliasStr: String?,
            removeAliasEntryDialog: AlertDialog
        ) {
            val popup = PopupMenu(context, v)
            popup.setOnMenuItemClickListener(
                PopupMenu.OnMenuItemClickListener { item ->
                    val i = item.itemId
                    if (i == R.id.menu_router_alias_edit) {
                        displayRouterAliasDialog(
                            context!!, mac, aliasStr,
                            DialogInterface.OnClickListener { _, _ ->
                                context.doRefreshRoutersListWithSpinner(
                                    RecyclerViewRefreshCause.DATA_SET_CHANGED, null
                                )
                            }
                        )
                        return@OnMenuItemClickListener true
                    } else if (i == R.id.menu_router_alias_remove) {
                        removeAliasEntryDialog.show()
                        return@OnMenuItemClickListener true
                    } else {
                    }
                    false
                }
            )
            val inflater = popup.menuInflater
            val menu = popup.menu
            inflater.inflate(R.menu.menu_manage_router_alias, menu)
            popup.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent

        val routerSelected = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED)
        mRouter = if (routerSelected?.isBlank() == false) {
            RouterManagementActivity.getDao(this).getRouter(routerSelected)
        } else {
            null
        }
        if (mRouter == null) {
            Toast.makeText(this, "Missing Router - might have been removed?", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mIsThemeLight = ColorUtils.isThemeLight(this)

        ColorUtils.setAppTheme(this, mRouter!!.routerFirmware, false)

        setContentView(R.layout.activity_manage_router_aliases)

        handleIntent(getIntent())

        mRouterPreferences = getSharedPreferences(routerSelected, Context.MODE_PRIVATE)

        mToolbar = findViewById<View>(R.id.manageRouterAliasesToolbar) as Toolbar
        if (mToolbar != null) {
            mToolbar!!.title = "Manage Aliases"
            mToolbar!!.subtitle = String.format(
                "%s (%s:%d)", mRouter!!.displayName, mRouter!!.remoteIpAddress,
                mRouter!!.remotePort
            )
            mToolbar!!.setTitleTextAppearance(applicationContext, R.style.ToolbarTitle)
            mToolbar!!.setSubtitleTextAppearance(applicationContext, R.style.ToolbarSubtitle)
            mToolbar!!.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
            mToolbar!!.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white))
            setSupportActionBar(mToolbar)
        }

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
        }

        mRecyclerView = findViewById<View>(R.id.routerAliasesListView) as RecyclerViewEmptySupport

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mRecyclerView!!.setHasFixedSize(true)

        // use a linear layout manager
        mLayoutManager = LinearLayoutManager(this)
        mLayoutManager!!.scrollToPosition(0)
        mRecyclerView!!.layoutManager = mLayoutManager

        val emptyView = findViewById<View>(R.id.empty_view) as TextView
        if (mIsThemeLight) {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.black))
        } else {
            emptyView.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
        mRecyclerView!!.setEmptyView(emptyView)

        // specify an adapter (see also next example)
        mAdapter = RouterAliasesListRecyclerViewAdapter(this, mRouter!!)
        mRecyclerView!!.setAdapter(mAdapter)

        //        final RecyclerView.ItemDecoration itemDecoration =
        //                new DividerItemDecoration(this, LinearLayoutManager.VERTICAL);
        //        mRecyclerView.addItemDecoration(itemDecoration);

        addNewButton = findViewById<View>(R.id.router_alias_add) as FloatingActionButton

        addNewButton!!.setOnClickListener(this)

        mSwipeRefreshLayout = findViewById<View>(R.id.swipeRefreshLayout) as SwipeRefreshLayout
        mSwipeRefreshLayout!!.setOnRefreshListener(this)
        mSwipeRefreshLayout!!.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light, android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        mRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {}

            override fun onScrolled(
                recyclerView: RecyclerView,
                firstVisibleItem: Int,
                visibleItemCount: Int
            ) {
                var enable = false
                if (recyclerView.childCount > 0) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                    if (layoutManager != null) {
                        // check if the first item of the list is visible
                        val firstItemVisible = layoutManager.findFirstVisibleItemPosition() == 0

                        // check if the top of the first item is visible
                        val childAt = layoutManager.getChildAt(0)
                        val topOfFirstItemVisible = childAt != null && childAt.top == 0

                        // enabling or disabling the refresh layout
                        enable = firstItemVisible && topOfFirstItemVisible
                    }
                }
                mSwipeRefreshLayout!!.isEnabled = enable
                //                super.onScrolled(recyclerView, dx, dy);
            }
        })
    }

    override fun onClick(view: View?) {
        if (view == null) {
            return
        }
        if (view.id == R.id.router_alias_add) {
            displayRouterAliasDialog(
                this,
                null,
                null,
                DialogInterface.OnClickListener { dialog, which ->
                    doRefreshRoutersListWithSpinner(
                        RecyclerViewRefreshCause.DATA_SET_CHANGED,
                        null
                    )
                }
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.menu_manage_router_aliases, menu)

        this.optionsMenu = menu

        // Search
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        val searchView = menu.findItem(R.id.router_aliases_list_refresh_search).actionView as SearchView

        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        searchView.setOnQueryTextListener(this)

        // Get the search close button image view
        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView?
        closeButton?.setOnClickListener {
            // Reset views
            val adapter = mAdapter as RouterAliasesListRecyclerViewAdapter?
            adapter!!.setAliasesColl(
                FluentIterable.from(Router.getAliases(this@ManageRouterAliasesActivity, mRouter))
                    .toList()
            )
            adapter.notifyDataSetChanged()
            // Hide it now
            searchView.isIconified = true
        }

        return super.onCreateOptionsMenu(menu)
    }

    @Throws(Exception::class)
    override fun onDismissEventTimeout(event: Int, bundle: Bundle?) {

        when (bundle?.getInt(MAIN_ACTIVITY_ACTION) ?: return) {
            RouterActions.EXPORT_ALIASES -> {
                // Load all aliases from preferences
                if (mRouterPreferences == null) {
                    return
                }
                val allRouterPrefs = mRouterPreferences!!.all
                if (allRouterPrefs == null || allRouterPrefs.isEmpty()) {
                    return
                }

                val aliases = mutableMapOf<String, String>()
                for ((key, value) in allRouterPrefs) {
                    if (isNullOrEmpty(key) || value == null) {
                        continue
                    }
                    // Check whether key is a MAC-Address
                    if (!Utils.MAC_ADDRESS.matcher(key).matches()) {
                        continue
                    }
                    // This is a MAC Address - collect it right away!
                    aliases[key] = nullToEmpty(value.toString())
                }

                val exportDirectory = StorageUtils.getExportDirectory(this)
                    ?: throw StorageException("Could not retrieve or create export directory!")

                val aliasesDir = File(exportDirectory, "alias")

                StorageUtils.createDirectoryOrRaiseException(aliasesDir)

                val outputFile = File(
                    aliasesDir,
                    Utils.getEscapedFileName(
                        String.format(
                            "Aliases_for_%s_%s_%s", mRouter!!.displayName,
                            mRouter!!.remoteIpAddress, mRouter!!.uuid
                        )
                    ) + ".json"
                )

                val backupDate = Date()
                val aliasesStr = JSONObject(aliases as Map<*, *>).toString(2)

                var fileOutputStream: FileOutputStream? = null
                try {
                    fileOutputStream = FileOutputStream(outputFile)
                    fileOutputStream.write(aliasesStr.toByteArray())
                } finally {
                    fileOutputStream?.close()
                }

                SnackbarUtils.buildSnackbar(
                    this, findViewById(android.R.id.content), Color.DKGRAY,
                    String.format("File '%s' created!", outputFile.absolutePath), Color.GREEN, "Share",
                    Color.WHITE, Snackbar.LENGTH_LONG,
                    object : SnackbarCallback {
                        @Throws(Exception::class)
                        override fun onDismissEventActionClick(event: Int, bundle: Bundle?) {
                            // Share button clicked - share file
                            try {
                                if (!outputFile.exists()) {
                                    Utils.displayMessage(
                                        this@ManageRouterAliasesActivity,
                                        String.format(
                                            "File '%s' no longer exists",
                                            outputFile.absolutePath
                                        ),
                                        ALERT
                                    )
                                    return
                                }
                                // Now allow user to share file if needed
                                val uriForFile = FileProvider.getUriForFile(
                                    this@ManageRouterAliasesActivity,
                                    RouterCompanionAppConstants.FILEPROVIDER_AUTHORITY, outputFile
                                )
                                this@ManageRouterAliasesActivity.grantUriPermission(
                                    this@ManageRouterAliasesActivity.packageName, uriForFile,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )

                                val shareIntent = Intent()
                                shareIntent.action = Intent.ACTION_SEND
                                shareIntent.putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    String.format(
                                        "Aliases Backup for Router '%s'",
                                        mRouter!!.canonicalHumanReadableName
                                    )
                                )
                                shareIntent.type = "text/html"
                                shareIntent.putExtra(
                                    Intent.EXTRA_TEXT,
                                    fromHtml(
                                        "Backup Date: $backupDate\n\n$aliasesStr\n\n\n"
                                            .replace("\n".toRegex(), "<br/>") + Utils.getShareIntentFooter()
                                    )
                                )
                                shareIntent.putExtra(Intent.EXTRA_STREAM, uriForFile)
                                //                                            shareIntent.setType("*/*");
                                this@ManageRouterAliasesActivity.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        this@ManageRouterAliasesActivity.resources
                                            .getText(R.string.share_backup)
                                    )
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Utils.reportException(this@ManageRouterAliasesActivity, e)
                                // No worries, but notify user
                                Utils.displayMessage(
                                    this@ManageRouterAliasesActivity,
                                    "Internal Error - please try again later or share file manually!",
                                    ALERT
                                )
                            }
                        }
                    },
                    null, true
                )
            }
            else -> {
            }
        } // Ignored
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else if (itemId == R.id.action_feedback) {
            Utils.openFeedbackForm(this, mRouter)
            return true
        } else if (itemId == R.id.router_aliases_add) {
            displayRouterAliasDialog(
                this,
                null,
                null,
                DialogInterface.OnClickListener { dialog, which ->
                    doRefreshRoutersListWithSpinner(
                        RecyclerViewRefreshCause.DATA_SET_CHANGED,
                        null
                    )
                }
            )
            return true
        } else if (itemId == R.id.router_aliases_import) {
            PermissionsUtils.requestPermissions(
                this, listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                {
                    val importAliasesFragment = supportFragmentManager.findFragmentByTag(IMPORT_ALIASES_FRAGMENT_TAG)
                    if (importAliasesFragment is DialogFragment) {
                        importAliasesFragment.dismiss()
                    }
                    val importAliases = ImportAliasesDialogFragment.newInstance(mRouter!!.uuid)
                    importAliases.show(supportFragmentManager, IMPORT_ALIASES_FRAGMENT_TAG)
                    Unit
                },
                { Unit },
                "Storage access required to perform this action!"
            )
            return true
        } else if (itemId == R.id.router_aliases_export_all) {
            PermissionsUtils.requestPermissions(
                this, listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                {
                    val token = Bundle()
                    token.putInt(MAIN_ACTIVITY_ACTION, RouterActions.EXPORT_ALIASES)

                    SnackbarUtils.buildSnackbar(
                        this,
                        String.format(
                            "Going to start exporting aliases for '%s' (%s)...",
                            mRouter!!.displayName, mRouter!!.remoteIpAddress
                        ),
                        "Undo",
                        Snackbar.LENGTH_SHORT, this@ManageRouterAliasesActivity, token, true
                    )
                    Unit
                },
                "Storage access required!",
                "Storage access required!",
                { Unit }
            )
            return true
        } else if (itemId == R.id.router_aliases_clear_all) {
            AlertDialog.Builder(this).setIcon(R.drawable.ic_action_alert_warning)
                .setTitle("Drop all aliases?")
                .setMessage("You'll lose all your local aliases for this router!")
                .setCancelable(true)
                .setPositiveButton("Proceed!") { dialog, which ->
                    val editor = mRouterPreferences!!.edit()
                    // Iterate over all possible aliases
                    val aliases = Router.getAliases(this@ManageRouterAliasesActivity, mRouter)
                    for (alias in aliases) {
                        if (alias == null) {
                            continue
                        }
                        editor.remove(alias.first)
                    }
                    editor.apply()
                    doRefreshRoutersListWithSpinner(RecyclerViewRefreshCause.DATA_SET_CHANGED, null)
                }
                .setNegativeButton("Cancel") { dialogInterface, i ->
                    // Cancelled - nothing more to do!
                }
                .create()
                .show()
            return true
        } else if (itemId == R.id.router_aliases_list_refresh) {
            doRefreshRoutersListWithSpinner(RecyclerViewRefreshCause.DATA_SET_CHANGED, null)
            return true
        } else {
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onQueryTextChange(s: String): Boolean {
        val adapter = mAdapter as RouterAliasesListRecyclerViewAdapter?
        if (TextUtils.isEmpty(s)) {
            adapter!!.setAliasesColl(FluentIterable.from(Router.getAliases(this, mRouter)).toList())
            adapter.notifyDataSetChanged()
        } else {
            adapter!!.filter.filter(s)
        }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onRefresh() {
        doRefreshRoutersListWithSpinner(RecyclerViewRefreshCause.DATA_SET_CHANGED, null)
    }

    @Throws(Exception::class)
    override fun onShowEvent(bundle: Bundle?) {
    }

    fun setRefreshActionButtonState(refreshing: Boolean) {
        mSwipeRefreshLayout!!.isRefreshing = refreshing
        if (optionsMenu != null) {
            val refreshItem = optionsMenu!!.findItem(R.id.router_aliases_list_refresh)
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.actionbar_indeterminate_progress)
                } else {
                    refreshItem.actionView = null
                }
            }
        }
    }

    private fun doRefreshRoutersListWithSpinner(
        cause: RecyclerViewRefreshCause,
        position: Int?
    ) {
        //        mSwipeRefreshLayout.setEnabled(false);
        setRefreshActionButtonState(true)
        Handler().postDelayed(
            {
                try {
                    val allAliases = FluentIterable.from(Router.getAliases(this@ManageRouterAliasesActivity, mRouter))
                        .toList()
                    (this@ManageRouterAliasesActivity.mAdapter as RouterAliasesListRecyclerViewAdapter).setAliasesColl(
                        allAliases
                    )
                    when (cause) {
                        RecyclerViewRefreshCause.DATA_SET_CHANGED -> this@ManageRouterAliasesActivity.mAdapter!!.notifyDataSetChanged()
                        RecyclerViewRefreshCause.INSERTED -> this@ManageRouterAliasesActivity.mAdapter!!.notifyItemInserted(
                            position!!
                        )
                        RecyclerViewRefreshCause.REMOVED -> this@ManageRouterAliasesActivity.mAdapter!!.notifyItemRemoved(
                            position!!
                        )
                        RecyclerViewRefreshCause.UPDATED -> this@ManageRouterAliasesActivity.mAdapter!!.notifyItemChanged(
                            position!!
                        )
                    }
                } finally {
                    setRefreshActionButtonState(false)
                    //                    mSwipeRefreshLayout.setEnabled(true);
                }
            },
            1000
        )
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val adapter = mAdapter as RouterAliasesListRecyclerViewAdapter?
            val query = intent.getStringExtra(SearchManager.QUERY)
            if (query == null) {
                adapter!!.setAliasesColl(FluentIterable.from(Router.getAliases(this, mRouter)).toList())
                adapter.notifyDataSetChanged()
                return
            }
            adapter!!.filter.filter(query)
        }
    }

    companion object {

        private val LOG_TAG = ManageRouterAliasesActivity::class.java.simpleName

        private val ADD_OR_EDIT_ROUTER_ALIAS_FRAGMENT = "ADD_OR_EDIT_ROUTER_ALIAS_FRAGMENT"

        private fun displayRouterAliasDialog(
            activity: ManageRouterAliasesActivity,
            macAddress: String?,
            currentAlias: String?,
            onClickListener: DialogInterface.OnClickListener
        ) {
            val fragment = AddOrUpdateRouterAliasDialogFragment.newInstance(
                activity.mRouter!!, macAddress, currentAlias,
                onClickListener
            )
            fragment.show(activity.supportFragmentManager, ADD_OR_EDIT_ROUTER_ALIAS_FRAGMENT)
        }
    }
}
