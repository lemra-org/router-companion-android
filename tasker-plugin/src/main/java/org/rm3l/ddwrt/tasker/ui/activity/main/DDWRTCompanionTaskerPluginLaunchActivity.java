/*
 * Copyright (c) 2016 Armel Soro
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.rm3l.ddwrt.tasker.ui.activity.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Splitter;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import org.rm3l.ddwrt.common.IDDWRTCompanionService;
import org.rm3l.ddwrt.common.resources.RouterInfo;
import org.rm3l.ddwrt.common.resources.audit.ActionLog;
import org.rm3l.ddwrt.common.utils.ActivityUtils;
import org.rm3l.ddwrt.tasker.BuildConfig;
import org.rm3l.ddwrt.tasker.Constants;
import org.rm3l.ddwrt.tasker.R;
import org.rm3l.ddwrt.tasker.exception.DDWRTCompanionPackageVersionRequiredNotFoundException;
import org.rm3l.ddwrt.tasker.feedback.maoni.FeedbackHandler;
import org.rm3l.ddwrt.tasker.ui.activity.action.ActionEditActivity;
import org.rm3l.ddwrt.tasker.utils.Utils;
import org.rm3l.maoni.Maoni;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.rm3l.ddwrt.tasker.Constants.DDWRT_COMPANION_MIN_VERSION_REQUIRED;

/**
 * EntryPoint Activity
 */
public class DDWRTCompanionTaskerPluginLaunchActivity extends AppCompatActivity {

    /** Service to which this client will bind */
    private IDDWRTCompanionService ddwrtCompanionService;
    /** Connection to the service (inner class) */
    private RouterServiceConnection conn;

    private String ddwrtCompanionAppPackage;

    private static final String TASKER_PKG_NAME = "net.dinglisch.android.taskerm";
    private RecyclerView mActionHistoryRecyclerView;
    private LinearLayoutManager mHistoryLayoutManager;

    private TextView mErrorView;

    private TextView mHistoryEmptyView;
    private TaskerActionHistoryAdapter mHistoryAdapter;
    private View mSlidingUpPanel;
    private View mHistoryDescriptionText;
    private AtomicBoolean mClearHistory = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.ddwrt_companion_tasker_main_activity);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitleTextAppearance(getApplicationContext(), R.style.ToolbarTitle);
            toolbar.setSubtitleTextAppearance(getApplicationContext(), R.style.ToolbarSubtitle);
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.white));
            toolbar.setTitle("DD-WRT Companion");
            toolbar.setSubtitle("Tasker Plugin");
            setSupportActionBar(toolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.mipmap.ic_launcher);
        }

        mErrorView = (TextView) findViewById(R.id.error_placeholder);

        final TextView aboutView = (TextView) findViewById(R.id.tasker_main_about_textview);
        aboutView.setText(getResources().getString(R.string.main_activity_app_description,
                DDWRT_COMPANION_MIN_VERSION_REQUIRED));

        mHistoryDescriptionText = findViewById(R.id.tasker_main_history_textview);

        mActionHistoryRecyclerView = (RecyclerView)
                findViewById(R.id.tasker_main_history_recyclerview);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        // allows for optimizations if all items are of the same size:
        mActionHistoryRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        mHistoryLayoutManager = new LinearLayoutManager(this);
        mHistoryLayoutManager.scrollToPosition(0);
        mActionHistoryRecyclerView.setLayoutManager(mHistoryLayoutManager);
        mHistoryEmptyView = (TextView) findViewById(R.id.tasker_main_history_recyclerview_empty_view);
