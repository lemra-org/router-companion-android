package org.rm3l.ddwrt.widgets;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.common.base.Strings;

import org.codepond.wizardroid.WizardFlow;
import org.codepond.wizardroid.WizardFragment;
import org.codepond.wizardroid.WizardStep;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.utils.tuple.Pair;

import java.util.List;

/**
 * Created by rm3l on 14/03/16.
 */
public abstract class MaterialWizard extends WizardFragment implements View.OnClickListener {

    private Button nextButton;
    private Button previousButton;

    //You must have an empty constructor according to Fragment documentation
    public MaterialWizard() {
    }

    /**
     * Binding the layout and setting buttons hooks
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View wizardLayout = inflater.inflate(R.layout.wizard, container, false);
        nextButton = (Button) wizardLayout.findViewById(R.id.wizard_next_button);
        if (!Strings.isNullOrEmpty(getNextButtonLabel())) {
            nextButton.setText(getNextButtonLabel());
        }
        nextButton.setOnClickListener(this);

        previousButton = (Button) wizardLayout.findViewById(R.id.wizard_previous_button);
        if (!Strings.isNullOrEmpty(getPreviousButtonLabel())) {
            previousButton.setText(getPreviousButtonLabel());
        }
        previousButton.setOnClickListener(this);

        return wizardLayout;
    }

    //You must override this method and create a wizard flow by
    //using WizardFlow.Builder as shown in this example
    @Override
    public final WizardFlow onSetup() {

        final WizardFlow.Builder wizardFlowBuilder = new WizardFlow.Builder();
        final List<Pair<Class<? extends WizardStep>, Boolean>> stepClasses = getStepClasses();
        if (stepClasses != null) {
            for (final Pair<Class<? extends WizardStep>, Boolean> stepClass : stepClasses) {
                if (stepClass == null || stepClass.first == null) {
                    continue;
                }
                wizardFlowBuilder.addStep(stepClass.first,
                        stepClass.second != null && stepClass.second);
            }
        }
        return wizardFlowBuilder.create();

    }

    /**
     * Triggered when the wizard is completed.
     * Overriding this method is optional.
     */
    @Override
    public final void onWizardComplete() {
        super.onWizardComplete();
//        //Do whatever you want to do once the Wizard is complete
//        //in this case I just close the activity, which causes Android
//        //to go back to the previous activity.
//        getActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.updateWizardControls();
    }

    @Override
    public final void onClick(View v) {
        switch(v.getId()) {
            case R.id.wizard_next_button:
                //Tell the wizard to go to next step
                wizard.goNext();
                break;
            case R.id.wizard_previous_button:
                //Tell the wizard to go back one step
                wizard.goBack();
                break;
        }
    }

    @Override
    public void onStepChanged() {
        super.onStepChanged();
        this.updateWizardControls();
    }

    /**
     * Updates the UI according to current step position
     */
    private void updateWizardControls() {
        this.previousButton.setEnabled(!this.wizard.isFirstStep());
        if (!Strings.isNullOrEmpty(this.getPreviousButtonLabel())) {
            this.previousButton.setText(this.getPreviousButtonLabel());
        }
        this.nextButton.setEnabled(this.wizard.canGoNext());
        if (this.wizard.isLastStep()) {
            if (!Strings.isNullOrEmpty(this.getFinishButtonLabel())) {
                this.nextButton.setText(this.getFinishButtonLabel());
            } else {
                this.nextButton.setText(R.string.action_finish);
            }
        } else {
            if (!Strings.isNullOrEmpty(this.getNextButtonLabel())) {
                this.nextButton.setText(this.getNextButtonLabel());
            } else {
                this.nextButton.setText(R.string.action_next);
            }
        }
    }

    @Nullable
    protected abstract List<Pair<Class<? extends WizardStep>, Boolean>> getStepClasses();


    //Override to define custom labels

    @Nullable
    protected String getPreviousButtonLabel() {
        return null;
    }

    @Nullable
    protected String getNextButtonLabel() {
        return null;
    }

    @Nullable
    protected String getFinishButtonLabel() {
        return null;
    }

}
