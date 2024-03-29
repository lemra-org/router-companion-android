package org.rm3l.router_companion.widgets.home.wol;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.actions.WakeOnLANRouterAction;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.services.wol.WakeOnLanTile;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.widgets.ConfirmDialogAsActivity;

/** Created by rm3l on 21/04/15. */
public class RouterWolWidgetConfirmationDialogFromWidgetActivity extends ConfirmDialogAsActivity {

  public static final String HOSTS_TO_WAKE = "HOSTS_TO_WAKE";

  public static final String LOAD_HOSTS_FROM_PREFS = "LOAD_HOSTS_FROM_PREFS";

  private String[] mBroadcastAddresses;

  private ArrayList<Device> mCurrentDevicesList;

  private Router mRouter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final Intent intent = getIntent();

    final String routerUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);

    if (routerUuid == null
        || (mRouter = RouterManagementActivity.Companion.getDao(this).getRouter(routerUuid))
            == null) {
      Toast.makeText(
              this,
              "Unknown Router. May have been removed. Please reconfigure the widget!",
              Toast.LENGTH_SHORT)
          .show();
      finish();
      return;
    }

    final String wolHosts;
    if (intent.getBooleanExtra(LOAD_HOSTS_FROM_PREFS, false)) {
      wolHosts =
          getSharedPreferences(routerUuid, Context.MODE_PRIVATE)
              .getString(WOLWidgetService.WOL_HOST_PREF_KEY, null);
    } else {
      wolHosts = intent.getStringExtra(HOSTS_TO_WAKE);
    }

    if (wolHosts == null || wolHosts.isEmpty()) {
      Toast.makeText(this, "No Host found. Please try again later!", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    try {
      mCurrentDevicesList =
          WakeOnLanTile.GSON_BUILDER
              .create()
              .fromJson(wolHosts, new TypeToken<ArrayList<Device>>() {}.getType());
    } catch (final Exception e) {
      e.printStackTrace();
      Utils.reportException(null, e);
      Toast.makeText(this, "Internal Error - please try again later!", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    if (mCurrentDevicesList == null || mCurrentDevicesList.isEmpty()) {
      Toast.makeText(this, "No Host found. Please try again later!", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    final Set<String> bCastAddrs =
        getSharedPreferences(routerUuid, Context.MODE_PRIVATE)
            .getStringSet(WOLWidgetService.WOL_BCAST_ADDRESSES_PREF_KEY, new HashSet<String>());

    //noinspection ConstantConditions
    mBroadcastAddresses = bCastAddrs.toArray(new String[bCastAddrs.size()]);
  }

  @Override
  protected View.OnClickListener getNoButtonOnClickListener() {
    return null;
  }

  @Override
  protected View.OnClickListener getYesButtonOnClickListener() {
    final Handler handler = new Handler(Looper.getMainLooper());

    return new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        handler.post(
            new Runnable() {
              @Override
              public void run() {
                Toast.makeText(
                        RouterWolWidgetConfirmationDialogFromWidgetActivity.this,
                        String.format(
                            "Waking %d host(s) from Router '%s' (%s) ...",
                            mCurrentDevicesList.size(),
                            mRouter.getName(),
                            mRouter.getRemoteIpAddress()),
                        Toast.LENGTH_SHORT)
                    .show();
              }
            });

        final AtomicInteger currentNum = new AtomicInteger(0);
        final AtomicInteger numActionsWithNoSuccess = new AtomicInteger(0);
        final int totalNumOfDevices = mCurrentDevicesList.size();

        final WakeOnLANRouterAction[] wolActions = new WakeOnLANRouterAction[totalNumOfDevices];
        int i = 0;
        for (final Device device : mCurrentDevicesList) {
          wolActions[i++] =
              new WakeOnLANRouterAction(
                  mRouter,
                  RouterWolWidgetConfirmationDialogFromWidgetActivity.this,
                  new RouterActionListener() {
                    @Override
                    public void onRouterActionFailure(
                        @NonNull final RouterAction routerAction,
                        @NonNull final Router router,
                        @Nullable final Exception exception) {
                      final int incrementAndGet = currentNum.incrementAndGet();
                      numActionsWithNoSuccess.incrementAndGet();
                      if (incrementAndGet >= totalNumOfDevices) {
                        // An error occurred
                        handler.post(
                            new Runnable() {
                              @Override
                              public void run() {
                                Toast.makeText(
                                        RouterWolWidgetConfirmationDialogFromWidgetActivity.this,
                                        String.format(
                                            "Action '%s' executed but %d error(s) occurred: %s",
                                            routerAction.toString(),
                                            numActionsWithNoSuccess.get(),
                                            Utils.handleException(exception).first),
                                        Toast.LENGTH_SHORT)
                                    .show();
                              }
                            });
                      }
                    }

                    @Override
                    public void onRouterActionSuccess(
                        @NonNull final RouterAction routerAction,
                        @NonNull final Router router,
                        Object returnData) {
                      final int incrementAndGet = currentNum.incrementAndGet();
                      if (incrementAndGet >= totalNumOfDevices) {
                        final int numActionsThatDidNotSucceed = numActionsWithNoSuccess.get();
                        if (numActionsThatDidNotSucceed > 0) {
                          // An error occurred
                          handler.post(
                              new Runnable() {
                                @Override
                                public void run() {
                                  Toast.makeText(
                                          RouterWolWidgetConfirmationDialogFromWidgetActivity.this,
                                          String.format(
                                              "Action '%s' executed but %d error(s) occurred",
                                              routerAction.toString(), numActionsThatDidNotSucceed),
                                          Toast.LENGTH_SHORT)
                                      .show();
                                }
                              });
                        } else {
                          // No error
                          handler.post(
                              new Runnable() {
                                @Override
                                public void run() {
                                  Toast.makeText(
                                          RouterWolWidgetConfirmationDialogFromWidgetActivity.this,
                                          String.format(
                                              "Action '%s' executed successfully on host '%s'",
                                              routerAction.toString(), router.getRemoteIpAddress()),
                                          Toast.LENGTH_SHORT)
                                      .show();
                                }
                              });
                        }
                      }
                    }
                  },
                  getSharedPreferences(
                      RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                      Context.MODE_PRIVATE),
                  device,
                  device.getWolPort(),
                  mBroadcastAddresses);
        }
        ActionManager.runTasks(wolActions);
      }
    };
  }
}