//        if (themeLight) {
//            statsEmptyView.setTextColor(ContextCompat.getColor(this, R.color.black));
//        } else {
//            statsEmptyView.setTextColor(ContextCompat.getColor(this, R.color.white));
//        }
//        mActionHistoryRecyclerView.setEmptyView(statsEmptyView);
//        // specify an adapter (see also next example)
        mHistoryAdapter = new TaskerActionHistoryAdapter(this);
        mHistoryAdapter.setActionLogs(Collections.<ActionLog> emptyList());
        mActionHistoryRecyclerView.setAdapter(mHistoryAdapter);

        mSlidingUpPanel = findViewById(R.id.tasker_main_history);

        try {
            final PackageInfo packageInfo = Utils
                    .getDDWRTCompanionAppPackageLeastRequiredVersion(getPackageManager());
            if (packageInfo == null ||
                    (this.ddwrtCompanionAppPackage = packageInfo.packageName) == null) {
                mErrorView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityUtils.openPlayStoreForPackage(
                                DDWRTCompanionTaskerPluginLaunchActivity.this, "org.rm3l.ddwrt");
                    }
                });
                mErrorView.setText("DD-WRT Companion app *not* found !");
                mErrorView.setVisibility(View.VISIBLE);
                //Hide history sliding layout
                mSlidingUpPanel.setVisibility(View.GONE);
                mHistoryDescriptionText.setVisibility(View.INVISIBLE);
            } else {
                //Bind service
                mErrorView.setVisibility(View.GONE);
                mSlidingUpPanel.setVisibility(View.VISIBLE);
                mHistoryDescriptionText.setVisibility(View.VISIBLE);

                // connect to the service
                conn = new RouterServiceConnection();

                // name must match the service's Intent filter in the Service Manifest file
                final Intent intent = new Intent(ActionEditActivity.DDWRT_COMPANION_SERVICE_NAME);
                intent.setPackage(this.ddwrtCompanionAppPackage);
                // bind to the Service, create it if it's not already there
                bindService(intent, conn, Context.BIND_AUTO_CREATE);
            }
        } catch (final DDWRTCompanionPackageVersionRequiredNotFoundException e) {
            Crashlytics.logException(e);
            mErrorView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityUtils.openPlayStoreForPackage(
                            DDWRTCompanionTaskerPluginLaunchActivity.this, "org.rm3l.ddwrt");
                }
            });
            mErrorView.setText(e.getMessage());
            mErrorView.setVisibility(View.VISIBLE);
            mSlidingUpPanel.setVisibility(View.GONE);
            mHistoryDescriptionText.setVisibility(View.INVISIBLE);
        }

        final View refreshButton = findViewById(R.id.tasker_main_history_refresh);
        final View clearAllButton = findViewById(R.id.tasker_main_history_clear_all);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mClearHistory.set(false);
                //Reload adapter data
                unbindService(conn);
                ddwrtCompanionService = null;

                // connect to the service
                conn = new RouterServiceConnection();

                // name must match the service's Intent filter in the Service Manifest file
                final Intent intent = new Intent(ActionEditActivity.DDWRT_COMPANION_SERVICE_NAME);
                intent.setPackage(DDWRTCompanionTaskerPluginLaunchActivity.this.ddwrtCompanionAppPackage);
                // bind to the Service, create it if it's not already there
                bindService(intent, conn, Context.BIND_AUTO_CREATE);
            }
        });

        clearAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(DDWRTCompanionTaskerPluginLaunchActivity.this)
                        .setIcon(R.drawable.ic_warning_black_24dp)
                        .setTitle("Clear All?")
                        .setMessage("You'll lose the history of all actions performed by this plugin!")
                        .setCancelable(true)
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, final int i) {
                                mClearHistory.set(true);
                                //Reload adapter data
                                unbindService(conn);
                                ddwrtCompanionService = null;

                                // connect to the service
                                conn = new RouterServiceConnection();

                                // name must match the service's Intent filter in the Service Manifest file
                                final Intent intent = new Intent(ActionEditActivity.DDWRT_COMPANION_SERVICE_NAME);
                                intent.setPackage(DDWRTCompanionTaskerPluginLaunchActivity.this.ddwrtCompanionAppPackage);
                                // bind to the Service, create it if it's not already there
                                bindService(intent, conn, Context.BIND_AUTO_CREATE);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Cancelled - nothing more to do!
                            }
                        }).create().show();
            }
        });


    }

    /** Clean up before Activity is destroyed */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        ddwrtCompanionService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public final boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;

            case R.id.launch_ddwrt_companion:
                final PackageInfo packageInfo = Utils
                        .getDDWRTCompanionAppPackage(getPackageManager());
                final String ddwrtCompanionAppPackage = (packageInfo != null ?
                        packageInfo.packageName : null);
                final boolean packageNameIsEmpty = TextUtils.isEmpty(ddwrtCompanionAppPackage);
                ActivityUtils.launchApp(
                        this,
                        packageNameIsEmpty ? "org.rm3l.ddwrt" : ddwrtCompanionAppPackage,
                        true);
                break;

            case R.id.launch_tasker:
                ActivityUtils.launchApp(this,TASKER_PKG_NAME, true);
                break;

            case R.id.ddwrt_companion_tasker_feedback:
                new Maoni.Builder(Constants.FILEPROVIDER_AUTHORITY)
                        .withTheme(R.style.AppThemeLight_StatusBarTransparent)
                        .withWindowTitle("Send Feedback")
                        .withExtraLayout(R.layout.activity_feedback_maoni)
                        .withHandler(new FeedbackHandler(this))
                        .build()
                        .start(this);
                break;

            case R.id.ddwrt_companion_tasker_about:
                new LibsBuilder()
                        .withFields(R.string.class.getFields())
                        .withActivityTitle("About")
                        //provide a style (optional) (LIGHT, DARK, LIGHT_DARK_TOOLBAR)
                        .withActivityStyle(Libs.ActivityStyle.LIGHT)
                        //start the activity
                        .start(this);
                break;

            case R.id.exit:
                finish();
                break;

            default:
                break;
        }
        return true;
    }

    public static class TaskerActionHistoryAdapter
            extends RecyclerView.Adapter<TaskerActionHistoryAdapter.ViewHolder>{

        private final Activity activity;

        private List<ActionLog> actionLogs;

        private IDDWRTCompanionService service;

        public TaskerActionHistoryAdapter(Activity activity) {
            this.activity = activity;
        }

        public TaskerActionHistoryAdapter setService(IDDWRTCompanionService service) {
            this.service = service;
            return this;
        }

        public TaskerActionHistoryAdapter setActionLogs(List<ActionLog> actionLogs) {
            this.actionLogs = actionLogs;
            notifyDataSetChanged();
            return this;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.action_history_log_cardview, parent, false);
            final CardView cardView = (CardView)
                    v.findViewById(R.id.action_history_log_card_view);
            //Light
            cardView.setCardBackgroundColor(ContextCompat
                    .getColor(activity, R.color.cardview_light_background));

//        return new ViewHolder(this.context,
//                RippleViewCreator.addRippleToView(v));
            return new ViewHolder(this.activity, v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (position < 0 || actionLogs == null || position >= actionLogs.size() ||
                    actionLogs.get(position) == null) {
                Crashlytics.log(Log.WARN, Constants.TAG,
                        "position < 0 || actionLogs == null || position >= actionLogs.size() || " +
                                "actionLogs.get(" + position + ") == null");
                return;
            }
            final ActionLog actionLog = actionLogs.get(position);
            final String actionName = actionLog.getActionName();

//            final Integer drawableResForCommand = getDrawableResForCommand(actionName);
//            holder.actionNameTv.setCompoundDrawables(null,
//                    drawableResForCommand != null ?
//                            ContextCompat.getDrawable(activity, drawableResForCommand) :
//                            null, null, null);

            if (!TextUtils.isEmpty(actionName)) {
                if (actionName.length() < "Execute custom command".length()) {
                    if (actionName.length() < "Wake On LAN".length()) {
                        holder.actionNameTv.setText("\n\n" + actionName);
                    } else {
                        holder.actionNameTv.setText("\n" + actionName);
                    }
                } else {
                    holder.actionNameTv.setText(actionName);
                }
            } else {
                holder.actionNameTv.setText("\n-\n");
            }
            final String actionLogDate = actionLog.getDate();
            if (TextUtils.isEmpty(actionLogDate)) {
                holder.dateTv.setText("-");
            } else {
                try {
                    holder.dateTv.setText(DateFormat.getDateTimeInstance().format(
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                                    .parse(actionLogDate)));
                } catch (final ParseException e) {
                    Crashlytics.logException(e);
                    holder.dateTv.setText("-");
                }
            }

            String routerDisplayName = null;
            if (service != null && service.asBinder().isBinderAlive()) {
                try {
                    final RouterInfo routerInfo = service.getRouterByUuid(actionLog.getRouter());
                    if (routerInfo != null) {
                        routerDisplayName = String.format("%s (%s)",
                                TextUtils.isEmpty(routerInfo.getName()) ? "-" : routerInfo.getName(),
                                routerInfo.isDemoRouter() ? "DEMO" :
                                        String.format("%s:%d",
                                                routerInfo.getRemoteIpAddress(),
                                                routerInfo.getRemotePort()));
                    }
                } catch (RemoteException e) {
                    Crashlytics.logException(e);
                    routerDisplayName = null;
                }
            }
            if (TextUtils.isEmpty(routerDisplayName)) {
                holder.routerTv.setVisibility(View.INVISIBLE);
            } else {
                holder.routerTv.setVisibility(View.VISIBLE);
            }
            holder.routerTv.setText(routerDisplayName);

            final int actionLogStatus = actionLog.getStatus();
            holder.actionStatusIV.setImageDrawable(ContextCompat.getDrawable(activity,
                    actionLogStatus == 0 ? R.drawable.ic_action_action_done : R.drawable.ic_action_alert_error));
            holder.actionStatusIV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(activity, actionLogStatus == 0 ?
                            "Action sent out successfully" : "An error occurred.",
                            Toast.LENGTH_SHORT).show();
                }
            });

        }

        @Override
        public int getItemCount() {
            if (actionLogs != null) {
                return actionLogs.size();
            }
            return 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            private final Context mContext;

//            private final ImageView actionAvatarIV;
            private final ImageView actionStatusIV;
            private final TextView dateTv;
            private final TextView actionNameTv;
            private final TextView routerTv;

            public ViewHolder(Context mContext, View itemView) {
                super(itemView);
                this.mContext = mContext;

//                this.actionAvatarIV = (ImageView) itemView
//                        .findViewById(R.id.action_history_log_card_view_action_image);
                this.actionNameTv = (TextView) itemView
                        .findViewById(R.id.action_history_log_card_view_action);
                this.routerTv = (TextView) itemView
                        .findViewById(R.id.action_history_log_card_view_router);
                this.dateTv = (TextView) itemView
                        .findViewById(R.id.action_history_log_card_view_date);
                this.actionStatusIV = (ImageView) itemView
                        .findViewById(R.id.action_history_log_card_view_action_status_image);
            }
        }

    }

    /** Inner class used to connect to UserDataService */
    class RouterServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Crashlytics.log(Log.DEBUG, Constants.TAG, "Service connected");
            mErrorView.setVisibility(View.GONE);
            ddwrtCompanionService = IDDWRTCompanionService.Stub.asInterface(service);

            mSlidingUpPanel.setVisibility(View.VISIBLE);
            mHistoryDescriptionText.setVisibility(View.VISIBLE);

            mHistoryAdapter.setService(ddwrtCompanionService);
            try {
                if (mClearHistory.getAndSet(false)) {
                    ddwrtCompanionService.clearActionsLogByOrigin(BuildConfig.APPLICATION_ID);
                }
                mHistoryAdapter.setActionLogs(ddwrtCompanionService
                        .getActionsByOrigin(BuildConfig.APPLICATION_ID));
            }  catch (RemoteException e) {
                Crashlytics.logException(e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Crashlytics.log(Log.WARN, Constants.TAG, "Service has unexpectedly disconnected");
            ddwrtCompanionService = null;
            mHistoryAdapter.setService(null);
            mErrorView.setText("Connection to DD-WRT Companion application unexpectedly disconnected. Please reload and try again.");
            mErrorView.setOnClickListener(null);
            mErrorView.setVisibility(View.VISIBLE);
        }
    }

    public static Integer getDrawableResForCommand(@Nullable final String command) {
        if (command == null) {
            return null;
        }
        switch (command) {
            case "Reboot":
                return R.drawable.ic_power_settings_new_black_24dp;
            case "Wake On LAN":
                return R.drawable.ic_settings_power_black_24dp;
            case "Execute custom command":
            case "Execute script from file":
                return R.drawable.ic_action_cli_black;
            //TODO Add other mappings here
            default:
                return null;
        }
    }

}
