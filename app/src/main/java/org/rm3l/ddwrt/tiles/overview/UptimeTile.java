package org.rm3l.ddwrt.tiles.overview;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.ColorUtils;
import org.rm3l.ddwrt.utils.SSHUtils;

import java.util.List;
import java.util.Random;

import static org.rm3l.ddwrt.utils.Utils.isDemoRouter;

public class UptimeTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = UptimeTile.class.getSimpleName();
    public static final String UPTIME_DAYS = "UPTIME_DAYS";
    public static final String UPTIME_HOURS = "UPTIME_HOURS";
    public static final String UPTIME_MINUTES = "UPTIME_MINUTES";
    public static final String N_A = "-";
    public static final Splitter COMMA_SPLITTER = Splitter.on(",").omitEmptyStrings();
    public static final String UPTIME = "UPTIME";
    private boolean isThemeLight;
//    private final View.OnClickListener routerStateClickListener;

    public UptimeTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_overview_uptime,
                R.id.tile_overview_uptime_togglebutton);
        isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

//        routerStateClickListener = new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //Open Router State tab
//                if (mParentFragmentActivity instanceof DDWRTMainActivity) {
//                    ((DDWRTMainActivity) mParentFragmentActivity)
//                            .selectItemInDrawer(2);
//                } else {
//                    //TODO Set proper flags ???
//                    final Intent intent = new Intent(mParentFragmentActivity, DDWRTMainActivity.class);
//                    intent.putExtra(ROUTER_SELECTED, mRouter.getUuid());
//                    intent.putExtra(DDWRTMainActivity.SAVE_ITEM_SELECTED, 2);
//                    mParentFragmentActivity.startActivity(intent);
//                }
//            }
//        };
    }

    @Override
    public int getTileHeaderViewId() {
        return 0;
    }

    @Override
    public int getTileTitleViewId() {
        return R.id.tile_overview_uptime_title;
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                try {

                    isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

                    Log.d(LOG_TAG, "Init background loader for " + UptimeTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    final NVRAMInfo nvramInfo = new NVRAMInfo();

                    NVRAMInfo nvramInfoTmp = null;
                    try {
                        if (isDemoRouter(mRouter)) {
                            final Random random = new Random();
                            final String days = Integer.toString(random.nextInt(364));
                            final String hours = Integer.toString(random.nextInt(60));
                            final String minutes = Integer.toString(random.nextInt(60));
                            nvramInfoTmp = new NVRAMInfo()
                                    .setProperty(UPTIME, "22:31:45 up " + days + " days, " + hours +
                                            ":" + minutes + ", load average: 0.11, 0.10, 0.09")
                                    .setProperty(UPTIME_DAYS, days)
                                    .setProperty(UPTIME_HOURS, hours)
                                    .setProperty(UPTIME_MINUTES, minutes);
                        } else {
                            nvramInfoTmp = new NVRAMInfo();

//                            final String[] manualProperty = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences, "uptime 2>/dev/null");
//                            if (manualProperty != null && manualProperty.length > 0 && manualProperty[0] != null) {
//                                nvramInfoTmp = new NVRAMInfo()
//                                        .setProperty(UPTIME, manualProperty[0].trim());
//                            }

                            final String[] uptimeOutput = SSHUtils.getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                    "uptime 2>/dev/null | awk -F'up ' '{print $2}' | awk -F'users' '{print $1}' | awk -F'load' '{print $1}'");
                            if (uptimeOutput != null && uptimeOutput.length > 0) {
                                final String uptime = uptimeOutput[uptimeOutput.length - 1];
                                final List<String> uptimeList = COMMA_SPLITTER.splitToList(uptime);
                                if (uptimeList != null) {
                                    final int uptimeListSize = uptimeList.size();
                                    if (uptimeListSize > 0) {
                                        final String first = uptimeList.get(0).trim();
                                        if (StringUtils.contains(first, "day")) {
                                            //day
                                            nvramInfoTmp.setProperty(UPTIME_DAYS,
                                                    first
                                                            .replaceAll("days", "")
                                                            .replaceAll("day", "")
                                                    .trim());

                                            if (uptimeListSize >= 2) {
                                                final String other = uptimeList.get(1);
                                                if (other != null) {
                                                    if (other.contains(":")) {
                                                        final List<String> otherList =
                                                                Splitter.on(":").omitEmptyStrings()
                                                                .splitToList(other);
                                                        if (otherList != null) {
                                                            if (otherList.size() >= 1) {
                                                                nvramInfoTmp.setProperty(UPTIME_HOURS,
                                                                        otherList.get(0).trim());
                                                            }
                                                            if (otherList.size() >= 2) {
                                                                nvramInfoTmp.setProperty(UPTIME_MINUTES,
                                                                        otherList.get(1).trim());
                                                            }
                                                        }
                                                    } else if (StringUtils.contains(other, "hour")) {
                                                        nvramInfoTmp.setProperty(UPTIME_HOURS,
                                                                other
                                                                        .replaceAll("hours", "")
                                                                        .replaceAll("hour", "").trim());
                                                    } else if (StringUtils.contains(other, "min")) {
                                                        nvramInfoTmp.setProperty(UPTIME_MINUTES,
                                                                other
                                                                        .replaceAll("mins", "")
                                                                        .replaceAll("min", "").trim());
                                                    }
                                                }
                                            }
                                        } else if (StringUtils.contains(first, ":")) {
                                            final List<String> otherList =
                                                    Splitter.on(":").omitEmptyStrings()
                                                            .splitToList(first);
                                            if (otherList != null) {
                                                if (otherList.size() >= 1) {
                                                    nvramInfoTmp.setProperty(UPTIME_HOURS,
                                                            otherList.get(0).trim());
                                                }
                                                if (otherList.size() >= 2) {
                                                    nvramInfoTmp.setProperty(UPTIME_MINUTES,
                                                            otherList.get(1).trim());
                                                }
                                            }
                                        } else if (StringUtils.contains(first, "hour")) {
                                            nvramInfoTmp.setProperty(UPTIME_HOURS,
                                                    first
                                                            .replaceAll("hours", "")
                                                            .replaceAll("hour", "").trim());
                                        } else if (StringUtils.contains(first, "min")) {
                                            nvramInfoTmp.setProperty(UPTIME_MINUTES,
                                                    first
                                                            .trim()
                                                            .replaceAll("mins", "")
                                                            .replaceAll("min", "").trim());
                                        }
                                    }
                                }
                            }

                        }
                    } finally {
                        if (nvramInfoTmp != null) {
                            nvramInfo.putAll(nvramInfoTmp);
                        }

                    }

                    return nvramInfo;

                } catch (@NonNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }
            }
        };
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        
        try {
            //Set tiles
            Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

            layout.findViewById(R.id.tile_overview_uptime_loading_view)
                    .setVisibility(View.GONE);
            layout.findViewById(R.id.tile_overview_uptime_gridLayout)
                    .setVisibility(View.VISIBLE);

            if (data == null) {
                data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
            }

            final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_overview_uptime_error);

            final Exception exception = data.getException();

            final RelativeLayout mapContainerView =
                    (RelativeLayout) layout.findViewById(R.id.tile_overview_uptime_container);

            if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

                if (exception == null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }

                //Days
                String property = data.getProperty(UPTIME_DAYS);
                try {
                    final int value = Integer.parseInt(property);
                    ((TextView) layout.findViewById(R.id.tile_overview_uptime_days_title))
                            .setText("day" + (value >= 2 ? "s" : ""));
                    property = String.valueOf(value);
                } catch (final NumberFormatException e) {
                    e.printStackTrace();
                    property = N_A;
                    ((TextView) layout.findViewById(R.id.tile_overview_uptime_days_title))
                            .setText("day(s)");
                }
                ((TextView) layout.findViewById(R.id.tile_overview_uptime_days))
                        .setText(property);

                //Hours
                property = data.getProperty(UPTIME_HOURS);
                try {
                    final int value = Integer.parseInt(property);
                    ((TextView) layout.findViewById(R.id.tile_overview_uptime_hours_title))
                            .setText("hour" + (value >= 2 ? "s" : ""));
                    property = String.valueOf(value);
                } catch (final NumberFormatException e) {
                    e.printStackTrace();
                    property = N_A;
                    ((TextView) layout.findViewById(R.id.tile_overview_uptime_hours_title))
                            .setText("hour(s)");
                }
                ((TextView) layout.findViewById(R.id.tile_overview_uptime_hours))
                        .setText(property);

                //Minutes
                property = data.getProperty(UPTIME_MINUTES);
                try {
                    final int value = Integer.parseInt(property);
                    ((TextView) layout.findViewById(R.id.tile_overview_uptime_minutes_title))
                            .setText("minute" + (value >= 2 ? "s" : ""));
                    property = String.valueOf(value);
                } catch (final NumberFormatException e) {
                    e.printStackTrace();
                    property = N_A;
                    ((TextView) layout.findViewById(R.id.tile_overview_uptime_minutes_title))
                            .setText("minute(s)");
                }
                ((TextView) layout.findViewById(R.id.tile_overview_uptime_minutes))
                        .setText(property);

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
                //Hide NTM
                mapContainerView.setVisibility(View.GONE);
            } else {
                mapContainerView.setVisibility(View.VISIBLE);
            }

//            mapContainerView.setOnClickListener(routerStateClickListener);

        }  finally {
            Log.d(LOG_TAG, "onLoadFinished(): done loading!");
            doneWithLoaderInstance(this, loader,
                    R.id.tile_overview_uptime_togglebutton_title,
                    R.id.tile_overview_uptime_togglebutton_separator);
        }
    }
}
