package org.rm3l.router_companion.mgmt.register;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import org.codepond.wizardroid.WizardStep;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.register.resources.RouterWizardAction;
import org.rm3l.router_companion.mgmt.register.steps.BasicDetailsStep;
import org.rm3l.router_companion.mgmt.register.steps.LocalSSIDLookupStep;
import org.rm3l.router_companion.mgmt.register.steps.ReviewStep;
import org.rm3l.router_companion.mgmt.register.steps.RouterConnectionDetailsStep;
import org.rm3l.router_companion.widgets.wizard.MaterialWizard;
import org.rm3l.router_companion.widgets.wizard.WizardStepVerifiable;

/** Created by rm3l on 14/03/16. */
public class ManageRouterWizard extends MaterialWizard {

  private int action;

  /**
   * Note that initially MaterialWizard inherits from {@link androidx.fragment.app.Fragment} and
   * therefore you must have an empty constructor
   */
  public ManageRouterWizard() {
    super();
  }

  public int getAction() {
    return action;
  }

  public ManageRouterWizard setAction(int action) {
    this.action = action;
    return this;
  }

  @Nullable
  @Override
  protected Intent getActivityIntentToReturnUponClose() {
    final Intent intent = new Intent();
    final String wizardRouter =
        getContext()
            .getSharedPreferences(
                RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getString(ManageRouterWizard.class.getSimpleName(), null);
    if (wizardRouter != null && !wizardRouter.isEmpty()) {
      intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, wizardRouter);
    }
    return intent;
  }

  @Nullable
  @Override
  protected String getFirstStepWizardSubTitle() {
    return BasicDetailsStep.getTitle();
  }

  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  protected <T extends WizardStep & WizardStepVerifiable> List<Class<T>> getStepClasses() {
    return Arrays.asList(
        (Class<T>) BasicDetailsStep.class,
        (Class<T>) RouterConnectionDetailsStep.class,
        (Class<T>) LocalSSIDLookupStep.class,
        (Class<T>) ReviewStep.class);
  }

  @NonNull
  @Override
  protected String getWizardTitle() {
    switch (action) {
      case RouterWizardAction.EDIT:
        return "Edit Router";
      default:
        return "Register a Router";
    }
  }
}
