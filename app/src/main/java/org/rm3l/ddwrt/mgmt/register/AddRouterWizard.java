package org.rm3l.ddwrt.mgmt.register;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.codepond.wizardroid.WizardStep;
import org.rm3l.ddwrt.mgmt.register.steps.BasicDetailsStep;
import org.rm3l.ddwrt.mgmt.register.steps.LocalSSIDLookupStep;
import org.rm3l.ddwrt.mgmt.register.steps.ReviewStep;
import org.rm3l.ddwrt.mgmt.register.steps.RouterConnectionDetailsStep;
import org.rm3l.ddwrt.widgets.wizard.MaterialWizard;
import org.rm3l.ddwrt.widgets.wizard.WizardStepVerifiable;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rm3l on 14/03/16.
 */
public class AddRouterWizard extends MaterialWizard {

    /**
     * Note that initially MaterialWizard inherits from {@link android.support.v4.app.Fragment} and therefore you must have an empty constructor
     */
    public AddRouterWizard() {
        super();
    }

    @NonNull
    @Override
    protected String getWizardTitle() {
        return "Register a Router";
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
                (Class<T>) ReviewStep.class
        );
    }

}
