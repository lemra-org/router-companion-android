package org.rm3l.router_companion.widgets.wizard;

import static org.rm3l.router_companion.widgets.wizard.MaterialWizard.CURRENT_WIZARD_CONTEXT_PREF_KEY;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.codepond.wizardroid.WizardStep;
import org.codepond.wizardroid.persistence.ContextVariable;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.events.wizard.WizardStepVisibleToUserEvent;
import org.rm3l.router_companion.resources.Encrypted;
import org.rm3l.router_companion.utils.ReportingUtils;

// import com.squareup.otto.Bus;
// import com.squareup.otto.Produce;

/** Created by rm3l on 28/03/16. */
public abstract class MaterialWizardStep extends WizardStep implements WizardStepVerifiable {

  private static final String LOG_TAG = MaterialWizardStep.class.getSimpleName();

  // protected final Bus busInstance = BusSingleton.getBusInstance();

  // create boolean for fetching data
  protected boolean isViewShown = false;

  private boolean isStarted = false;

  private boolean isVisible = false;

  @Override
  public void onStart() {
    super.onStart();
    isStarted = true;
    isViewShown = (isVisible && isStarted && getView() != null);
    if (isViewShown) {
      viewDidAppear();
    } else {
      viewDidHide();
    }
  }

  public String getWizardStepTitle() {
    return null;
  }

  /** Called whenever the wizard proceeds to the next step or goes back to the previous step */
  @Override
  public final void onExit(int exitCode) {}

  /** Called whenever the wizard proceeds to the next step or goes back to the previous step */
  public final void onExitSynchronous(int exitCode) {
    switch (exitCode) {
      case WizardStep.EXIT_NEXT:
        Log.d(this.getClass().getSimpleName(), "onExit( NEXT> )");
        this.onExitNext();
        this.doPersistContextVariableFields();
        break;
      case WizardStep.EXIT_PREVIOUS:
        Log.d(this.getClass().getSimpleName(), "onExit( <PREVIOUS )");
        // Do nothing...
        break;
    }
  }

  // @Produce
  public WizardStepVisibleToUserEvent produceInitialEvent() {
    return new WizardStepVisibleToUserEvent(this.getWizardStepTitle());
  }

  @Override
  public final void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);
    Log.d(
        this.getClass().getSimpleName(),
        "setUserVisibleHint(isVisibleToUser=" + isVisibleToUser + ")");
    isVisible = isVisibleToUser;
    isViewShown = (isVisible && isStarted && getView() != null);

    if (isViewShown) {
      viewDidAppear();
    } else {
      viewDidHide();
    }

    //        if (getView() != null) {
    //            isViewShown = true;
    //            if (isVisibleToUser) {
    //                busInstance.register(this);
    //                this.onVisibleToUser();
    //                FirebaseCrashlytics.getInstance().log(
    //                        "POST event wizardStepVisibleToUser(" + getWizardStepTitle() + ") on
    // bus " +
    //                                busInstance);
    //                busInstance.post(
    //                        new WizardStepVisibleToUserEvent(this.getWizardStepTitle()));
    //            } else {
    //                busInstance.unregister(this);
    //                onHiddenToUser();
    //            }
    //        } else {
    //            busInstance.unregister(this);
    //            isViewShown = false;
    //            onHiddenToUser();
    //        }
  }

  /** Override this to enable persist context variable fields */
  protected void onExitNext() {}

  /** Override this if needed */
  protected void onHiddenToUser() {}

  /** Override this if needed */
  protected void onVisibleToUser() {}

  @SuppressWarnings("unchecked")
  private void doPersistContextVariableFields() {

    try {
      final SharedPreferences globalPrefs =
          getContext()
              .getSharedPreferences(
                  RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);

      final Gson gson = MaterialWizard.GSON_BUILDER.create();
      final Map wizardContextMap = new HashMap(MaterialWizard.getWizardContext(getContext()));

      final Field[] fields = this.getClass().getDeclaredFields();
      for (Field field : fields) {
        if (field.isAnnotationPresent(ContextVariable.class)) {
          field.setAccessible(true);
          final Object value = field.get(this);
          final String key = field.getName();
          if (value == null) {
            wizardContextMap.remove(key);
          } else {
            if (key != null && key.toLowerCase().contains("password")) {
              // Encrypt
              wizardContextMap.put(key, Encrypted.e(value.toString()));
            } else {
              wizardContextMap.put(key, value);
            }
          }
        }
      }
      globalPrefs
          .edit()
          .putString(CURRENT_WIZARD_CONTEXT_PREF_KEY, gson.toJson(wizardContextMap))
          .apply();
    } catch (final Exception e) {
      ReportingUtils.reportException(getContext(), e);
    }
  }

  private void viewDidAppear() {
    // your logic
    // busInstance.register(this);
    this.onVisibleToUser();
    // FirebaseCrashlytics.getInstance().log(
    //    "POST event wizardStepVisibleToUser(" + getWizardStepTitle() + ") on bus " + busInstance);
    // busInstance.post(new WizardStepVisibleToUserEvent(this.getWizardStepTitle()));
  }

  private void viewDidHide() {
    // your logic
    // busInstance.unregister(this);
    isViewShown = false;
    onHiddenToUser();
  }
}
