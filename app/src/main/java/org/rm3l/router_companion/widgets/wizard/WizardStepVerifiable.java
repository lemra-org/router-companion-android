package org.rm3l.router_companion.widgets.wizard;

import androidx.annotation.Nullable;
import org.codepond.wizardroid.Wizard;

/**
 * Created by rm3l on 26/03/16.
 */
public interface WizardStepVerifiable {

    @Nullable
    Boolean validateStep(@Nullable final Wizard wizard);
}
