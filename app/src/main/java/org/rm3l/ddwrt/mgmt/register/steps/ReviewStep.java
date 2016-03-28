package org.rm3l.ddwrt.mgmt.register.steps;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.common.base.Splitter;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.Encrypted;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.widgets.wizard.MaterialWizard;
import org.rm3l.ddwrt.widgets.wizard.MaterialWizardStep;

import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Created by rm3l on 21/03/16.
 */
public class ReviewStep extends MaterialWizardStep {

    private String uuid;
    private TextView uuidView;

    private String routerName;
    private TextView routerNameView;

    private String routerIpOrDns;
    private TextView routerIpOrDnsView;

    private String routerFirmware;
    private TextView routerFirmwareView;

    private String connectionProtocol;
    private TextView connectionProtocolView;

    private String port;
    private TextView portView;

    private String username;
    private TextView usernameView;

    private Integer authMethod;
    private RadioGroup authMethodRg;

    private String password;
    private EditText passwordView;

    private String privkeyButtonHint;
    private TextView privkeyButtonHintView;

    private String privkeyPath;
    private TextView privkeyPathView;

    private boolean fallBackToPrimary;
    private TextView fallBackToPrimaryView;

    private boolean useLocalSSIDLookup;
    private TextView useLocalSSIDLookupView;

    private String localSSIDLookupDetails; //JSON
    private TextView localSSIDLookupDetailsView;

    private Router router;

    private DDWRTCompanionDAO dao;

    //Wire the layout to the step
    public ReviewStep() {
    }

    //Set your layout here
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Context context = getContext();
        this.dao = RouterManagementActivity.getDao(context);
        
        final View v = inflater.inflate(
                R.layout.wizard_add_router_4_review,
                container, false);

        uuidView = (TextView) v.findViewById(R.id.wizard_add_router_review_router_uuid);
        routerNameView = (TextView) v.findViewById(R.id.wizard_add_router_review_router_name);
        routerIpOrDnsView = (TextView) v.findViewById(R.id.wizard_add_router_review_router_ip_dns);
        routerFirmwareView = (TextView) v.findViewById(R.id.wizard_add_router_review_router_firmware);
        connectionProtocolView = (TextView) v.findViewById(R.id.wizard_add_router_review_router_conn_proto);
        portView = (TextView) v.findViewById(R.id.wizard_add_router_review_router_conn_proto_ssh_port);
        usernameView = (TextView) v.findViewById(R.id.wizard_add_router_review_router_conn_proto_ssh_username);
        authMethodRg = (RadioGroup) v.findViewById(R.id.wizard_add_router_review_ssh_auth_method);
        privkeyButtonHintView = (TextView) v.findViewById(R.id.wizard_add_router_review_ssh_auth_method_privkey_path);
        passwordView = (EditText) v.findViewById(R.id.wizard_add_router_review_ssh_auth_method_password_value);

