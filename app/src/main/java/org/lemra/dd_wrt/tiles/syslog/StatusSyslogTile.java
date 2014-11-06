package org.lemra.dd_wrt.tiles.syslog;

import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.common.base.Throwables;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.NVRAMInfo;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.exceptions.DDWRTNoDataException;
import org.lemra.dd_wrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.lemra.dd_wrt.tiles.DDWRTTile;

import static org.lemra.dd_wrt.api.conn.NVRAMInfo.SYSLOG;

/**
 * Created by armel on 9/9/14.
 */
public class StatusSyslogTile extends DDWRTTile<NVRAMInfo> {

    private static final String LOG_TAG = StatusSyslogTile.class.getSimpleName();

    public StatusSyslogTile(@NotNull SherlockFragmentActivity parentFragmentActivity, @NotNull Bundle arguments, Router router) {
        super(parentFragmentActivity, arguments, router, R.layout.tile_status_router_syslog, R.id.tile_status_router_syslog_togglebutton);
    }

    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

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

                    //TODO Test only
                    final String syslogData = "Space suits go with future at the carnivorous alpha quadrant!\n" +
                            "Cur guttus mori? Ferox, clemens hippotoxotas acceleratrix " +
                            "anhelare de germanus, camerarius bubo. Always purely feel the magical lord.\n" +
                            "Refrigerate roasted lobsters in a cooker with hollandaise sauce for about an hour to enhance their thickness." +
                            "With escargots drink BBQ sauce.Yarr there's nothing like the misty amnesty screaming on the sea.\n" +
                            "Death is a stormy whale.The undead parrot smartly leads the anchor.\n\n\n";
                    final NVRAMInfo nvramInfo = new NVRAMInfo();
                    nvramInfo.setProperty(SYSLOG, syslogData);

                    //END TESTS
                    return nvramInfo;

                } catch (@NotNull final Exception e) {
                    e.printStackTrace();
                    return new NVRAMInfo().setException(e);
                }
            }
        };
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo data) {
        //Set tiles
        Log.d(LOG_TAG, "onLoadFinished: loader=" + loader + " / data=" + data);

        if (data == null) {
            data = new NVRAMInfo().setException(new DDWRTNoDataException("No Data!"));
        }

        @NotNull final TextView errorPlaceHolderView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_error);

        @Nullable final Exception exception = data.getException();

        if (!(exception instanceof DDWRTTileAutoRefreshNotAllowedException)) {

            if (exception == null) {
                if (errorPlaceHolderView != null) {
                    errorPlaceHolderView.setVisibility(View.GONE);
                }
            }

            ((TextView) this.layout.findViewById(R.id.tile_status_router_syslog_content))
                    .setText(data.getProperty(SYSLOG, "N/A"));

        }

        if (exception != null) {
            if (errorPlaceHolderView != null) {
                errorPlaceHolderView.setText(Throwables.getRootCause(exception).getMessage());
                errorPlaceHolderView.setVisibility(View.VISIBLE);
            }
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_wireless_iface_togglebutton_title, R.id.tile_status_wireless_iface_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    @Override
    public void onClick(View view) {
        Toast.makeText(this.mParentFragmentActivity, this.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
    }
}
