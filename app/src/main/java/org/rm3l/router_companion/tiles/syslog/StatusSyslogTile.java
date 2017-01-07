/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
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

package org.rm3l.router_companion.tiles.syslog;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v4.content.PermissionChecker;
import android.support.v7.widget.SwitchCompat;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.crashlytics.android.Crashlytics;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.jcraft.jsch.Session;

import org.rm3l.router_companion.BuildConfig;
import org.rm3l.router_companion.R;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.actions.SetNVRAMVariablesAction;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.DDWRTCompanionConstants;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.keyboardsurfer.android.widget.crouton.Style;

import static android.support.v4.content.ContextCompat.getColor;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.SYSLOG;
import static org.rm3l.router_companion.resources.conn.NVRAMInfo.SYSLOGD_ENABLE;
import static org.rm3l.router_companion.utils.DDWRTCompanionConstants.EMPTY_STRING;
import static org.rm3l.router_companion.utils.Utils.fromHtml;

/**
 *
 */
public class StatusSyslogTile extends DDWRTTile<NVRAMInfo> {


    protected static final String LOG_TAG = StatusSyslogTile.class.getSimpleName();

    public static final String CAT_TMP_VAR_LOG_MESSAGES = "cat /tmp/var/log/messages";

    protected static final Joiner LOGS_JOINER = Joiner.on("\n").useForNull(EMPTY_STRING);
    private static final String FONT_COLOR_MATCHING_HTML = "<font color='#ffff00'>";
    private static final String SLASH_FONT_HTML = "</font>";
    private static final String LAST_SEARCH = "lastSearch";
    public static final String LOGS_TO_VIEW_PREF = "logs_to_view";
    public static final int MAX_LOG_LINES = 15;
    @Nullable
    private final String mGrep;
    private final boolean mDisplayStatus;
    private final ImageButton mTileMenu;
    protected long mLastSync;
    private AtomicBoolean isToggleStateActionRunning = new AtomicBoolean(false);
    private AsyncTaskLoader<NVRAMInfo> mLoader;
    private Session mSshSession;

    private final AtomicReference<String> mLogs = new AtomicReference<>();
    private File mFileToShare;

    public StatusSyslogTile(@NonNull Fragment parentFragment, @Nullable final ViewGroup parentViewGroup,
                            @NonNull Bundle arguments, @Nullable final String tileTitle,
                            final boolean displayStatus, Router router, @Nullable final String grep) {
        super(parentFragment, arguments, router, R.layout.tile_status_router_syslog,
                null);

        this.mGrep = grep;
        this.mDisplayStatus = displayStatus;
        if (!isNullOrEmpty(tileTitle)) {
            ((TextView) layout.findViewById(R.id.tile_status_router_syslog_title))
                    .setText(tileTitle);
        }

        this.parentViewGroup = parentViewGroup;

        // Create Options Menu
        mTileMenu = (ImageButton) layout.findViewById(R.id.tile_status_router_syslog_menu);
        if (!ColorUtils.isThemeLight(mParentFragmentActivity)) {
            //Set menu background to white
            mTileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
        }
        mTileMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popup = new PopupMenu(mParentFragmentActivity, v);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        final int itemId = item.getItemId();

                        final int nbLinesToView;
                        switch (itemId) {

                            case R.id.tile_status_syslog_view_share:
                                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                                    Utils.displayUpgradeMessage(mParentFragmentActivity,
                                            "Share Router Logs");
                                } else {
                                    StatusSyslogTile.this.dumpAndShareLogs();
                                }
                                return true;

                            case R.id.tile_status_syslog_view_last25:
                                nbLinesToView= 25;
                                break;

                            case R.id.tile_status_syslog_view_last50:
                                nbLinesToView= 50;
                                break;

                            case R.id.tile_status_syslog_view_last100:
                                nbLinesToView= 100;
                                break;

                            case R.id.tile_status_syslog_view_all:
                                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                                    Utils.displayUpgradeMessage(mParentFragmentActivity,
                                            "View All Logs");
                                    return true;
                                }
                                nbLinesToView= -1;
                                break;

