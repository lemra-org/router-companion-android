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

package org.rm3l.ddwrt.tiles.syslog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.actions.SetNVRAMVariablesAction;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.SYSLOG;
import static org.rm3l.ddwrt.resources.conn.NVRAMInfo.SYSLOGD_ENABLE;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_STRING;

/**
 *
 */
public class StatusSyslogTile extends DDWRTTile<NVRAMInfo> {


    protected static final String LOG_TAG = StatusSyslogTile.class.getSimpleName();
    protected static final Joiner LOGS_JOINER = Joiner.on("\n").useForNull(EMPTY_STRING);
    protected static final int MAX_LOG_LINES = 15;
    private static final String FONT_COLOR_MATCHING_HTML = "<font color='#009900'>";
    private static final String SLASH_FONT_HTML = "</font>";
    private static final String LAST_SEARCH = "lastSearch";
    @Nullable
    private final String mGrep;
    private final boolean mDisplayStatus;
    protected long mLastSync;
    private AtomicBoolean isToggleStateActionRunning = new AtomicBoolean(false);
    private AsyncTaskLoader<NVRAMInfo> mLoader;

    public StatusSyslogTile(@NonNull Fragment parentFragment, @Nullable final ViewGroup parentViewGroup,
                            @NonNull Bundle arguments, @Nullable final String tileTitle,
                            final boolean displayStatus, Router router, @Nullable final String grep) {
        super(parentFragment, arguments, router, R.layout.tile_status_router_syslog, R.id.tile_status_router_syslog_togglebutton);
        this.mGrep = grep;
        this.mDisplayStatus = displayStatus;
        if (!isNullOrEmpty(tileTitle)) {
            ((TextView) layout.findViewById(R.id.tile_status_router_syslog_title))
                    .setText(tileTitle);
        }

        this.parentViewGroup = parentViewGroup;

    }

    @Override
    public boolean isEmbeddedWithinScrollView() {
//        return false;
        return BuildConfig.WITH_ADS && super.isEmbeddedWithinScrollView();
    }

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
                    Log.d(LOG_TAG, "Init background loader for " + StatusSyslogTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    mLastSync = System.currentTimeMillis();

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
                        nvramInfo.setProperty(SYSLOGD_ENABLE, String.valueOf(new Random().nextInt()));
                    } else {
                        NVRAMInfo nvramInfoTmp = null;
                        try {
                            nvramInfoTmp = SSHUtils.getNVRamInfoFromRouter(mParentFragmentActivity, mRouter, mGlobalPreferences, SYSLOGD_ENABLE);
                        } finally {
                            if (nvramInfoTmp != null) {
                                nvramInfo.putAll(nvramInfoTmp);
                            }

                            String[] logs = null;
                            try {
                                //Get last log lines
                                logs = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter,
                                        mGlobalPreferences, String.format("tail -n %d /tmp/var/log/messages %s",
                                                MAX_LOG_LINES, isNullOrEmpty(mGrep) ? "" : " | grep -i -E \"" + mGrep + "\""));
                            } finally {
                                if (logs != null) {
                                    nvramInfo.setProperty(SYSLOG, LOGS_JOINER.join(logs));
                                }
                            }
                        }
                    }

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
        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

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

            syslogState.setText(syslogdEnabledPropertyValue == null ? "-" : (isSyslogEnabled ? "Enabled" : "Disabled"));

            syslogState.setVisibility(mDisplayStatus ? View.VISIBLE : View.GONE);

            final TextView logTextView = (TextView) syslogContentView;
            if (isSyslogEnabled) {

                //Highlight textToFind for new log lines
                final String newSyslog = data.getProperty(SYSLOG, EMPTY_STRING);

                //Hide container if no data at all (no existing data, and incoming data is empty too)
                final View scrollView = layout.findViewById(R.id.tile_status_router_syslog_content_scrollview);

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

                if (isNullOrEmpty(logTextView.getText().toString()) && isNullOrEmpty(newSyslog)) {
                    scrollView.setVisibility(View.INVISIBLE);
                } else {
                    scrollView.setVisibility(View.VISIBLE);

                    logTextView.setMovementMethod(new ScrollingMovementMethod());

                    logTextView.append(new SpannableStringBuilder()
                            .append(Html.fromHtml("<br/>"))
                            .append(newSyslogSpan));
                }

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
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_router_syslog_togglebutton_title, R.id.tile_status_router_syslog_togglebutton);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    @NonNull
    private Spanned findAndHighlightOutput(@NonNull final CharSequence text, @NonNull final String textToFind) {
        final Matcher matcher = Pattern.compile("(" + Pattern.quote(textToFind) + ")", Pattern.CASE_INSENSITIVE)
                .matcher(text);
        return Html.fromHtml(matcher.replaceAll(Matcher.quoteReplacement(FONT_COLOR_MATCHING_HTML) + "$1" + Matcher.quoteReplacement(SLASH_FONT_HTML))
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

    private class ManageSyslogdToggle implements View.OnClickListener {

        private boolean enable;

        @Override
        public void onClick(View view) {

            isToggleStateActionRunning.set(true);

            if (!(view instanceof CompoundButton)) {
                Utils.reportException(new IllegalStateException("ManageSyslogdToggle#onClick: " +
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

            new UndoBarController.UndoBar(mParentFragmentActivity)
                    .message(String.format("Syslog will be %s on '%s' (%s). Router will be rebooted. ",
                            enable ? "enabled" : "disabled",
                            mRouter.getDisplayName(),
                            mRouter.getRemoteIpAddress()))
                    .listener(new UndoBarController.AdvancedUndoListener() {
                                  @Override
                                  public void onHide(@Nullable Parcelable parcelable) {

                                      Utils.displayMessage(mParentFragmentActivity,
                                              String.format("%s Syslog...",
                                                      enable ? "Enabling" : "Disabling"),
                                              Style.INFO);

                                      new SetNVRAMVariablesAction(mParentFragmentActivity,
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
                                                                          String.format("Syslog %s successfully on host '%s' (%s). ",
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
                                                                              1l,
                                                                              R.id.tile_status_router_syslog_togglebutton_title,
                                                                              R.id.tile_status_router_syslog_togglebutton_separator);
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
                                                                          String.format("Error while trying to %s Syslog on '%s' (%s): %s",
                                                                                  enable ? "enable" : "disable",
                                                                                  router.getDisplayName(),
                                                                                  router.getRemoteIpAddress(),
                                                                                  ExceptionUtils.getRootCauseMessage(exception)),
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
                                              mGlobalPreferences).

                                              execute(mRouter);

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
