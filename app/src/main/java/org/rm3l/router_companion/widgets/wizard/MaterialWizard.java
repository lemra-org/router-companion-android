package org.rm3l.router_companion.widgets.wizard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.codepond.wizardroid.WizardFlow;
import org.codepond.wizardroid.WizardFragment;
import org.codepond.wizardroid.WizardStep;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.events.bus.BusSingleton;
import org.rm3l.router_companion.events.wizard.WizardStepVisibleToUserEvent;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.mgmt.register.resources.RouterWizardAction;
import org.rm3l.router_companion.utils.ColorUtils;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.widgets.ViewPagerWithAllowedSwipeDirection;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static org.rm3l.router_companion.mgmt.register.resources.RouterWizardAction.ROUTER_WIZARD_ACTION;

/**
 * Created by rm3l on 14/03/16.
 */
public abstract class MaterialWizard extends WizardFragment implements View.OnClickListener, ViewPager.OnPageChangeListener {

    private static final String LOG_TAG = MaterialWizard.class.getSimpleName();

    public static final GsonBuilder GSON_BUILDER = new GsonBuilder();
    public static final String CURRENT_WIZARD_CONTEXT_PREF_KEY = \"fake-key\";

    private ViewPagerWithAllowedSwipeDirection mPager;

    private Button nextButton;
    private Button previousButton;
//    private Button cancelButton;

    private CollapsingToolbarLayout collapsingToolbarLayout;
    private TextView wizardSubTitle;

    private LinearLayout pager_indicator;
    private int dotsCount;
    private ImageView[] dots;

    //You must have an empty constructor according to Fragment documentation
    public MaterialWizard() {
    }

    @NonNull
    protected abstract String getWizardTitle();

    @Nullable
    protected String getFirstStepWizardSubTitle() {
        return null;
    }

