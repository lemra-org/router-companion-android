package org.rm3l.ddwrt.mgmt.register.steps;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import org.codepond.wizardroid.WizardStep;
import org.codepond.wizardroid.persistence.ContextVariable;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.utils.ViewGroupUtils;

import java.util.List;

import static android.widget.TextView.BufferType.EDITABLE;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Created by rm3l on 21/03/16.
 */
public class LocalSSIDLookupStep extends WizardStep {

    @ContextVariable
    private String port;

    @ContextVariable
    private boolean fallBackToPrimary;

    @ContextVariable
    private boolean useLocalSSIDLookup;

    @ContextVariable
    private String localSSIDLookupDetails; //JSON

    private CheckBox fallBackToPrimaryCb;

    private CheckBox useLocalSSIDLookupCb;

    private LinearLayout localSSIDLookupDetailedView;

    //Wire the layout to the step
    public LocalSSIDLookupStep() {
    }

    //Set your layout here
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        final View v = inflater.inflate(
                R.layout.wizard_add_router_3_advanced_options_1_local_ssid_lookup_step,
                container, false);

//        v.findViewById(R.id.router_add_local_ssid_skip_button)
//                .setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        //TODO Move to next section right away
//                    }
//                });

        fallBackToPrimaryCb = (CheckBox) v.findViewById(R.id.router_add_fallback_to_primary);
        useLocalSSIDLookupCb = (CheckBox) v.findViewById(R.id.router_add_local_ssid_lookup);

        fallBackToPrimaryCb.setChecked(fallBackToPrimary);

        final RelativeLayout localSSIDLookupDetail = (RelativeLayout)
                v.findViewById(R.id.router_add_local_ssid_section);

        localSSIDLookupDetailedView = (LinearLayout)
                v.findViewById(R.id.router_add_local_ssid_container);
        if (!TextUtils.isEmpty(localSSIDLookupDetails)) {
            //TODO Restore state from @ContextVariable
        }

        useLocalSSIDLookupCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                localSSIDLookupDetail.setVisibility(isChecked ?
                    View.VISIBLE : View.INVISIBLE);
                fallBackToPrimaryCb.setChecked(!isChecked);
            }
        });

        useLocalSSIDLookupCb.setChecked(useLocalSSIDLookup);

        final FragmentActivity activity = getActivity();

        v.findViewById(R.id.router_add_local_ssid_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                            //Download the full version to unlock this version
                            Utils.displayUpgradeMessage(activity, "Add Alternate Addresses");
                            return;
                        }

                        final AlertDialog.Builder addLocalSsidLookupDialogBuilder = new AlertDialog.Builder(activity);
                        final View addLocalSsidLookupDialogView = inflater.inflate(R.layout.activity_router_add_local_ssid_lookup, null);

                        final WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                        final TextInputLayout ssidTil = (TextInputLayout)
                                addLocalSsidLookupDialogView.findViewById(R.id.router_add_local_ssid_lookup_ssid_til);
                        final AutoCompleteTextView ssidAutoCompleteView = (AutoCompleteTextView)
                                addLocalSsidLookupDialogView.findViewById(R.id.router_add_local_ssid_lookup_ssid);

                        try {
                            if (wifiManager != null) {
                                final List<ScanResult> results = wifiManager.getScanResults();
                                ssidAutoCompleteView
                                        .setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1,
                                                FluentIterable.from(results)
                                                        .transform(new Function<ScanResult, String>() {
                                                            @Override
                                                            public String apply(@Nullable ScanResult input) {
                                                                if (input == null) {
                                                                    return null;
                                                                }
                                                                return input.SSID;
                                                            }
                                                        }).toArray(String.class)));
                            }
                            //Fill with current network SSID
                            String wifiName = Utils.getWifiName(activity);
                            if (wifiName != null && wifiName.startsWith("\"") && wifiName.endsWith("\"")) {
                                wifiName = wifiName.substring(1, wifiName.length() - 1);
                            }
                            ssidAutoCompleteView.setText(wifiName, EDITABLE);
                        } catch (final Exception e) {
                            e.printStackTrace();
                            //No worries
                        }

                        final TextInputLayout ipTil = (TextInputLayout) addLocalSsidLookupDialogView.findViewById(R.id.router_add_local_ssid_lookup_ip_til);
                        final EditText ipEditText = (EditText) addLocalSsidLookupDialogView.findViewById(R.id.router_add_local_ssid_lookup_ip);
                        //Fill with network gateway IP
                        try {
                            if (wifiManager != null) {
                                final DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                                if (dhcpInfo != null) {
                                    ipEditText.setText(Utils.intToIp(dhcpInfo.gateway), EDITABLE);
                                }
                            }
                        } catch (final Exception e) {
                            e.printStackTrace();
                            //No worries
                        }

                        final TextInputLayout portTil = (TextInputLayout) addLocalSsidLookupDialogView.findViewById(R.id.router_add_local_ssid_lookup_port_til);
                        final EditText portEditText = (EditText) addLocalSsidLookupDialogView.findViewById(R.id.router_add_local_ssid_lookup_port);
                        portEditText.setText(port, EDITABLE);

                        final AlertDialog addLocalSsidLookupDialog = addLocalSsidLookupDialogBuilder
                                .setTitle("Add Local SSID Lookup")
                                .setMessage("This allows you to define an alternate IP or DNS name to use for this router, " +
                                        "when connected to a network with the specified name.\n" +
                                        "For example, you may want to set a local IP address when connected to your home network, " +
                                        "and by default use an external DNS name. " +
                                        "This would speed up router data retrieval from the app when at home.")
                                .setView(addLocalSsidLookupDialogView)
                                .setCancelable(true)
                                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //Do nothing here because we override this button later to change the close behaviour.
                                        //However, we still need this because on older versions of Android unless we
                                        //pass a handler the button doesn't get instantiated
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //Cancelled - nothing more to do!
                                    }
                                })
                                .create();

                        addLocalSsidLookupDialog.show();

                        addLocalSsidLookupDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                final String ssidText = ssidAutoCompleteView.getText().toString();
                                //Validate fields
                                if (isNullOrEmpty(ssidText)) {
                                    //displayMessage and prevent exiting
                                    ssidTil.setErrorEnabled(true);
                                    ssidTil.setError("Invalid Network Name");
                                    ssidAutoCompleteView.requestFocus();
                                    Utils.openKeyboard(activity, ssidAutoCompleteView);
                                    return;
                                }
                                ssidTil.setErrorEnabled(false);

                                final String ipEditTextText = ipEditText.getText().toString();
                                if (!(Patterns.IP_ADDRESS.matcher(ipEditTextText).matches()
                                        || Patterns.DOMAIN_NAME.matcher(ipEditTextText).matches())) {
                                    //displayMessage and prevent exiting
                                    ipTil.setErrorEnabled(true);
                                    ipTil.setError("Invalid IP or DNS Name");
                                    ipEditText.requestFocus();
                                    Utils.openKeyboard(activity, ipEditText);
                                    return;
                                }
                                ipTil.setErrorEnabled(false);

                                boolean validPort;
                                final String portStr = portEditText.getText().toString();
                                try {
                                    validPort = (!isNullOrEmpty(portStr) && (Integer.parseInt(portStr) > 0));
                                } catch (@NonNull final Exception e) {
                                    e.printStackTrace();
                                    validPort = false;
                                }
                                if (!validPort) {
                                    portTil.setErrorEnabled(true);
                                    portTil.setError("Invalid Port");
                                    portEditText.requestFocus();
                                    Utils.openKeyboard(activity, portEditText);
                                    return;
                                }
                                portTil.setErrorEnabled(false);

                                final TextView localSsidView = new TextView(activity);
                                localSsidView.setText(ssidText + "\n" +
                                        ipEditTextText + "\n" + portStr
                                );
                                localSsidView.setLayoutParams(new ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT));
                                localSsidView
                                        .setCompoundDrawablesWithIntrinsicBounds(
                                                0, 0, android.R.drawable.ic_menu_close_clear_cancel, 0);

                                localSsidView.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View v, MotionEvent event) {
                                        final int DRAWABLE_RIGHT = 2;

                                        if (event.getAction() == MotionEvent.ACTION_UP) {
                                            if (event.getRawX() >= (localSsidView.getRight() -
                                                    localSsidView.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                                                //Remove view from container layout
                                                final ViewParent parent = localSsidView.getParent();
                                                if (parent instanceof LinearLayout) {
                                                    ((LinearLayout)parent).removeView(localSsidView);
                                                }
                                            }
                                        }
                                        return true;
                                    }
                                });

                                localSSIDLookupDetailedView.addView(localSsidView);
                                final View lineView = Utils.getLineView(activity);
                                if (lineView != null) {
                                    localSSIDLookupDetailedView.addView(lineView);
                                }

                                addLocalSsidLookupDialog.dismiss();
                            }
                        });
                    }
                });

        return v;
    }

    /**
     * Called whenever the wizard proceeds to the next step or goes back to the previous step
     */

    @Override
    public void onExit(int exitCode) {
        switch (exitCode) {
            case WizardStep.EXIT_NEXT:
                bindDataFields();
                break;
            case WizardStep.EXIT_PREVIOUS:
                //Do nothing...
                break;
        }
    }

    private void bindDataFields() {
        //The values of these fields will be automatically stored in the wizard context
        //and will be populated in the next steps only if the same field names are used.
        useLocalSSIDLookup = useLocalSSIDLookupCb.isChecked();
        fallBackToPrimary = fallBackToPrimaryCb.isChecked();

        final List<View> children = ViewGroupUtils
                .getLinearLayoutChildren(localSSIDLookupDetailedView);
        //TODO

    }
}
