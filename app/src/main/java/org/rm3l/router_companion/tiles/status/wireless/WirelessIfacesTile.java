/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.router_companion.tiles.status.wireless;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.ImageButton;

import com.cocosw.undobar.UndoBarController;
import com.crashlytics.android.Crashlytics;

import org.rm3l.router_companion.BuildConfig;
import org.rm3l.router_companion.R;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.actions.ToggleWirelessRadioRouterAction;
import org.rm3l.router_companion.exceptions.DDWRTNoDataException;
import org.rm3l.router_companion.fragments.status.StatusWirelessFragment;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.status.bandwidth.IfacesTile;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.utils.SSHUtils;
import org.rm3l.router_companion.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.nullToEmpty;

public class WirelessIfacesTile extends IfacesTile {

    private static final String TAG = WirelessIfacesTile.class.getSimpleName();
    public static final String WL_RADIO = "WL_RADIO";
    public static final String WL_NO_OUTPUT = "WL_NO_OUTPUT";

    @NonNull
    private List<WirelessIfaceTile> mWirelessIfaceTiles = new ArrayList<>();

    private final AtomicBoolean viewsBuilt = new AtomicBoolean(true);

    private AtomicBoolean isToggleStateActionRunning = new AtomicBoolean(false);
    private AsyncTaskLoader<NVRAMInfo> mLoader;

