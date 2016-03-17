package org.rm3l.ddwrt.mgmt.register.steps;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import org.codepond.wizardroid.WizardStep;
import org.codepond.wizardroid.persistence.ContextVariable;
import org.rm3l.ddwrt.R;

/**
 * Created by rm3l on 15/03/16.
 */
public class RouterConnectionDetailsStep extends WizardStep {


    @ContextVariable
    private String username;

    private EditText usernameEt;

    @ContextVariable
    private String port;

    private EditText portEt;

    @ContextVariable
    private String connectionProtocol;

    private Spinner connectionProtocolView;

    @ContextVariable
    private String password;

    private EditText pwdView;

    @ContextVariable
    private int checkedAuthMethodRadioButtonId;

    private RadioGroup authMethodRg;

    //Wire the layout to the step
    public RouterConnectionDetailsStep() {
    }

    //Set your layout here
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(
                R.layout.wizard_add_router_2_router_connection_details_step, container, false);


        usernameEt = (EditText) view.findViewById(R.id.router_add_username);

        portEt = (EditText) view.findViewById(R.id.router_add_port);

        connectionProtocolView = (Spinner) view.findViewById(R.id.router_add_proto);

        pwdView = (EditText) view.findViewById(R.id.router_add_password);
        final CheckBox pwdShowCheckBox = (CheckBox) view.findViewById(R.id.router_add_password_show_checkbox);

        pwdShowCheckBox
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!isChecked) {
                            pwdView.setInputType(
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            pwdView.requestFocus();
                        } else {
                            pwdView.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                            pwdView.requestFocus();
                        }
                        pwdView.setSelection(pwdView.length());
                    }
                });

        ((RadioGroup) view.findViewById(R.id.router_add_ssh_auth_method))
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        final View privkeyHdrView = view.findViewById(R.id.router_add_privkey_hdr);
                        final Button privkeyView = (Button) view.findViewById(R.id.router_add_privkey);
                        final TextView privkeyPathView = (TextView) view.findViewById(R.id.router_add_privkey_path);
                        final TextView pwdHdrView = (TextView) view.findViewById(R.id.router_add_password_hdr);
//                        final EditText pwdView = (EditText) view.findViewById(R.id.router_add_password);
//                        final CheckBox pwdShowCheckBox = (CheckBox) view.findViewById(R.id.router_add_password_show_checkbox);

                        switch (checkedId) {
                            case R.id.router_add_ssh_auth_method_none:
                                privkeyPathView.setText(null);
                                privkeyHdrView.setVisibility(View.GONE);
                                privkeyView.setVisibility(View.GONE);
                                pwdHdrView.setVisibility(View.GONE);
                                pwdView.setText(null);
                                pwdView.setVisibility(View.GONE);
                                pwdShowCheckBox.setVisibility(View.GONE);
                                break;
                            case R.id.router_add_ssh_auth_method_password:
                                privkeyPathView.setText(null);
                                privkeyHdrView.setVisibility(View.GONE);
                                privkeyView.setVisibility(View.GONE);
                                pwdHdrView.setText("Password");
                                pwdHdrView.setVisibility(View.VISIBLE);
                                pwdView.setVisibility(View.VISIBLE);
                                pwdView.setHint("e.g., 'default' (may be empty) ");
                                pwdShowCheckBox.setVisibility(View.VISIBLE);
                                break;
                            case R.id.router_add_ssh_auth_method_privkey:
                                pwdView.setText(null);
                                privkeyView.setHint(getString(R.string.router_add_path_to_privkey));
                                pwdHdrView.setText("Passphrase (if applicable)");
                                pwdHdrView.setVisibility(View.VISIBLE);
                                pwdView.setVisibility(View.VISIBLE);
                                pwdView.setHint("Key passphrase, if applicable");
                                pwdShowCheckBox.setVisibility(View.VISIBLE);
                                privkeyHdrView.setVisibility(View.VISIBLE);
                                privkeyView.setVisibility(View.VISIBLE);
                                break;
                            default:
                                break;
                        }
                    }
                });


        //and set default values by using Context Variables
        usernameEt.setText(username);
        portEt.setText(port);
        pwdView.setText(password);

        authMethodRg = (RadioGroup) view.findViewById(R.id.router_add_ssh_auth_method);

        authMethodRg.check(checkedAuthMethodRadioButtonId);

        //TODO Set Spinner

        return view;
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
        //TODO Do some work
        //...
        //The values of these fields will be automatically stored in the wizard context
        //and will be populated in the next steps only if the same field names are used.
        username = usernameEt.getText().toString();
        password = pwdView.getText().toString();
        port = portEt.getText().toString();
        checkedAuthMethodRadioButtonId = authMethodRg.getCheckedRadioButtonId();
    }
}
