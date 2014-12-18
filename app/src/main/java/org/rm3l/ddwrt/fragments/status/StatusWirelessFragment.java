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

package org.rm3l.ddwrt.fragments.status;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TableRow;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.fragments.DDWRTBaseFragment;
import org.rm3l.ddwrt.resources.conn.NVRAMInfo;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile;
import org.rm3l.ddwrt.tiles.status.wireless.WirelessIfaceTile;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class StatusWirelessFragment extends DDWRTBaseFragment<Collection<DDWRTTile>> {

    private static final String LOG_TAG = StatusWirelessFragment.class.getSimpleName();

    @Nullable
    private Collection<DDWRTTile> mIfaceTiles = null;

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Arrays.<DDWRTTile>asList(new WirelessClientsTile(this, savedInstanceState, router));
    }

    @Nullable
    @Override
    protected Loader<Collection<DDWRTTile>> getLoader(final int id, @NotNull final Bundle args) {

        return new AsyncTaskLoader<Collection<DDWRTTile>>(getSherlockActivity()) {

            @Nullable
            @Override
            public Collection<DDWRTTile> loadInBackground() {

                try {
                    Log.d(LOG_TAG, "Init background loader for " + StatusWirelessFragment.class + ": routerInfo=" +
                            router);

                    final SherlockFragment parentFragment= StatusWirelessFragment.this;

                    if (DDWRTCompanionConstants.TEST_MODE) {
                        return Arrays.<DDWRTTile>asList(new WirelessIfaceTile("eth0.test", parentFragment, args, router),
                                new WirelessIfaceTile("eth1.test", parentFragment, args, router),
                                new WirelessIfaceTile("eth2.test", parentFragment, args, router));
                    }

                    @Nullable final NVRAMInfo nvramInfo = SSHUtils.getNVRamInfoFromRouter(StatusWirelessFragment.this.router,
                            NVRAMInfo.LANDEVS);

                    if (nvramInfo == null) {
                        return null;
                    }

                    final String landevs = nvramInfo.getProperty(NVRAMInfo.LANDEVS);
                    if (landevs == null) {
                        return null;
                    }

                    final List<String> splitToList = Splitter.on(" ").omitEmptyStrings().trimResults().splitToList(landevs);
                    if (splitToList == null || splitToList.isEmpty()) {
                        return null;
                    }

                    final List<DDWRTTile> tiles = Lists.newArrayList();

                    for (@Nullable final String landev : splitToList) {
                        if (landev == null || !(landev.startsWith("wl") || landev.startsWith("ath"))) {
                            continue;
                        }
                        tiles.add(new WirelessIfaceTile(landev, parentFragment, args, router));

                    }

                    if (tiles.isEmpty()) {
                        return null;
                    }

                    return tiles;

                } catch (@NotNull final Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };

    }

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link android.support.v4.app.FragmentManager#beginTransaction()
     * FragmentManager.openTransaction()} for further discussion on this.
     * <p/>
     * <p>This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.  In particular:
     * <p/>
     * <ul>
     * <li> <p>The Loader will monitor for changes to the data, and report
     * them to you through new calls here.  You should not monitor the
     * data yourself.  For example, if the data is a {@link android.database.Cursor}
     * and you place it in a {@link android.widget.CursorAdapter}, use
     * the {@link android.widget.CursorAdapter#CursorAdapter(android.content.Context,
     * android.database.Cursor, int)} constructor <em>without</em> passing
     * in either {@link android.widget.CursorAdapter#FLAG_AUTO_REQUERY}
     * or {@link android.widget.CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER}
     * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
     * from doing its own observing of the Cursor, which is not needed since
     * when a change happens you will get a new Cursor throw another call
     * here.
     * <li> The Loader will release the data once it knows the application
     * is no longer using it.  For example, if the data is
     * a {@link android.database.Cursor} from a {@link android.content.CursorLoader},
     * you should not call close() on it yourself.  If the Cursor is being placed in a
     * {@link android.widget.CursorAdapter}, you should use the
     * {@link android.widget.CursorAdapter#swapCursor(android.database.Cursor)}
     * method so that the old Cursor is not closed.
     * </ul>
     *
     * @param loader The Loader that has finished.
     * @param tiles  The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(final Loader<Collection<DDWRTTile>> loader, @Nullable final Collection<DDWRTTile> tiles) {
        Log.d(LOG_TAG, "Done loading background task for " + StatusWirelessFragment.class.getCanonicalName());
        this.mIfaceTiles = tiles;

        if (tiles == null || this.mLayout == null) {
            return;
        }

        for (@NotNull final DDWRTTile tile : tiles) {
            @Nullable final ViewGroup tileViewGroupLayout = tile.getViewGroupLayout();
            if (tileViewGroupLayout == null) {
                continue;
            }

            //Init loaders for these tiles
            getLoaderManager().initLoader(Utils.getNextLoaderId(), null, tile);

            //Add row for this iface
            final CardView cardView = new CardView(getSherlockActivity());
            cardView.setOnClickListener(tile);
            tileViewGroupLayout.setOnClickListener(tile);

            //Remove view prior to adding it again to parent
            cardView.removeView(tileViewGroupLayout);
            cardView.addView(tileViewGroupLayout);

            //Remove view prior to adding it again to parent
            this.mLayout.removeView(cardView);
            this.mLayout.addView(cardView);
        }


    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Collection<DDWRTTile>> loader) {
        super.onLoaderReset(loader);
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.  The next time the fragment needs
     * to be displayed, a new view will be created.  This is called
     * after {@link #onStop()} and before {@link #onDestroy()}.  It is called
     * <em>regardless</em> of whether {@link #onCreateView} returned a
     * non-null view.  Internally it is called after the view's state has
     * been saved but before it has been removed from its parent.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mIfaceTiles != null) {
            int l = 100;
            final LoaderManager supportLoaderManager = getLoaderManager();
            for (int i = l; i < l + mIfaceTiles.size(); i++) {
                supportLoaderManager.destroyLoader(i);
            }
        }
    }
}
