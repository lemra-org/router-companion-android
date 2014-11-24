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

package org.rm3l.ddwrt.tiles.status.wireless;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.common.base.Throwables;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.api.conn.NVRAMInfo;
import org.rm3l.ddwrt.api.conn.Router;
import org.rm3l.ddwrt.exceptions.DDWRTNoDataException;
import org.rm3l.ddwrt.exceptions.DDWRTTileAutoRefreshNotAllowedException;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.utils.SSHUtils;

/**
 *
 */
public class WirelessIfaceTile extends DDWRTTile<NVRAMInfo> {

    public static final String WIRELESS_IFACE = "wireless_iface";
    private static final String LOG_TAG = WirelessIfaceTile.class.getSimpleName();
    @NotNull
    private String iface;

    public WirelessIfaceTile(@NotNull String iface, @NotNull SherlockFragmentActivity parentFragmentActivity, @NotNull Bundle arguments, @Nullable Router router) {
        super(parentFragmentActivity, arguments, router, R.layout.tile_status_wireless_iface, R.id.tile_status_wireless_iface_togglebutton);
        this.iface = iface;
        ((TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_title)).setText(this.iface);
    }

    @Nullable
    @Override
    protected Loader<NVRAMInfo> getLoader(int id, Bundle args) {
        return new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                @NotNull final String wlIface = WirelessIfaceTile.this.iface;

                try {
                    Log.d(LOG_TAG, "Init background loader for " + WirelessIfaceTile.class + ": routerInfo=" +
                            mRouter + " / this.mAutoRefreshToggle= " + mAutoRefreshToggle + " / nbRunsLoader=" + nbRunsLoader);

                    if (nbRunsLoader > 0 && !mAutoRefreshToggle) {
                        //Skip run
                        Log.d(LOG_TAG, "Skip loader run");
                        return new NVRAMInfo().setException(new DDWRTTileAutoRefreshNotAllowedException());
                    }
                    nbRunsLoader++;

                    @Nullable final NVRAMInfo nvramInfo = SSHUtils.getNVRamInfoFromRouter(mRouter,
                            wlIface + "_radio",
                            wlIface + "_mode",
                            wlIface + "_hwaddr",
                            wlIface + "_ifname",
                            wlIface + "_net_mode",
                            wlIface + "_ssid",
                            wlIface + "_channel",
                            wlIface + "_txpwr",
                            wlIface + "_rate",
                            wlIface + "_akm");

                    if (nvramInfo != null) {
                        nvramInfo.setProperty(WIRELESS_IFACE, wlIface);
                    }

                    return nvramInfo;

                } catch (@NotNull final Exception e) {
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
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(@NotNull Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {
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

            //SSID
            @NotNull final TextView ssidView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_ssid);
            if (ssidView != null) {
                ssidView.setText(data.getProperty(this.iface + "_ssid", "SSID N/A"));
            }

            //Ifname
            @NotNull final TextView ifnameView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_ifname);
            if (ifnameView != null) {
                ifnameView.setText(data.getProperty(this.iface + "_ifname", "N/A"));
            }

            //MAC
            @NotNull final TextView hwAddrView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_mac_address);
            if (hwAddrView != null) {
                hwAddrView.setText(data.getProperty(this.iface + "_hwaddr", "N/A"));
            }

            //Radio
            @NotNull final CheckBox radioView = (CheckBox) this.layout.findViewById(R.id.tile_status_wireless_iface_radio);
            if (radioView != null) {
                radioView.setEnabled(false);
                radioView.setChecked("1".equals(data.getProperty(this.iface + "_radio")));
            }

            //Mode
            @NotNull final TextView modeView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_mode);
            if (modeView != null) {
                modeView.setText(data.getProperty(this.iface + "_mode", "N/A").toUpperCase());
            }

            //Net Mode
            @NotNull final TextView netModeView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_network);
            if (netModeView != null) {
                final String netmode = data.getProperty(this.iface + "_net_mode");
                String mode = "N/A";
                if ("disabled".equalsIgnoreCase(netmode)) {
                    mode = "Disabled";
                } else if ("mixed".equalsIgnoreCase(netmode)) {
                    mode = "Mixed";
                } else if ("g-only".equalsIgnoreCase(netmode)) {
                    mode = "G-Only";
                } else if ("b-only".equalsIgnoreCase(netmode)) {
                    mode = "B-Only";
                } else if ("a-only".equalsIgnoreCase(netmode)) {
                    mode = "A-Only";
                } else if ("n-only".equalsIgnoreCase(netmode)) {
                    mode = "N-Only";
                } else if ("ng-only".equalsIgnoreCase(netmode)) {
                    mode = "NG-Only";
                } else if ("n5-only".equalsIgnoreCase(netmode)) {
                    mode = "N-Only (5GHz)";
                } else if (netmode != null) {
                    mode = netmode;
                }

                netModeView.setText(mode);
            }

            //Channel
            @NotNull final TextView channelView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_channel);
            if (channelView != null) {
                channelView.setText(data.getProperty(this.iface + "_channel", "N/A"));
            }

            //Rate
            @NotNull final TextView rateView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_rate);
            if (rateView != null) {
                rateView.setText(data.getProperty(this.iface + "_rate", "N/A"));
            }

            //TX Power
            @NotNull final TextView xmitView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_tx_power);
            if (xmitView != null) {
                xmitView.setText(data.getProperty(this.iface + "_txpwr", "N/A"));
            }

            //Encryption
            @NotNull final TextView encryptionView = (TextView) this.layout.findViewById(R.id.tile_status_wireless_iface_encryption);
            if (encryptionView != null) {
                final String akm = data.getProperty(this.iface + "_akm");
                @NotNull String encryption = "N/A";
                if ("psk".equalsIgnoreCase(akm)) {
                    encryption = "WPA Pre-shared Key";
                } else if ("wpa".equalsIgnoreCase(akm)) {
                    encryption = "WPA RADIUS";
                } else if ("psk2".equalsIgnoreCase(akm)) {
                    encryption = "WPA2 Pre-shared Key";
                } else if ("wpa2".equalsIgnoreCase(akm)) {
                    encryption = "WPA2 RADIUS";
                } else if ("psk psk2".equalsIgnoreCase(akm)) {
                    encryption = "WPA2 Pre-shared Key Mixed";
                } else if ("wpa wpa2".equalsIgnoreCase(akm)) {
                    encryption = "WPA RADIUS Mixed";
                } else if ("radius".equalsIgnoreCase(akm)) {
                    encryption = "RADIUS";
                } else if ("wep".equalsIgnoreCase(akm)) {
                    encryption = "WEP";
                }

                encryptionView.setText(encryption);
            }

        }

        if (exception != null) {
            errorPlaceHolderView.setText("Error: " + Throwables.getRootCause(exception).getMessage());
            errorPlaceHolderView.setVisibility(View.VISIBLE);
        }

        doneWithLoaderInstance(this, loader,
                R.id.tile_status_wireless_iface_togglebutton_title, R.id.tile_status_wireless_iface_togglebutton_separator);

        Log.d(LOG_TAG, "onLoadFinished(): done loading!");
    }

    @Nullable
    @Override
    protected Intent getOnclickIntent() {
        //TODO
        return null;
    }

    @NotNull
    public String getIface() {
        return iface;
    }

    public void setIface(@NotNull String iface) {
        this.iface = iface;
    }
}
