package org.rm3l.ddwrt.mgmt.register;

import android.support.annotation.Nullable;

import org.codepond.wizardroid.WizardStep;
import org.rm3l.ddwrt.mgmt.register.steps.BasicDetailsStep;
import org.rm3l.ddwrt.utils.tuple.Pair;
import org.rm3l.ddwrt.widgets.MaterialWizard;

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

    @Nullable
    @Override
    protected List<Pair<Class<? extends WizardStep>, Boolean>> getStepClasses() {
        return Arrays.asList(
                Pair.<Class<? extends WizardStep>, Boolean> create(BasicDetailsStep.class, true)
        );
    }
}