                            default:
                                return false;
                        }

                        if (nbLinesToView == -1) {
                            final Intent viewSyslogIntent = new Intent(mParentFragmentActivity,
                                    ViewSyslogActivity.class);
                            viewSyslogIntent.putExtra(RouterManagementActivity.ROUTER_SELECTED,
                                    mRouter.getUuid());
                            viewSyslogIntent.putExtra(ViewSyslogActivity.FILTER_TEXT, mGrep);
                            viewSyslogIntent.putExtra(ViewSyslogActivity.WINDOW_TITLE, getTitle());
                            viewSyslogIntent.putExtra(ViewSyslogActivity.FULL_LOGS_RETRIEVAL_COMMAND,
                                    getFullLogsRetrievalCommand());
                            mParentFragmentActivity.startActivity(viewSyslogIntent);
                            return true;
                        }

                        //Save preference
                        if (mParentFragmentPreferences != null) {
                            mParentFragmentPreferences.edit()
                                    .putInt(getFormattedPrefKey(LOGS_TO_VIEW_PREF), nbLinesToView)
                                    .apply();
                        }
                        Utils.displayMessage(mParentFragmentActivity,
                                "Last " + nbLinesToView + " lines will be displayed upon next sync.",
                                Style.CONFIRM);

                        return true;
                    }
                });
                final MenuInflater inflater = popup.getMenuInflater();
                final Menu menu = popup.getMenu();
                inflater.inflate(R.menu.tile_status_syslog_options, menu);

                //Hide 'Share' menu item if we do not have any logs (yet!)
                menu.findItem(R.id.tile_status_syslog_view_share)
                        .setVisible(!TextUtils.isEmpty(mLogs.get()));

                if (mParentFragmentPreferences != null) {
                    final int nbLogsToView = mParentFragmentPreferences
                            .getInt(getFormattedPrefKey(LOGS_TO_VIEW_PREF), 50);
                    final Integer menuItemToCheck;
                    switch (nbLogsToView) {
                        case 25:
                            menuItemToCheck = R.id.tile_status_syslog_view_last25;
                            break;
                        case 50:
                            menuItemToCheck = R.id.tile_status_syslog_view_last50;
                            break;
                        case 100:
                            menuItemToCheck = R.id.tile_status_syslog_view_last100;
                            break;
                        default:
                            menuItemToCheck = null;
                            break;
                    }
                    if (menuItemToCheck != null) {
                        final MenuItem menuItem = menu.findItem(menuItemToCheck);
                        if (menuItem != null) {
                            menuItem.setChecked(true);
                        }
                    }
                }

                popup.show();
            }
        });
    }

    @NonNull
    protected CharSequence getFullLogsRetrievalCommand() {
        return CAT_TMP_VAR_LOG_MESSAGES;
    }

    @SuppressLint("DefaultLocale")
    private void dumpAndShareLogs() {
        final int nbLogsToView = mParentFragmentPreferences
                .getInt(getFormattedPrefKey(LOGS_TO_VIEW_PREF), 50);
        final String logs = mLogs.get();
        if (TextUtils.isEmpty(logs)) {
            Utils.displayMessage(mParentFragmentActivity,
                    "No data to share - please try again later",
                    Style.ALERT);
            return;
        }

        if (PermissionChecker.checkSelfPermission(mParentFragmentActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            // permission was granted, yay! Do the
            // contacts-related task you need to do.
            mFileToShare = new File(mParentFragmentActivity.getCacheDir(),
                    Utils.getEscapedFileName(String.format(
                            "Logs_%d__%s",
                            nbLogsToView,
                            nullToEmpty(mRouter.getUuid()))) + ".txt");

            Exception exception = null;
            OutputStream outputStream = null;
            try {
                outputStream = new BufferedOutputStream(
                        new FileOutputStream(mFileToShare, false));
                //noinspection ConstantConditions
                outputStream.write(logs.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                exception = e;
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (exception != null) {
                Utils.displayMessage(mParentFragmentActivity,
                        "Error while trying to share CPU Info - please try again later",
                        Style.ALERT);
                return;
            }

            final Uri uriForFile = FileProvider
                    .getUriForFile(mParentFragmentActivity,
                            DDWRTCompanionConstants.FILEPROVIDER_AUTHORITY, mFileToShare);
            mParentFragmentActivity
                    .grantUriPermission(
                            mParentFragmentActivity.getComponentName()
                                    .getPackageName(),
                            uriForFile, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            final Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                    String.format("Logs (last %d lines) for Router '%s'",
                            nbLogsToView,
                            mRouter.getCanonicalHumanReadableName()));
            sendIntent.setType("text/html");
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    fromHtml(String.format("%s",
//                            logs,
                            Utils.getShareIntentFooter())
                            .replaceAll("\n", "<br/>")));

            sendIntent.setData(uriForFile);
            sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            mParentFragmentActivity.startActivity(
                    Intent.createChooser(sendIntent,  "Share Router logs"));

        } else {
            //Permission requests
            // Should we show an explanation?
            if (ActivityCompat
                    .shouldShowRequestPermissionRationale(
                            mParentFragmentActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                SnackbarUtils.buildSnackbar(mParentFragmentActivity,
                        "Storage access is required to share logs.",
                        "OK",
                        Snackbar.LENGTH_INDEFINITE,
                        new SnackbarCallback() {
                            @Override
                            public void onShowEvent(@Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {
                                //Request permission
                                ActivityCompat.requestPermissions(mParentFragmentActivity,
                                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        DDWRTCompanionConstants.Permissions.STORAGE);
                            }

                            @Override
                            public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

                            }

                            @Override
                            public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {

                            }
                        },
                        null,
                        true);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(mParentFragmentActivity,
                        new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        DDWRTCompanionConstants.Permissions.STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

//    public boolean canChildScrollUp() {
//        final View syslogContentScrollView = layout
//                .findViewById(R.id.tile_status_router_syslog_content_scrollview);
//        final boolean canScrollVertically = ViewCompat.canScrollVertically(
//                syslogContentScrollView,
//                -1);
//        if (!canScrollVertically) {
//            return canScrollVertically;
//        }
//
//        //TODO ScrollView can scroll vertically,
//        // but detect whether the touch was done outside of the scroll view
//        // (in which case we should return false)
//
//        return canScrollVertically;
//    }

//    @Override
//    public boolean isEmbeddedWithinScrollView() {
////        return false;
//        return BuildConfig.WITH_ADS && super.isEmbeddedWithinScrollView();
//    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_status_router_syslog_hdr;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_status_router_syslog_title;
    }

    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        mLoader = new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {
                try {
                    Crashlytics.log(Log.DEBUG, LOG_TAG, "Init background loader for " + StatusSyslogTile.class + ": routerInfo=" +
                            mRouter + " / nbRunsLoader=" + nbRunsLoader);

                    final int maxLogLines = (mParentFragmentPreferences != null ?
                            mParentFragmentPreferences
                                    .getInt(getFormattedPrefKey(LOGS_TO_VIEW_PREF), 50) :
                            50);

                    @SuppressLint("DefaultLocale")
                    final String cmd = String.format("tail -n %d /tmp/var/log/messages %s",
                            maxLogLines,
                            isNullOrEmpty(mGrep) ? "" : " | grep -i -E \"" + mGrep + "\"");

                    if (Looper.myLooper() == null) {
                        //Check for this - otherwise it yields the following error:
                        // "only one looper may be created per thread")
                        //cf. http://stackoverflow.com/questions/23038682/java-lang-runtimeexception-only-one-looper-may-be-created-per-thread
                        Looper.prepare();
                    }

                    if (mRefreshing.getAndSet(true)) {
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    updateProgressBarViewSeparator(0);

                    mLastSync = System.currentTimeMillis();

                    updateProgressBarViewSeparator(10);

                    final NVRAMInfo nvramInfo = new NVRAMInfo();
//                    if (DDWRTCompanionConstants.TEST_MODE) {
                    if (Utils.isDemoRouter(mRouter)) {
                        final String syslogData = "Space suits go with future at the carnivorous alpha quadrant!\n" +
                                "Cur guttus mori? Ferox, clemens hippotoxotas acceleratrix " +
                                "anhelare de germanus, camerarius bubo. Always purely feel the magical lord.\n" +
                                "Refrigerate roasted lobsters in a cooker with hollandaise sauce for about an hour to enhance their thickness." +
                                "With escargots drink BBQ sauce.Yarr there's nothing like the misty amnesty screaming on the sea.\n" +
                                "Death is a stormy whale.The undead parrot smartly leads the anchor.\n\n\n";
                        nvramInfo.setProperty(SYSLOG, syslogData);
                        final boolean syslogState = new Random().nextBoolean();
                        nvramInfo.setProperty(SYSLOGD_ENABLE, syslogState ? "1" : "0");
                    } else {
                        NVRAMInfo nvramInfoTmp = null;
                        try {
                            nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter, mGlobalPreferences, SYSLOGD_ENABLE);
                        } finally {
                            updateProgressBarViewSeparator(50);
                            if (nvramInfoTmp != null) {
                                nvramInfo.putAll(nvramInfoTmp);
                            }

                            String[] logs = null;
                            try {
                                //Get last log lines
                                logs = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter,
                                        mGlobalPreferences, cmd);
                            } finally {
                                updateProgressBarViewSeparator(70);
                                if (logs != null) {
                                    nvramInfo.setProperty(SYSLOG, LOGS_JOINER.join(logs));
                                }
                            }
                        }
                    }
                    updateProgressBarViewSeparator(90);

                    mLogs.set(nvramInfo.getProperty(SYSLOG));

                    return nvramInfo;

                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }
            }
        };
        return mLoader;
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {
        try {
            //Set tiles
            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_status_router_syslog_header_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_router_syslog_content_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_status_router_syslog_state)
                    .setVisibility(View.VISIBLE);
            layout.findViewById(R.id.tile_status_router_syslog_content)
                    .setVisibility(View.VISIBLE);

            Exception preliminaryCheckException = null;
            if (data == null) {
                //noinspection ThrowableInstanceNeverThrown
                preliminaryCheckException = new DDWRTNoDataException("No Data!");
            } else //noinspection ThrowableResultOfMethodCallIgnored
                if (data.getException() == null) {
                    final String syslogdEnabled = data.getProperty(SYSLOGD_ENABLE);
                    if (syslogdEnabled == null || !Arrays.asList("0", "1").contains(syslogdEnabled)) {
                        //noinspection ThrowableInstanceNeverThrown
                        preliminaryCheckException = new DDWRTSyslogdStateUnknown("Unknown state");
                    }
                }

            final SwitchCompat enableTraffDataButton =
                    (SwitchCompat) this.layout.findViewById(R.id.tile_status_router_syslog_status);
            enableTraffDataButton.setVisibility(View.VISIBLE);

            final boolean makeToogleEnabled = (data != null &&
                    data.getData() != null &&
                    data.getData().containsKey(SYSLOGD_ENABLE));

            if (!isToggleStateActionRunning.get()) {
                if (makeToogleEnabled) {
                    if ("1".equals(data.getProperty(SYSLOGD_ENABLE))) {
                        //Enabled
                        enableTraffDataButton.setChecked(true);
                    } else {
                        //Disabled
                        enableTraffDataButton.setChecked(false);
                    }
                    enableTraffDataButton.setEnabled(true);
                } else {
                    enableTraffDataButton.setChecked(false);
                    enableTraffDataButton.setEnabled(false);
                }

                enableTraffDataButton.setOnClickListener(new ManageSyslogdToggle());
            }

            if (preliminaryCheckException != null) {
                data = new NVRAMInfo().setException(preliminaryCheckException);
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_router_syslog_error);

            final Exception exception = data.getException();

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                final String syslogdEnabledPropertyValue = data.getProperty(SYSLOGD_ENABLE);
                final boolean isSyslogEnabled = "1".equals(syslogdEnabledPropertyValue);

                final TextView syslogState = (TextView) this.layout.findViewById(R.id.tile_status_router_syslog_state);

                final View syslogContentView = this.layout.findViewById(R.id.tile_status_router_syslog_content);
                final EditText filterEditText = (EditText) this.layout.findViewById(R.id.tile_status_router_syslog_filter);

                syslogState.setText(syslogdEnabledPropertyValue == null ?
                        "-" : (isSyslogEnabled ? "Enabled" : "Disabled"));

                layout.findViewById(R.id.tile_status_router_syslog_gridlayout)
                        .setVisibility(syslogdEnabledPropertyValue == null ?
                                View.VISIBLE : View.GONE);

//                mTileMenu.setEnabled(syslogdEnabledPropertyValue != null);

                final TextView logTextView = (TextView) syslogContentView;
                if (isSyslogEnabled) {
                    logTextView.setTypeface(Typeface.MONOSPACE);
                    logTextView.setTextColor(getColor(mParentFragmentActivity, R.color.white));

                    //Highlight textToFind for new log lines
                    final String newSyslog = data.getProperty(SYSLOG, EMPTY_STRING);

                    //noinspection ConstantConditions
                    Spanned newSyslogSpan = new SpannableString(newSyslog);

                    final SharedPreferences sharedPreferences = this.mParentFragmentPreferences;
                    final String existingSearch = sharedPreferences != null ?
                            sharedPreferences.getString(getFormattedPrefKey(LAST_SEARCH), null) : null;

                    if (!isNullOrEmpty(existingSearch)) {
                        if (isNullOrEmpty(filterEditText.getText().toString())) {
                            filterEditText.setText(existingSearch);
                        }
                        if (!isNullOrEmpty(newSyslog)) {
                            //noinspection ConstantConditions
                            newSyslogSpan = findAndHighlightOutput(newSyslog, existingSearch);
                        }
                    }

//                if (!(isNullOrEmpty(existingSearch) || isNullOrEmpty(newSyslog))) {
//                    filterEditText.setText(existingSearch);
//                    //noinspection ConstantConditions
//                    newSyslogSpan = findAndHighlightOutput(newSyslog, existingSearch);
//                }

//                    if (!isNullOrEmpty(logTextView.getText().toString()) && !isNullOrEmpty(newSyslog)) {
////                        logTextView.setMovementMethod(new ScrollingMovementMethod());
//
//                        logTextView.setText(new SpannableStringBuilder()
//                                .append(fromHtml("<br/>"))
//                                .append(newSyslogSpan));
//                    }

                    logTextView.setText(new SpannableStringBuilder()
                            .append(fromHtml("<br/>"))
                            .append(newSyslogSpan));

                    filterEditText.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            final int DRAWABLE_LEFT = 0;
                            final int DRAWABLE_TOP = 1;
                            final int DRAWABLE_RIGHT = 2;
                            final int DRAWABLE_BOTTOM = 3;

                            if (event.getAction() == MotionEvent.ACTION_UP) {
                                if (event.getRawX() >= (filterEditText.getRight() - filterEditText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                                    //Reset everything
                                    filterEditText.setText(EMPTY_STRING); //this will trigger the TextWatcher, thus disabling the "Find" button
                                    //Highlight text in textview
                                    final String currentText = logTextView.getText().toString();

                                    logTextView.setText(currentText
                                            .replaceAll(SLASH_FONT_HTML, EMPTY_STRING)
                                            .replaceAll(FONT_COLOR_MATCHING_HTML, EMPTY_STRING));

                                    if (sharedPreferences != null) {
                                        final SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putString(getFormattedPrefKey(LAST_SEARCH), EMPTY_STRING);
                                        editor.apply();
                                    }
                                    return true;
                                }
                            }
                            return false;
                        }
                    });

                    filterEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                                final String textToFind = filterEditText.getText().toString();
                                if (isNullOrEmpty(textToFind)) {
                                    //extra-check, even though we can be pretty sure the button is enabled only if textToFind is present
                                    return true;
                                }
                                if (sharedPreferences != null) {
                                    if (textToFind.equalsIgnoreCase(existingSearch)) {
                                        //No need to go further as this is already the string we are looking for
                                        return true;
                                    }
                                    final SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString(getFormattedPrefKey(LAST_SEARCH), textToFind);
                                    editor.apply();
                                }
                                //Highlight text in textview
                                final String currentText = logTextView.getText().toString();

                                logTextView.setText(findAndHighlightOutput(currentText
                                        .replaceAll(SLASH_FONT_HTML, EMPTY_STRING)
                                        .replaceAll(FONT_COLOR_MATCHING_HTML, EMPTY_STRING), textToFind));

                                return true;
                            }
                            return false;
                        }
                    });
                }

                //Update last sync
                final RelativeTimeTextView lastSyncView = (RelativeTimeTextView) layout.findViewById(R.id.tile_last_sync);
