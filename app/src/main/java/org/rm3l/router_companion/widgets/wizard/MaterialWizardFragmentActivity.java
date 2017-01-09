package org.rm3l.router_companion.widgets.wizard;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;
import android.view.WindowManager;

import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.mgmt.register.ManageRouterWizard;
import org.rm3l.router_companion.mgmt.register.resources.RouterWizardAction;
import org.rm3l.router_companion.utils.AppShortcutUtils;
import org.rm3l.router_companion.utils.ColorUtils;

import static org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.router_companion.mgmt.register.resources.RouterWizardAction.ROUTER_WIZARD_ACTION;

/**
 * Created by rm3l on 01/04/16.
 */
public abstract class MaterialWizardFragmentActivity extends FragmentActivity {

    @Override
    public final void onCreate(Bundle savedInstanceState) {

        if (isFullScreen()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        super.onCreate(savedInstanceState);

        if (ColorUtils.isThemeLight(this)) {
            //Light
            setTheme(R.style.AppThemeLight);
//            getWindow().getDecorView()
//                    .setBackgroundColor(ContextCompat.getColor(this, R.color.GhostWhite));
        } else {
            //Default is Dark
            setTheme(R.style.AppThemeDark);
        }

        setContentView(getContentView());

        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        final ManageRouterWizard fragment = new ManageRouterWizard();
        final Bundle args = new Bundle();
        final Intent intent = getIntent();
        if (intent != null) {
            final String routerSelected = intent.getStringExtra(ROUTER_SELECTED);
            if (routerSelected != null) {
                args.putString(ROUTER_SELECTED, routerSelected);
            }
            final int routerWizardAction = intent.getIntExtra(ROUTER_WIZARD_ACTION,
                    RouterWizardAction.ADD);
            if (routerWizardAction == RouterWizardAction.ADD) {
                //#199: report app shortcut
                AppShortcutUtils.reportShortcutUsed(this, "register-router");
            }
            fragment.setAction(routerWizardAction);
            args.putInt(ROUTER_WIZARD_ACTION, routerWizardAction);
        } else {
            //By default, this is an "Add Router" operation
            //#199: report app shortcut
            AppShortcutUtils.reportShortcutUsed(this, "register-router");
        }
        fragment.setArguments(args);
        fragmentTransaction.add(R.id.wizard_add_router_fragment_container,
                fragment,
                ManageRouterWizard.class.getSimpleName());
        fragmentTransaction.commit();
    }

    @LayoutRes
    protected abstract int getContentView();

    /**
     * Override to make activity full screen.
     * @return whether to make activity full screen
     */
    protected boolean isFullScreen() {
        return false;
    }

}