    /**
     * Binding the layout and setting buttons hooks
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Context context = getContext();

        final View mWizardLayout = inflater.inflate(R.layout.wizard, container, false);

        wizardSubTitle = (TextView) mWizardLayout.findViewById(R.id.wizard_subtitle);

        pager_indicator = (LinearLayout) mWizardLayout.findViewById(R.id.viewPagerCountDots);

        collapsingToolbarLayout = (CollapsingToolbarLayout) mWizardLayout.findViewById(R.id.htab_collapse_toolbar);
        collapsingToolbarLayout.setTitleEnabled(true);
        collapsingToolbarLayout.setCollapsedTitleTextColor(
                ContextCompat.getColor(getContext(), R.color.white));
        collapsingToolbarLayout.setExpandedTitleColor(
                ContextCompat.getColor(getContext(), R.color.white));
        String wizardTitle = getWizardTitle();
        if (TextUtils.isEmpty(wizardTitle)) {
            wizardTitle = "Wizard";
        }
        collapsingToolbarLayout.setTitle(wizardTitle);

        final Toolbar toolbar = (Toolbar) mWizardLayout.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        toolbar.setNavigationContentDescription("Close");
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeWizard(RESULT_CANCELED);
            }
        });
        toolbar.setPopupTheme(ColorUtils.isThemeLight(getContext()) ?
                R.style.PopupThemeLight : R.style.PopupTheme);
        toolbar.inflateMenu(R.menu.menu_material_wizard);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_feedback:
                        Utils.openFeedbackForm(MaterialWizard.this.getActivity(), "");
                        return true;

                    default:
                        break;
                }
                return false;
            }
        });

        wizardSubTitle.setText(getFirstStepWizardSubTitle());

        mPager = (ViewPagerWithAllowedSwipeDirection) mWizardLayout.findViewById(R.id.step_container);
        mPager.setAllowedSwipeDirection(ViewPagerWithAllowedSwipeDirection.SwipeDirection.NONE);
        mPager.setOffscreenPageLimit(1);

        nextButton = (Button) mWizardLayout.findViewById(R.id.wizard_next_button);
        if (!Strings.isNullOrEmpty(getNextButtonLabel())) {
            nextButton.setText(getNextButtonLabel());
        }
        nextButton.setOnClickListener(this);

        previousButton = (Button) mWizardLayout.findViewById(R.id.wizard_previous_button);
//        if (!Strings.isNullOrEmpty(getPreviousButtonLabel())) {
//            previousButton.setText(getPreviousButtonLabel());
//        }
        previousButton.setOnClickListener(this);

        mPager.addOnPageChangeListener(this);

        final Bundle arguments = getArguments();
        if (arguments != null) {
            final String routerUuid = arguments.getString(RouterManagementActivity.ROUTER_SELECTED);
            final int routerWizardAction = arguments.getInt(ROUTER_WIZARD_ACTION, RouterWizardAction.ADD);
            Crashlytics.log(routerUuid);
            mPager.setTag(RouterWizardAction.GSON_BUILDER.create().toJson(new RouterWizardAction()
                    .setAction(routerWizardAction).setRouterUuid(routerUuid)));
        }

//        cancelButton = (Button) mWizardLayout.findViewById(R.id.wizard_cancel_button);
//        cancelButton.setOnClickListener(this);
//        this.cancelButton.setEnabled(true);

        return mWizardLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setUiPageViewController();
    }

    private void setUiPageViewController() {

        final PagerAdapter adapter;
        if (mPager == null || (adapter = mPager.getAdapter()) == null) {
            Crashlytics.log(Log.WARN, LOG_TAG, "No pager or pager adapter => no dots in wizard!");
            return;
        }
        dotsCount = adapter.getCount();
        dots = new ImageView[dotsCount];

        final Context context = getContext();

        for (int i = 0; i < dotsCount; i++) {
            dots[i] = new ImageView(context);
            dots[i].setImageDrawable(ContextCompat.getDrawable(context,
                    R.drawable.nonselecteditem_dot));

            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            params.setMargins(4, 0, 4, 0);

            pager_indicator.addView(dots[i], params);
        }

        dots[0].setImageDrawable(ContextCompat.getDrawable(context,
                R.drawable.selecteditem_dot));
    }

    @Override
    public void onStart() {
        super.onStart();
        final Bus busInstance = BusSingleton.getBusInstance();
        Crashlytics.log(Log.DEBUG, LOG_TAG, "onStart: Register on bus (" + busInstance + ")");
        busInstance.register(this);
    }

    @Override
    public void onStop() {
        final Bus busInstance = BusSingleton.getBusInstance();
        Crashlytics.log(Log.DEBUG, LOG_TAG, "onStop: Unregister from bus (" + busInstance + ")");
        busInstance.unregister(this);
        super.onStop();
    }

    //You must override this method and create a wizard flow by
    //using WizardFlow.Builder as shown in this example
    @SuppressWarnings("unchecked")
    @Override
    public final WizardFlow onSetup() {

        final WizardFlow.Builder wizardFlowBuilder = new WizardFlow.Builder();
        final List<?> stepClasses = getStepClasses();
        if (stepClasses != null) {
            for (final Object stepClass : stepClasses) {
                if (stepClass == null ||
                        !Class.class.isAssignableFrom(stepClass.getClass())) {
                    continue;
                }
                wizardFlowBuilder.addStep((Class<? extends WizardStep>) stepClass, false);
            }
        }
        return wizardFlowBuilder.create();

    }

    public static Map getWizardContext(@Nullable final Context context) {
        if (context == null) {
            return Collections.emptyMap();
        }

        final SharedPreferences globalPrefs = context
                .getSharedPreferences(RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                        Context.MODE_PRIVATE);

        final String wizardContextJson = globalPrefs.getString(CURRENT_WIZARD_CONTEXT_PREF_KEY,
                "{}");
        final Gson gson = GSON_BUILDER.create();
        final Map wizardContextMap = gson.fromJson(wizardContextJson, Map.class);
        return Collections.unmodifiableMap(wizardContextMap);
    }

    /**
     * Triggered when the wizard is completed.
     * Overriding this method is optional.
     */
    @Override
    public void onWizardComplete() {
        super.onWizardComplete();
        final SharedPreferences globalPrefs = this.getContext()
                .getSharedPreferences(RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                        Context.MODE_PRIVATE);
        globalPrefs.edit().remove(CURRENT_WIZARD_CONTEXT_PREF_KEY).apply();
//        //Do whatever you want to do once the Wizard is complete
//        //in this case I just close the activity, which causes Android
//        //to go back to the previous activity.
        closeWizard(RESULT_OK);
    }

