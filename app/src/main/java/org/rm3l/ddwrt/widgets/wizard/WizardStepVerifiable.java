package org.rm3l.ddwrt.widgets.wizard;

import android.support.annotation.Nullable;

import org.codepond.wizardroid.Wizard;

/**
 * Created by rm3l on 26/03/16.
 */
public interface WizardStepVerifiable {

    @Nullable
    Boolean validateStep(@Nullable final Wizard wizard);

}