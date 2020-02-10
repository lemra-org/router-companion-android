package org.rm3l.router_companion.events.wizard;

/** Created by rm3l on 30/03/16. */
public class WizardStepVisibleToUserEvent extends WizardEvent {

  private final String stepTitle;

  public WizardStepVisibleToUserEvent(final String stepTitle) {
    this.stepTitle = stepTitle;
  }

  public String getStepTitle() {
    return stepTitle;
  }
}