    private void closeWizard(Integer resultCode) {
        final FragmentActivity activity = getActivity();
        if (resultCode != null) {
            final Intent data = getActivityIntentToReturnUponClose();
            if (data == null) {
                activity.setResult(resultCode);
            } else {
                activity.setResult(resultCode, data);
            }
        }
        activity.finish();
    }

    @Nullable
    protected Intent getActivityIntentToReturnUponClose() {
        return new Intent();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.updateWizardControls();
    }

    @Override
    public final void onClick(View v) {
        Crashlytics.log("onclick");
        final MaterialWizardStep currentStep = (MaterialWizardStep) wizard.getCurrentStep();

        switch(v.getId()) {
            case R.id.wizard_next_button:
                //Tell the wizard to go to next step
                final Boolean stepValidated = currentStep.validateStep(wizard);
                Crashlytics.log("stepValidated: " + stepValidated);
                if (stepValidated != null && stepValidated) {
                    currentStep.onExitSynchronous(WizardStep.EXIT_NEXT);
                    wizard.goNext();
                }
                //Maybe provide a 'denial' animation
                break;
            case R.id.wizard_previous_button:
                //Tell the wizard to go back one step
                currentStep.onExitSynchronous(WizardStep.EXIT_PREVIOUS);
                wizard.goBack();
                break;
//            case R.id.wizard_cancel_button:
//                final SharedPreferences globalPrefs = this.getContext()
//                        .getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
//                                Context.MODE_PRIVATE);
//                globalPrefs.edit().remove(CURRENT_WIZARD_CONTEXT_PREF_KEY).apply();
//                //Close wizard
//                getActivity().finish();
//                break;
        }
    }

    @Subscribe
    public void wizardStepVisibleToUser(final WizardStepVisibleToUserEvent event) {
        final String eventStepTitle = event.getStepTitle();
        Crashlytics.log(Log.DEBUG, LOG_TAG, "wizardStepVisibleToUser(" + eventStepTitle + ")");
        wizardSubTitle.setText(eventStepTitle);
    }

    @Override
    public void onStepChanged() {
        super.onStepChanged();
        final MaterialWizardStep currentStep = (MaterialWizardStep) wizard.getCurrentStep();
        wizardSubTitle.setText(currentStep.getWizardStepTitle());
        this.updateWizardControls();
    }

    /**
     * Updates the UI according to current step position
     */
    private void updateWizardControls() {
//        this.cancelButton.setEnabled(true);

        final boolean previousButtonEnabled = !this.wizard.isFirstStep();
        this.previousButton.setEnabled(previousButtonEnabled);
        this.previousButton.setVisibility(previousButtonEnabled ?
            View.VISIBLE : View.INVISIBLE);

        if (!Strings.isNullOrEmpty(this.getPreviousButtonLabel())) {
            this.previousButton.setText(this.getPreviousButtonLabel());
        } else {
            this.previousButton.setText(R.string.wizard_previous);
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
                this.nextButton.setText(R.string.wizard_next);
            }
        }
    }

    @Nullable
    protected abstract <T extends WizardStep & WizardStepVerifiable> List<Class<T>> getStepClasses();


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

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if (position < 0 || position >= dotsCount) {
            Crashlytics.log(Log.WARN, LOG_TAG,
                    "Invalid position: " + position + " - max dots length is " + dotsCount);
            return;
        }
        final Context context = getContext();
        for (int i = 0; i < dotsCount; i++) {
            dots[i].setImageDrawable(
                    ContextCompat.getDrawable(context,
                            (position != i) ?
                                    R.drawable.nonselecteditem_dot : R.drawable.selecteditem_dot));
        }
//        dots[position].setImageDrawable(ContextCompat.getDrawable(context,
//                R.drawable.selecteditem_dot));
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
