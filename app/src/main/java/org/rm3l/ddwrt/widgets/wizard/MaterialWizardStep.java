package org.rm3l.ddwrt.widgets.wizard;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.codepond.wizardroid.WizardStep;
import org.codepond.wizardroid.persistence.ContextVariable;
import org.rm3l.ddwrt.resources.Encrypted;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.rm3l.ddwrt.widgets.wizard.MaterialWizard.CURRENT_WIZARD_CONTEXT_PREF_KEY;

/**
 * Created by rm3l on 28/03/16.
 */
public abstract class MaterialWizardStep extends WizardStep implements WizardStepVerifiable {

    // create boolean for fetching data
    protected boolean isViewShown = false;

    /**
      * Called whenever the wizard proceeds to the next step or goes back to the previous step
      */
    public final void onExitSynchronous(int exitCode) {
        switch (exitCode) {
            case WizardStep.EXIT_NEXT:
                Log.d(this.getClass().getSimpleName(), "onExit( NEXT> )");
                this.onExitNext();
                this.doPersistContextVariableFields();
                break;
            case WizardStep.EXIT_PREVIOUS:
                Log.d(this.getClass().getSimpleName(), "onExit( <PREVIOUS )");
                //Do nothing...
                break;
        }
    }

    public String getWizardStepTitle() {
        return null;
    }

    /**
     * Called whenever the wizard proceeds to the next step or goes back to the previous step
     */
    @Override
    public final void onExit(int exitCode) {
//        throw new UnsupportedOperationException("Use onExitSynchronous() instead");
//        switch (exitCode) {
//            case WizardStep.EXIT_NEXT:
//                Log.d(this.getClass().getSimpleName(), "onExit( NEXT> )");
//                this.onExitNext();
//                this.doPersistContextVariableFields();
//                break;
//            case WizardStep.EXIT_PREVIOUS:
//                Log.d(this.getClass().getSimpleName(), "onExit( <PREVIOUS )");
//                //Do nothing...
//                break;
//        }
    }

    @Override
    public final void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.d(this.getClass().getSimpleName(), "setUserVisibleHint(isVisibleToUser=" +
                isVisibleToUser + ")");
        if (getView() != null) {
            isViewShown = true;
            if (isVisibleToUser) {
                //FIXME Set wizard title here (rather than in Wizard#onClick)
                this.onVisibleToUser();
            }
        } else {
            isViewShown = false;
        }
    }

    protected abstract void onVisibleToUser();

    /**
     * Override this to enable persist context variable fields
     */
    protected void onExitNext() {
    }

    @SuppressWarnings("unchecked")
    private void doPersistContextVariableFields() {

        try {
            final SharedPreferences globalPrefs = getContext()
                    .getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                            Context.MODE_PRIVATE);

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
                        if (key != null && StringUtils.containsIgnoreCase(key, "password")) {
                            //Encrypt
                            wizardContextMap.put(key, Encrypted.e(value.toString()));
                        } else {
                            wizardContextMap.put(key, value);
                        }
                    }
                }
            }
            globalPrefs
                    .edit()
                    .putString(CURRENT_WIZARD_CONTEXT_PREF_KEY,
                            gson.toJson(wizardContextMap))
                    .apply();
        } catch (final Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }
    }
}
