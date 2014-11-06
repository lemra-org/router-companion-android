package org.lemra.dd_wrt.tiles.syslog;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.jetbrains.annotations.NotNull;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.NVRAMInfo;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.tiles.DDWRTTile;

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
        return null;
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    public void onLoadFinished(Loader<NVRAMInfo> loader, NVRAMInfo nvramInfo) {

    }

    @Override
    public void onClick(View view) {
        Toast.makeText(this.mParentFragmentActivity, this.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
    }
}
