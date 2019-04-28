package org.rm3l.router_companion.widgets.home.actions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.Toast;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.RebootRouterAction;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterActionListener;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.widgets.ConfirmDialogAsActivity;

/**
 * Created by rm3l on 21/04/15.
 */
public class RouterRebootWidgetConfirmationDialogFromWidgetActivity
        extends ConfirmDialogAsActivity {

    private Router mRouter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        final String routerUuid = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);

        if (routerUuid == null
                || (mRouter = RouterManagementActivity.getDao(this).getRouter(routerUuid)) == null) {
            Toast.makeText(this, "Unknown Router. May have been removed. Please reconfigure the widget!",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
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
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RouterRebootWidgetConfirmationDialogFromWidgetActivity.this,
                                String.format("Rebooting router '%s' (%s) ...", mRouter.getName(),
                                        mRouter.getRemoteIpAddress()), Toast.LENGTH_SHORT).show();
                    }
                });

                ActionManager.runTasks(new RebootRouterAction(mRouter,
                        RouterRebootWidgetConfirmationDialogFromWidgetActivity.this,
                        new RouterActionListener() {
                            @Override
                            public void onRouterActionFailure(@NonNull final RouterAction routerAction,
                                    @NonNull final Router router, @Nullable final Exception exception) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(RouterRebootWidgetConfirmationDialogFromWidgetActivity.this,
                                                String.format("Error on action '%s': %s", routerAction.toString(),
                                                        Utils.handleException(exception).first), Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                });
                            }

                            @Override
                            public void onRouterActionSuccess(@NonNull final RouterAction routerAction,
                                    @NonNull final Router router, Object returnData) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(RouterRebootWidgetConfirmationDialogFromWidgetActivity.this,
                                                String.format("Action '%s' executed successfully on '%s'.",
                                                        routerAction.toString(), router.getRemoteIpAddress()),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }, RouterRebootWidgetConfirmationDialogFromWidgetActivity.this.getSharedPreferences(
                        RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)));
            }
        };
    }
}