//                lastSyncView.setTextColor(getColor(mParentFragmentActivity, R.color.DarkGray));
                lastSyncView.setReferenceTime(mLastSync);
                lastSyncView.setPrefix("Last sync: ");
            }

            if (exception != null && !(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {
                //noinspection ThrowableResultOfMethodCallIgnored
                final Throwable rootCause = Throwables.getRootCause(exception);
                errorPlaceHolderView.setText("Error: " + (rootCause != null ? rootCause.getMessage() : "null"));
                final Context parentContext = this.mParentFragmentActivity;
                errorPlaceHolderView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        if (rootCause != null) {
                            Toast.makeText(parentContext,
                                    rootCause.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
                errorPlaceHolderView.setVisibility(View.VISIBLE);
                updateProgressBarWithError();
            } else if (exception == null){
                updateProgressBarWithSuccess();
            }

            Crashlytics.log(Log.DEBUG, LOG_TAG, "onLoadFinished(): done loading!");
        } finally {
            mRefreshing.set(false);
            doneWithLoaderInstance(this, loader);
        }
    }

    @NonNull
    public static Spanned findAndHighlightOutput(@NonNull final CharSequence text, @NonNull final String textToFind) {
        final Matcher matcher = Pattern.compile("(" + Pattern.quote(textToFind) + ")", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        return fromHtml(matcher.replaceAll(Matcher.quoteReplacement(FONT_COLOR_MATCHING_HTML) + "$1" + Matcher.quoteReplacement(SLASH_FONT_HTML))
                .replaceAll(Pattern.quote("\n"), Matcher.quoteReplacement("<br/>")));
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        //TODO
        return null;
    }

    private class DDWRTSyslogdStateUnknown extends DDWRTNoDataException {

        public DDWRTSyslogdStateUnknown(@Nullable String detailMessage) {
            super(detailMessage);
        }
    }

    protected String getTitle() {
        return "Syslog";
    }

    private class ManageSyslogdToggle implements View.OnClickListener {

        private boolean enable;

        @Override
        public void onClick(View view) {

            isToggleStateActionRunning.set(true);

            if (!(view instanceof CompoundButton)) {
                Utils.reportException(null, new IllegalStateException("ManageSyslogdToggle#onClick: " +
                        "view is NOT an instance of CompoundButton!"));
                isToggleStateActionRunning.set(false);
                return;
            }

            final CompoundButton compoundButton = (CompoundButton) view;

            mParentFragmentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    compoundButton.setEnabled(false);
                }
            });

            this.enable = compoundButton.isChecked();

//            if (BuildConfig.WITH_ADS) {
//                Utils.displayUpgradeMessage(mParentFragmentActivity, "Toggle Syslog");
//                isToggleStateActionRunning.set(false);
//                mParentFragmentActivity.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        compoundButton.setChecked(!enable);
//                        compoundButton.setEnabled(true);
//                    }
//                });
//                return;
//            }

            final NVRAMInfo nvramInfoToSet = new NVRAMInfo();

            nvramInfoToSet.setProperty(SYSLOGD_ENABLE, enable ? "1" : "0");

            final String title = getTitle();

            new UndoBarController.UndoBar(mParentFragmentActivity)
                    .message(String.format("%s will be %s on '%s' (%s). Router will be rebooted. ",
                            title,
                            enable ? "enabled" : "disabled",
                            mRouter.getDisplayName(),
                            mRouter.getRemoteIpAddress()))
                    .listener(new UndoBarController.AdvancedUndoListener() {
                                  @Override
                                  public void onHide(@Nullable Parcelable parcelable) {

                                      Utils.displayMessage(mParentFragmentActivity,
                                              String.format("%s %s...",
                                                      enable ? "Enabling" : "Disabling",
                                                      title),
                                              Style.INFO);

                                      ActionManager.runTasks(
                                        new SetNVRAMVariablesAction(
                                              mRouter,
                                              mParentFragmentActivity,
                                              nvramInfoToSet,
                                              true,
                                              new RouterActionListener() {
                                                  @Override
                                                  public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull final Router router, Object returnData) {
                                                      mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                          @Override
                                                          public void run() {

                                                              try {
                                                                  compoundButton.setChecked(enable);
                                                                  Utils.displayMessage(mParentFragmentActivity,
                                                                          String.format("%s %s successfully on host '%s' (%s). ",
                                                                                  title,
                                                                                  enable ? "enabled" : "disabled",
                                                                                  router.getDisplayName(),
                                                                                  router.getRemoteIpAddress()),
                                                                          Style.CONFIRM);
                                                              } finally {
                                                                  compoundButton.setEnabled(true);
                                                                  isToggleStateActionRunning.set(false);
                                                                  if (mLoader != null) {
                                                                      //Reload everything right away
                                                                      doneWithLoaderInstance(StatusSyslogTile.this,
                                                                              mLoader,
                                                                              1l);
                                                                  }
                                                              }
                                                          }

                                                      });
                                                  }

                                                  @Override
                                                  public void onRouterActionFailure(@NonNull RouterAction
                                                                                            routerAction, @NonNull final Router
                                                                                            router, @Nullable final Exception exception) {
                                                      mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                          @Override
                                                          public void run() {
                                                              try {
                                                                  compoundButton.setChecked(!enable);
                                                                  Utils.displayMessage(mParentFragmentActivity,
                                                                          String.format("Error while trying to %s %s on '%s' (%s): %s",
                                                                                  enable ? "enable" : "disable",
                                                                                  title,
                                                                                  router.getDisplayName(),
                                                                                  router.getRemoteIpAddress(),
                                                                                  Utils.handleException(exception).first),
                                                                          Style.ALERT);
                                                              } finally {
                                                                  compoundButton.setEnabled(true);
                                                                  isToggleStateActionRunning.set(false);
                                                              }
                                                          }
                                                      });


                                                  }
                                              }

                                              ,
                                              mGlobalPreferences)
                                      );

                                  }

                                  @Override
                                  public void onClear(@NonNull Parcelable[] parcelables) {
                                      mParentFragmentActivity.runOnUiThread(new Runnable() {
                                          @Override
                                          public void run() {
                                              try {
                                                  compoundButton.setChecked(!enable);
                                                  compoundButton.setEnabled(true);
                                              } finally {
                                                  isToggleStateActionRunning.set(false);
                                              }
                                          }
                                      });
                                  }

                                  @Override
                                  public void onUndo(@Nullable Parcelable parcelable) {
                                      mParentFragmentActivity.runOnUiThread(new Runnable() {
                                          @Override
                                          public void run() {
                                              try {
                                                  compoundButton.setChecked(!enable);
                                                  compoundButton.setEnabled(true);
                                              } finally {
                                                  isToggleStateActionRunning.set(false);
                                              }
                                          }
                                      });
                                  }
                              }

                    )
                    .

                            token(new Bundle()

                            )
                    .

                            show();
        }
    }
}
