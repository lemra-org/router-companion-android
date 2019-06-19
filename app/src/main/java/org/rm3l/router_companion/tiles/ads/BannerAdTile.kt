package org.rm3l.router_companion.tiles.ads

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.loader.content.AsyncTaskLoader
import androidx.core.content.ContextCompat
import androidx.loader.content.Loader
import com.google.android.gms.ads.AdView
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.tiles.DDWRTTile
import org.rm3l.router_companion.utils.AdUtils

class BannerAdTile(
    parentFragment: Fragment, arguments: Bundle?,
    router: Router?
) : DDWRTTile<Unit>(parentFragment, arguments, router, R.layout.tile_adview, null) {

    override fun getTileBackgroundColor(): Int? {
        return ContextCompat.getColor(mParentFragmentActivity, android.R.color.transparent)
    }

    override fun isAdTile(): Boolean {
        return true
    }

    override fun onLoadFinished(loader: Loader<Unit>, data: Unit?) {
        AdUtils.buildAndDisplayAdViewIfNeeded(
            mParentFragmentActivity,
            layout.findViewById(R.id.router_main_activity_tile_adView) as AdView
        )
    }

    override fun getLoader(id: Int, args: Bundle?): Loader<Unit>? {
        return object : AsyncTaskLoader<Unit>(mParentFragmentActivity) {
            override fun loadInBackground() {}
        }
    }
}
