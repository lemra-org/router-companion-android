package org.rm3l.router_companion.widgets.wizard;

import static org.rm3l.router_companion.mgmt.register.resources.RouterWizardAction.ROUTER_WIZARD_ACTION;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.LayoutRes;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.register.ManageRouterWizard;
import org.rm3l.router_companion.mgmt.register.resources.RouterWizardAction;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.AppShortcutUtils;
import org.rm3l.router_companion.utils.ColorUtils;

/** Created by rm3l on 01/04/16. */
public abstract class MaterialWizardFragmentActivity extends FragmentActivity {

  @Override
  public final void onCreate(Bundle savedInstanceState) {

    if (isFullScreen()) {
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      getWindow()
          .setFlags(
              WindowManager.LayoutParams.FLAG_FULLSCREEN,
              WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    super.onCreate(savedInstanceState);

    final Intent intent = getIntent();
    if (intent != null) {
      final String routerSelected = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
      if (routerSelected != null) {
        final Router router =
            RouterManagementActivity.Companion.getDao(this).getRouter(routerSelected);
        ColorUtils.Companion.setAppTheme(
            this, router != null ? router.getRouterFirmware() : null, false);
      } else {
        ColorUtils.Companion.setAppTheme(this, null, false);
      }
    } else {
      ColorUtils.Companion.setAppTheme(this, null, false);
    }

    //        if (ColorUtils.isThemeLight(this)) {
    //            //Light
    //            setTheme(R.style.AppThemeLight);
    ////            getWindow().getDecorView()
    ////                    .setBackgroundColor(ContextCompat.getColor(this, R.color.GhostWhite));
    //        } else {
    //            //Default is Dark
    //            setTheme(R.style.AppThemeDark);
    //        }

    setContentView(getContentView());

    final FragmentManager fragmentManager = getSupportFragmentManager();
    final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

    final ManageRouterWizard fragment = new ManageRouterWizard();
    final Bundle args = new Bundle();
    if (intent != null) {
      final String routerSelected = intent.getStringExtra(RouterManagementActivity.ROUTER_SELECTED);
      if (routerSelected != null) {
        args.putString(RouterManagementActivity.ROUTER_SELECTED, routerSelected);
      }
      final int routerWizardAction =
          intent.getIntExtra(ROUTER_WIZARD_ACTION, RouterWizardAction.ADD);
      if (routerWizardAction == RouterWizardAction.ADD) {
        // #199: report app shortcut
        AppShortcutUtils.reportShortcutUsed(this, "register-router");
      }
      fragment.setAction(routerWizardAction);
      args.putInt(ROUTER_WIZARD_ACTION, routerWizardAction);
    } else {
      // By default, this is an "Add Router" operation
      // #199: report app shortcut
      AppShortcutUtils.reportShortcutUsed(this, "register-router");
    }
    fragment.setArguments(args);
    fragmentTransaction.add(
        R.id.wizard_add_router_fragment_container,
        fragment,
        ManageRouterWizard.class.getSimpleName());
    fragmentTransaction.commit();
  }

  @LayoutRes
  protected abstract int getContentView();

  /**
   * Override to make activity full screen.
   *
   * @return whether to make activity full screen
   */
  protected boolean isFullScreen() {
    return false;
  }
}
