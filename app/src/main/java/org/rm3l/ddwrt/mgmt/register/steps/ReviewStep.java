package org.rm3l.ddwrt.mgmt.register.steps;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Splitter;

import org.codepond.wizardroid.WizardStep;
import org.codepond.wizardroid.persistence.ContextVariable;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.widgets.wizard.WizardStepVerifiable;

import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Created by rm3l on 21/03/16.
 */
public class ReviewStep extends WizardStep implements WizardStepVerifiable {

    @ContextVariable
    private String uuid;

    @ContextVariable
    private String routerName;

    @ContextVariable
    private String routerIpOrDns;

    @ContextVariable
    private String routerFirmware;

    @ContextVariable
    private String connectionProtocol;

    @ContextVariable
    private String port;

    @ContextVariable
    private String username;

    @ContextVariable
    private int checkedAuthMethodRadioButtonId;

    @ContextVariable
    private String password;

    @ContextVariable
    private String privkeyButtonHint;

    @ContextVariable
    private String privkeyErrorMsg;

    @ContextVariable
    private String privkeyPath;

    @ContextVariable
    private boolean fallBackToPrimary;

    @ContextVariable
    private boolean useLocalSSIDLookup;

    @ContextVariable
    private String localSSIDLookupDetails; //JSON

    private Router router;

    private DDWRTCompanionDAO dao;

    //Wire the layout to the step
    public ReviewStep() {
    }

    //Set your layout here
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View v = inflater.inflate(
                R.layout.wizard_add_router_4_review,
                container, false);

        this.dao = RouterManagementActivity.getDao(getContext());
        //TODO

        //Validate form and check connection
//        notifyCompleted();
//        boolean validForm = validateForm(d);

//        if (validForm) {
//            // Now check actual connection to router ...
//            new CheckRouterConnectionAsyncTask(
//                    routerIpOrDns,
//                    getActivity().getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
//                            .getBoolean(ALWAYS_CHECK_CONNECTION_PREF_KEY, true))
//                    .execute(d);
//        }

        return v;
    }

    private Router buildRouter()  {
        final Router router = new Router(getContext());
        if (!isNullOrEmpty(uuid)) {
            router.setUuid(uuid);
        } else {
            router.setUuid(UUID.randomUUID().toString());
        }
        router.setName(routerName);
        router.setRemoteIpAddress(routerIpOrDns);
        router.setRemotePort(port != null ? Integer.parseInt(port) : 22);
        router.setRouterConnectionProtocol(
                connectionProtocol != null ?
                        Router.RouterConnectionProtocol.valueOf(connectionProtocol) :
                        Router.RouterConnectionProtocol.SSH);
//        final int pos = (((Spinner) d.findViewById(R.id.router_add_firmware))).getSelectedItemPosition();
//        final String[] fwStringArray = d.getContext().getResources().getStringArray(R.array.router_firmwares_array_values);
//        if (fwStringArray != null && pos < fwStringArray.length) {
//            final String fwSelection = fwStringArray[pos];
//            if (!"auto".equals(fwSelection)) {
//                router.setRouterFirmware(fwSelection);
//            } // else we will try to guess
//        } // else we will try to guess

        router.setUsername(username, true);
//        router.setStrictHostKeyChecking(((CheckBox) d.findViewById(R.id.router_add_is_strict_host_key_checking)).isChecked());

//        final String password = ((EditText) d.findViewById(R.id.router_add_password)).getText().toString();
//        final String privkey = ((TextView) d.findViewById(R.id.router_add_privkey_path)).getText().toString();
        if (!isNullOrEmpty(password)) {
            router.setPassword(password, true);
        }
        if (!isNullOrEmpty(privkeyPath)) {

//            //Convert privkey into a format accepted by JSCh
            //Causes a build issue with SpongyCastle
//            final PEMParser pemParser = new PEMParser(new StringReader(privkey));
//            Object object = pemParser.readObject();
//            PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(nullToEmpty(password).toCharArray());
//            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("SC");
//            KeyPair kp;
//            if (object instanceof PEMEncryptedKeyPair) {
//                Crashlytics.log(Log.DEBUG, LOG_TAG, "Encrypted key - we will use provided password");
//                kp = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
//            } else {
//                Crashlytics.log(Log.DEBUG, LOG_TAG, "Unencrypted key - no password needed");
//                kp = converter.getKeyPair((PEMKeyPair) object);
//            }
//            final PrivateKey privateKey = \"fake-key\";
//            StringWriter stringWriter = new StringWriter();
//            JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter);
//            pemWriter.writeObject(privateKey);
//            pemWriter.close();

            router.setPrivKey(privkeyPath, true);
        }

        final FragmentActivity activity = getActivity();
        router.setUseLocalSSIDLookup(activity,
                useLocalSSIDLookup);
        router.setFallbackToPrimaryAddr(activity,
                fallBackToPrimary);

        final Splitter splitter = Splitter.on("\n").omitEmptyStrings();

        //Now build SSID data
//        final LinearLayout container = (LinearLayout) d.findViewById(R.id.router_add_local_ssid_container);
//        final int childCount = container.getChildCount();
//        final List<Router.LocalSSIDLookup> lookups = new ArrayList<>();
//        for (int i = 0; i < childCount; i++){
//            final View view = container.getChildAt(i);
//            if (!(view instanceof TextView)) {
//                continue;
//            }
//            final String textViewString = ((TextView) view).getText().toString();
//            final List<String> strings = splitter.splitToList(textViewString);
//            if (strings.size() < 3) {
//                continue;
//            }
//            final Router.LocalSSIDLookup localSSIDLookup = new Router.LocalSSIDLookup();
//            localSSIDLookup.setNetworkSsid(strings.get(0));
//            localSSIDLookup.setReachableAddr(strings.get(1));
//            try {
//                localSSIDLookup.setPort(Integer.parseInt(strings.get(2)));
//            } catch (final Exception e) {
//                ReportingUtils.reportException(null, e);
//                localSSIDLookup.setPort(22); //default SSH port
//            }
//            lookups.add(localSSIDLookup);
//        }
//        router.setLocalSSIDLookupData(activity, lookups);

        return router;
    }

    /**
     * Called whenever the wizard proceeds to the next step or goes back to the previous step
     */

    @Override
    public void onExit(int exitCode) {
        switch (exitCode) {
            case WizardStep.EXIT_NEXT: {
                this.dao.insertRouter(router);
            }
                break;
            case WizardStep.EXIT_PREVIOUS:
                //Do nothing...
                break;
        }
    }

    @Override
    public boolean validateStep() {
        router = buildRouter();
        return (router != null);
    }
}