    public WirelessIfacesTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, Router router) {
        super(parentFragment, arguments, router);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected Loader<NVRAMInfo> getLoader(final int id, final Bundle args) {

        mLoader = new AsyncTaskLoader<NVRAMInfo>(this.mParentFragmentActivity) {

            @Nullable
            @Override
            public NVRAMInfo loadInBackground() {

                final NVRAMInfo nvramInfo = WirelessIfacesTile.super.doLoadInBackground();

                try {
                    if (nvramInfo != null) {
                        final String[] wlRadioOutput = SSHUtils
                                .getManualProperty(mParentFragmentActivity, mRouter, mGlobalPreferences,
                                        //On = 0x0000 and off = 0x0001
                                        "/usr/sbin/wl radio 2>/dev/null || /usr/sbin/wl_atheros radio 2>/dev/null");
                        if (wlRadioOutput != null) {
                            nvramInfo.setProperty(WL_RADIO,
                                    (wlRadioOutput.length > 0) ?
                                            ("0x0000".equals(wlRadioOutput[0]) ? "1" : "0") : WL_NO_OUTPUT);
                        }
                    }
                } catch (Exception e) {
                    Utils.reportException(null, e);
                    e.printStackTrace();
                }

                //Also set details
                mWirelessIfaceTiles.clear();

                updateProgressBarViewSeparator(25);
                mParentFragmentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        mProgressBar.setVisibility(View.VISIBLE);
//                        mProgressBarDesc.setVisibility(View.VISIBLE);
                        mProgressBar.setProgress(25);
                        mProgressBarDesc.setText("Retrieving list of wireless ifaces...");
                    }
                });

                final Collection<WirelessIfaceTile> wirelessIfaceTiles = StatusWirelessFragment
                        .getWirelessIfaceTiles(args, mParentFragmentActivity, mParentFragment, mRouter);

                if (wirelessIfaceTiles != null) {
                    mWirelessIfaceTiles = new ArrayList<>(wirelessIfaceTiles);

                    final int size = wirelessIfaceTiles.size();

                    updateProgressBarViewSeparator(57);
                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setProgress(57);
                            mProgressBarDesc.setText(
                                    String.format("Retrieved %d wireless ifaces. Now loading their details...",
                                            size));
                        }
                    });
                    int i = 0;
                    boolean allViewsBuilt = true;
                    for (final WirelessIfaceTile mWirelessIfaceTile : mWirelessIfaceTiles) {
                        if (mWirelessIfaceTile == null) {
                            continue;
                        }
                        final AsyncTaskLoader<NVRAMInfo> mWirelessIfaceTileLoader = (AsyncTaskLoader<NVRAMInfo>)
                                mWirelessIfaceTile.getLoader(id, args);
                        if (mWirelessIfaceTileLoader == null) {
                            continue;
                        }

                        final int j = (++i);

                        mParentFragmentActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressBarDesc.setText(
                                        String.format("Retrieving details about iface %s (%d/%d)...",
                                                mWirelessIfaceTile.getIface(),
                                                j, size));
                            }
                        });

                        Crashlytics.log(Log.DEBUG, TAG, "Building view for iface " + mWirelessIfaceTile.getIface());
                        try {
                            new android.os.AsyncTask<Void, Void, NVRAMInfo>() {

                                @Override
                                protected NVRAMInfo doInBackground(Void... params) {
                                    return mWirelessIfaceTileLoader.loadInBackground();
                                }

                                @Override
                                protected void onPostExecute(NVRAMInfo result) {
                                    super.onPostExecute(result);
                                    if (result == null) {
                                        return;
                                    }
                                    mWirelessIfaceTile.buildView(result);
                                }
                            }.execute();

//                            mParentFragmentActivity.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    mWirelessIfaceTile.buildView(
//                                            mWirelessIfaceTileLoader.loadInBackground());
//                                }
//                            });
                            allViewsBuilt &= true;
                        } catch (final Exception e) {
                            Utils.reportException(null, e);
                            e.printStackTrace();
                            allViewsBuilt = false;
                            //No worries
                        }
                    }

                    updateProgressBarViewSeparator(95);
                    mParentFragmentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setProgress(95);
                            mProgressBarDesc.setText("Now building final view...");
                        }
                    });

                    viewsBuilt.set(allViewsBuilt);
                }

                return nvramInfo;
            }
        };
        return mLoader;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<NVRAMInfo> loader, @Nullable NVRAMInfo data) {
        try {
            //Hide Non-wireless lines
            final int[] viewsToHide = new int[]{
                    R.id.tile_status_bandwidth_ifaces_lan_title,
                    R.id.tile_status_bandwidth_ifaces_lan,
                    R.id.tile_status_bandwidth_ifaces_wan,
                    R.id.tile_status_bandwidth_ifaces_wan_title
            };
            for (final int viewToHide : viewsToHide) {
                this.layout.findViewById(viewToHide).setVisibility(View.GONE);
            }

            //Show Radio
            final int[] viewsToShow = new int[]{
                    R.id.tile_status_bandwidth_ifaces_wireless_radio_title,
                    R.id.tile_status_bandwidth_ifaces_wireless_radio_togglebutton
            };
            for (final int viewToShow : viewsToShow) {
                this.layout.findViewById(viewToShow).setVisibility(View.VISIBLE);
            }

            mProgressBar.setProgress(97);
            mProgressBarDesc.setText("Generating views...");

            mProgressBar.setVisibility(View.GONE);
            mProgressBarDesc.setVisibility(View.GONE);

            final GridLayout container = (GridLayout) this.layout
                    .findViewById(R.id.tile_status_bandwidth_ifaces_list_container);
            container.setVisibility(View.VISIBLE);

            //Now add each wireless iface tile

            container.removeAllViews();

            final Resources resources = mParentFragmentActivity.getResources();
            container.setBackgroundColor(
                    ContextCompat.getColor(mParentFragmentActivity, android.R.color.transparent));

            final boolean isThemeLight = ColorUtils.isThemeLight(mParentFragmentActivity);

            Exception preliminaryCheckException = null;
            if (data == null) {
                //noinspection ThrowableInstanceNeverThrown
                preliminaryCheckException = new DDWRTNoDataException("No Data!");
            } else //noinspection ThrowableResultOfMethodCallIgnored
                if (data.getException() == null) {
                    final String wlRadioEnabled = data.getProperty(WL_RADIO);

                    if (wlRadioEnabled == null || !Arrays.asList(WL_NO_OUTPUT, "0", "1").contains(wlRadioEnabled)) {
                        //noinspection ThrowableInstanceNeverThrown
                        preliminaryCheckException = new DDWRTNoDataException("Unknown state");
                    }
                }

            final SwitchCompat enableRadioButton =
                    (SwitchCompat) this.layout.findViewById(R.id.tile_status_bandwidth_ifaces_wireless_radio_togglebutton);
            enableRadioButton.setVisibility(View.VISIBLE);

            final boolean makeToogleEnabled = (data != null &&
                    data.getData() != null &&
                    data.getData().containsKey(WL_RADIO));

            if (!isToggleStateActionRunning.get()) {
                if (makeToogleEnabled) {
                    if ("1".equals(data.getProperty(WL_RADIO))) {
                        //Enabled
                        enableRadioButton.setChecked(true);
                    } else {
                        //Disabled
                        enableRadioButton.setChecked(false);
                    }
                    enableRadioButton.setEnabled(true);
                } else {
                    enableRadioButton.setChecked(false);
                    enableRadioButton.setEnabled(false);
                }

                enableRadioButton.setOnClickListener(new ManageWirelessRadioToggle());
            }

            if (data != null && data.getData() != null) {
                final View enableRadioTitle =
                        layout.findViewById(R.id.tile_status_bandwidth_ifaces_wireless_radio_title);

                if (WL_NO_OUTPUT.equals(data.getProperty(WL_RADIO))) {
                    enableRadioButton.setVisibility(View.GONE);
                    enableRadioTitle.setVisibility(View.GONE);
                } else {
                    enableRadioButton.setVisibility(View.VISIBLE);
                    enableRadioTitle.setVisibility(View.VISIBLE);
                }
            }

            if (preliminaryCheckException != null) {
                data = new NVRAMInfo().setException(preliminaryCheckException);
            }

            Collections.sort(mWirelessIfaceTiles, new Comparator<WirelessIfaceTile>() {
                @Override
                public int compare(WirelessIfaceTile tile0, WirelessIfaceTile tile1) {
                    if (tile0 == tile1) {
                        return 0;
                    }
                    if (tile0 == null) {
                        return 1;
                    }
                    if (tile1 == null) {
                        return -1;
                    }
                    final String tile0Iface = tile0.getIface();
                    final String tile1Iface = tile1.getIface();
                    return nullToEmpty(tile0Iface)
                            .compareToIgnoreCase(nullToEmpty(tile1Iface));
                }
            });

            int i = 0;
            for (final WirelessIfaceTile tile : mWirelessIfaceTiles) {
                if (tile == null) {
                    continue;
                }
                final ViewGroup tileViewGroupLayout = tile.getViewGroupLayout();
                if (tileViewGroupLayout instanceof CardView) {

                    final CardView cardView = (CardView) tileViewGroupLayout;

                    //Create Options Menu
                    final ImageButton tileMenu = (ImageButton) cardView.findViewById(R.id.tile_status_wireless_iface_menu);

                    if (!isThemeLight) {
                        //Set menu background to white
                        tileMenu.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
                    }

                    //Add padding to CardView on v20 and before to prevent intersections between the Card content and rounded corners.
                    cardView.setPreventCornerOverlap(true);
                    //Add padding in API v21+ as well to have the same measurements with previous versions.
                    cardView.setUseCompatPadding(true);

                    //Highlight CardView
//                cardView.setCardElevation(10f);

                    if (isThemeLight) {
                        //Light
                        cardView.setCardBackgroundColor(
                                ContextCompat.getColor(mParentFragmentActivity,
                                        R.color.cardview_light_background));
                    } else {
                        //Default is Dark
                        cardView.setCardBackgroundColor(
                                ContextCompat.getColor(mParentFragmentActivity,
                                        R.color.cardview_dark_background));
                    }
                }
                if (tileViewGroupLayout != null) {
                    container.addView(tileViewGroupLayout, i++);
                }
            }

            super.onLoadFinished(loader, data);
        } finally {
            mRefreshing.set(false);
        }
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        return super.getOnclickIntent();
    }

    private class ManageWirelessRadioToggle implements View.OnClickListener {

        private boolean enable;

        @Override
        public void onClick(View view) {

            isToggleStateActionRunning.set(true);

            if (!(view instanceof CompoundButton)) {
                Utils.reportException(null, new IllegalStateException("ManageWirelessRadioToggle#onClick: " +
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

            if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                Utils.displayUpgradeMessage(mParentFragmentActivity, "Toggle Wireless Radio");
                isToggleStateActionRunning.set(false);
                mParentFragmentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        compoundButton.setChecked(!enable);
                        compoundButton.setEnabled(true);
                    }
                });
                return;
            }

            new UndoBarController.UndoBar(mParentFragmentActivity)
                    .message(String.format("Wireless Radio will be %s on '%s' (%s).",
                            enable ? "enabled" : "disabled",
                            mRouter.getDisplayName(),
                            mRouter.getRemoteIpAddress()))
                    .listener(new UndoBarController.AdvancedUndoListener() {
                                  @Override
                                  public void onHide(@Nullable Parcelable parcelable) {

                                      Utils.displayMessage(mParentFragmentActivity,
                                              String.format("%s Wireless Radio...",
                                                      enable ? "Enabling" : "Disabling"),
                                              Style.INFO);

                                      ActionManager.runTasks(
                                        new ToggleWirelessRadioRouterAction(
                                              mRouter,
                                              mParentFragmentActivity,
                                              new RouterActionListener() {
                                                  @Override
                                                  public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull final Router router, Object returnData) {
                                                      mParentFragmentActivity.runOnUiThread(new Runnable() {
                                                          @Override
                                                          public void run() {

                                                              try {
                                                                  compoundButton.setChecked(enable);
                                                                  Utils.displayMessage(mParentFragmentActivity,
                                                                          String.format("Wireless Radio %s successfully on host '%s' (%s). ",
                                                                                  enable ? "enabled" : "disabled",
                                                                                  router.getDisplayName(),
                                                                                  router.getRemoteIpAddress()),
                                                                          Style.CONFIRM);
                                                              } finally {
                                                                  compoundButton.setEnabled(true);
                                                                  isToggleStateActionRunning.set(false);
                                                                  if (mLoader != null) {
                                                                      //Reload everything right away
                                                                      doneWithLoaderInstance(WirelessIfacesTile.this,
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
                                                                          String.format("Error while trying to %s Wireless Radio on '%s' (%s): %s",
                                                                                  enable ? "enable" : "disable",
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
                                              },
                                              mGlobalPreferences,
                                              enable)
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