        final CheckBox showPasswordCheckBox = (CheckBox) v.findViewById(R.id.wizard_add_router_review_password_show_checkbox);
        showPasswordCheckBox
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            passwordView.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        } else {
                            passwordView.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        }
                        passwordView.setEnabled(false);
                    }
                });
        authMethodRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                final TextView privkeyHdrView = (TextView)
                        v.findViewById(R.id.wizard_add_router_review_ssh_auth_method_privkey_hdr);
                final TextView passwordHdrView = (TextView)
                        v.findViewById(R.id.wizard_add_router_review_ssh_auth_method_password_hdr);
                switch (checkedId) {
                    case R.id.router_add_ssh_auth_method_none:
                        privkeyHdrView.setVisibility(View.GONE);
                        privkeyButtonHintView.setText(null);
                        privkeyButtonHintView.setVisibility(View.GONE);

                        passwordHdrView.setVisibility(View.GONE);
                        passwordView.setText(null);
                        showPasswordCheckBox.setVisibility(View.GONE);
                        break;
                    case R.id.router_add_ssh_auth_method_password:
                        privkeyHdrView.setVisibility(View.GONE);
                        privkeyButtonHintView.setText(null);
                        privkeyButtonHintView.setVisibility(View.GONE);

                        passwordHdrView.setVisibility(View.VISIBLE);
                        passwordView.setText(password);
                        showPasswordCheckBox.setVisibility(View.VISIBLE);
                        break;
                    case R.id.router_add_ssh_auth_method_privkey:
                        privkeyHdrView.setVisibility(View.VISIBLE);
                        privkeyButtonHintView.setText(privkeyButtonHint);
                        privkeyButtonHintView.setVisibility(View.VISIBLE);

                        passwordHdrView.setVisibility(View.GONE);
                        passwordView.setText(null);
                        showPasswordCheckBox.setVisibility(View.GONE);
                        break;
                    default:
                        break;
                }
            }
        });

        if (!isViewShown) {
            loadFromWizardContext();
        }

        return v;
    }

    @Override
    protected void onVisibleToUser() {
        //Load from context
        loadFromWizardContext();
        if (isViewShown) {
            uuidView.setText(uuid);
            routerNameView.setText(isNullOrEmpty(routerName) ? "-" : routerName);
            routerIpOrDnsView.setText(isNullOrEmpty(routerIpOrDns) ? "-" : routerIpOrDns );
            routerFirmwareView.setText(isNullOrEmpty(routerFirmware) ? "-" : routerFirmware );
            connectionProtocolView.setText(isNullOrEmpty(connectionProtocol) ? "-" : connectionProtocol );
            portView.setText(isNullOrEmpty(port) ? "-" : port );
            usernameView.setText(isNullOrEmpty(username) ? "-" : username );
            passwordView.setText(isNullOrEmpty(password) ? "-" : password );
            privkeyButtonHintView.setText(isNullOrEmpty(privkeyButtonHint) ? "-" : privkeyButtonHint );
            if (authMethod != null) {
                switch (authMethod) {
                    case Router.SSHAuthenticationMethod_NONE:
                        authMethodRg.check(R.id.router_add_ssh_auth_method_none);
                        break;
                    case Router.SSHAuthenticationMethod_PASSWORD:
                        authMethodRg.check(R.id.router_add_ssh_auth_method_password);
                        break;
                    case Router.SSHAuthenticationMethod_PUBLIC_PRIVATE_KEY:
                        authMethodRg.check(R.id.router_add_ssh_auth_method_privkey);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void loadFromWizardContext() {
        final Map wizardContext = MaterialWizard.getWizardContext(getContext());

        final Object uuidObj = wizardContext.get("uuid");
        uuid = uuidObj != null ? uuidObj.toString() : "-";

        final Object routerNameObj = wizardContext.get("routerName");
        routerName = routerNameObj != null ? routerNameObj.toString() : "-";

        final Object routerIpOrDnsObj = wizardContext.get("routerIpOrDns");
        routerIpOrDns = routerIpOrDnsObj != null ? routerIpOrDnsObj.toString() : "-";

        final Object routerFirmwareObj = wizardContext.get("routerFirmware");
        routerFirmware = routerFirmwareObj != null ? routerFirmwareObj.toString() : "-";

        final Object connectionProtocolObj = wizardContext.get("connectionProtocol");
        connectionProtocol = connectionProtocolObj != null ?
                connectionProtocolObj.toString() : "-";

        final Object portObj = wizardContext.get("port");
        port = portObj != null ? portObj.toString() : "-";

        final Object usernameObj = wizardContext.get("username");
        username = usernameObj != null ? usernameObj.toString() : "-";

        final Object passwordObj = wizardContext.get("password");
        password = passwordObj != null ? Encrypted.d(passwordObj.toString()) : "-";

        final Object privkeyButtonHintObj = wizardContext.get("privkeyButtonHint");
        privkeyButtonHint = privkeyButtonHintObj != null ? privkeyButtonHintObj.toString() : "-";

        final Object privkeyPathObj = wizardContext.get("privkeyPath");
        if (privkeyPathObj != null) {
            privkeyPath = privkeyPathObj.toString();
        }

        final Object authMethodObj = wizardContext.get("authMethod");
        if (authMethodObj != null) {
            try {
                authMethod = Integer.parseInt(authMethodObj.toString());
            } catch (final NumberFormatException e) {
                e.printStackTrace();
            }
        }

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

    @Override
    protected void onExitNext() {
        if (TextUtils.isEmpty(router.getUuid())) {
            this.dao.insertRouter(router);
        } else {
            this.dao.updateRouter(router);
        }
    }

    @Override
    public boolean validateStep() {
        router = buildRouter();
        return (router != null);
    }
}
