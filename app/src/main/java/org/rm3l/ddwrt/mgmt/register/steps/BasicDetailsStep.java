package org.rm3l.ddwrt.mgmt.register.steps;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.codepond.wizardroid.WizardStep;
import org.codepond.wizardroid.persistence.ContextVariable;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.utils.Utils;
import org.rm3l.ddwrt.utils.ViewGroupUtils;
import org.rm3l.ddwrt.widgets.wizard.WizardStepVerifiable;

import static org.rm3l.ddwrt.utils.Utils.isDemoRouter;

/**
 * Created by rm3l on 15/03/16.
 */
public class BasicDetailsStep extends WizardStep implements WizardStepVerifiable {

    /**
     * Tell WizarDroid that these are context variables.
     * These values will be automatically bound to any field annotated with {@link ContextVariable}.
     * NOTE: Context Variable names are unique and therefore must
     * have the same name wherever you wish to use them.
     */
    @ContextVariable
    private String uuid;

    @ContextVariable
    private String routerName;

    @ContextVariable
    private String routerIpOrDns;

    @ContextVariable
    private String routerFirmware;


    private TextView uuidTv;

    private EditText routerNameEt;

    private EditText routerIpOrDnsEt;
    private TextInputLayout routerIpTil;

    private Spinner routerFirmwareSpinner;

    //Wire the layout to the step
    public BasicDetailsStep() {
    }

    //Set your layout here
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.wizard_add_router_1_basic_details_step, container, false);
        uuidTv = (TextView) v.findViewById(R.id.router_add_uuid);
        routerNameEt = (EditText) v.findViewById(R.id.router_add_name);

        routerIpOrDnsEt = (EditText) v.findViewById(R.id.router_add_ip);
        routerIpTil = (TextInputLayout)
                v.findViewById(R.id.router_add_ip_input_layout);

        final TextView demoText = (TextView) v.findViewById(R.id.router_add_ip_demo_text);
        demoText.setText(demoText.getText().toString()
                .replace("%PACKAGE_NAME%", BuildConfig.APPLICATION_ID));

        //and set default values by using Context Variables
        uuidTv.setText(uuid);
        routerNameEt.setText(routerName);
        if (routerIpOrDns != null) {
            routerIpOrDnsEt.setText(routerIpOrDns);
        } else {
            if (TextUtils.isEmpty(routerIpOrDnsEt.getText())) {
                //Do this only if nothing has been filled in the EditText by the user
                final WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null) {
                    final DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                    if (dhcpInfo != null) {
                        routerIpOrDnsEt.setText(Utils.intToIp(dhcpInfo.gateway));
                    }
                }
            }
        }

        routerFirmwareSpinner = (Spinner) v.findViewById(R.id.router_add_firmware);
        if (routerFirmware != null) {
            routerFirmwareSpinner.setSelection(
                    ViewGroupUtils.getSpinnerIndex(routerFirmwareSpinner, routerFirmware), true);
        }

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
        uuid = uuidTv.getText().toString();
        routerName = routerNameEt.getText().toString();
        routerIpOrDns = routerIpOrDnsEt.getText().toString();
        routerFirmware = routerFirmwareSpinner.getSelectedItem().toString();
    }

    @Override
    public boolean validateStep() {
        final String routerReachableAddr = routerIpOrDnsEt.getText().toString();
        final boolean stepValidated;
        if (isDemoRouter(routerReachableAddr)
                || Patterns.IP_ADDRESS.matcher(routerReachableAddr).matches()
                || Patterns.DOMAIN_NAME.matcher(routerReachableAddr).matches()) {
            routerIpTil.setErrorEnabled(false);
            stepValidated = true;
        } else {
            routerIpTil.setErrorEnabled(true);
            routerIpTil.setError(getString(R.string.router_add_dns_or_ip_invalid));
            stepValidated = false;
        }
        return stepValidated;
    }
}
