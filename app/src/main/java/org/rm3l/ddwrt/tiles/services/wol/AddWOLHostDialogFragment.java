package org.rm3l.ddwrt.tiles.services.wol;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.google.common.base.Strings;
import com.google.gson.Gson;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.actions.WakeOnLANRouterAction;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.keyboardsurfer.android.widget.crouton.Style.ALERT;
import static org.rm3l.ddwrt.main.DDWRTMainActivity.ROUTER_ACTION;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;

public class AddWOLHostDialogFragment extends DialogFragment implements
        UndoBarController.AdvancedUndoListener, RouterActionListener {

    private static final String LOG_TAG =  AddWOLHostDialogFragment.class.getSimpleName();
    public static final String BROADCAST_ADDRESSES = "BROADCAST_ADDRESSES";
    public static final String WOL_PREF_KEY = \"fake-key\";
    private FragmentActivity activity;

    @Nullable
    private Router mRouter;

    @Nullable
    private ArrayList<String> bcastAddresses;

    protected SharedPreferences mGlobalPreferences;

    @Nullable
    protected SharedPreferences mRouterPreferences;

    private String mPreferencesKey;

    private Device mDevice;

    public static AddWOLHostDialogFragment newInstance(@NonNull final String routerUuid,
                                                       @NonNull final ArrayList<String> bcastAddresses,
                                                       @NonNull final String preferenceKey) {
        final AddWOLHostDialogFragment addWOLHostDialogFragment = new AddWOLHostDialogFragment();

        final Bundle args = new Bundle();
        args.putString(RouterManagementActivity.ROUTER_SELECTED, routerUuid);
        args.putStringArrayList(BROADCAST_ADDRESSES, bcastAddresses);
        args.putString(WOL_PREF_KEY, preferenceKey);

        addWOLHostDialogFragment.setArguments(args);

        return addWOLHostDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = getActivity();

        mGlobalPreferences = activity
                .getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);

        final Bundle arguments = getArguments();

        final String routerSelected = arguments.getString(RouterManagementActivity.ROUTER_SELECTED);

        if (routerSelected == null || routerSelected.isEmpty()) {
            Toast.makeText(activity, "Internal Error: unknown router. Please try again later", Toast.LENGTH_SHORT).show();
            Utils.reportException(new IllegalStateException("Internal Error: unknown router"));
            dismiss();
        }

        mRouterPreferences = (routerSelected != null ? activity
                .getSharedPreferences(routerSelected, Context.MODE_PRIVATE) : null);

        mRouter = RouterManagementActivity.getDao(activity).getRouter(routerSelected);

        bcastAddresses = arguments.getStringArrayList(BROADCAST_ADDRESSES);

        mPreferencesKey = \"fake-key\";

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = this.activity;

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Get the layout inflater
        final LayoutInflater inflater = activity.getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view = inflater.inflate(R.layout.activity_wol_host_add, null);

        builder
                .setMessage("For this to work properly:\n" +
                                "- Target hardware must support WOL. You can enable it in the BIOS or in the System Settings.\n" +
                                "- WOL magic packet will be sent from the router. To wake over the Internet, " +
                                "you must forward packets from any port you want to the device you wish to wake.\n" +
                                "Note: some devices support WOL only in Sleep or Hibernated mode, " +
                                "not powered off. Some may also require a SecureOn password, which is not supported (yet)!")
                .setView(view)
                        // Add action buttons
                .setPositiveButton("Send Magic Packet", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //Do nothing here because we override this button later to change the close behaviour.
                        //However, we still need this because on older versions of Android unless we
                        //pass a handler the button doesn't get instantiated
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                });
        builder.setTitle("Add a new WOL Host");

        return builder.create();

    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point

        final AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Validate form
                    boolean validForm = validateForm(d);

                    if (validForm) {

                        mDevice = new Device(((EditText) d.findViewById(R.id.wol_host_add_mac_addr)).getText().toString());
                        mDevice.setAlias(((EditText) d.findViewById(R.id.wol_host_add_name)).getText().toString());
                        try {
                            mDevice.setWolPort(Integer.parseInt(((EditText) d.findViewById(R.id.wol_host_add_port)).getText().toString()));
                        } catch (@NonNull final Exception e) {
                            e.printStackTrace();
                        }

                        final Bundle token = new Bundle();
                        token.putString(ROUTER_ACTION, RouterAction.WAKE_ON_LAN.name());

                        dismiss();

                        new UndoBarController.UndoBar(activity)
                                .message(String.format("WOL Request will be sent from router to '%s' (%s)",
                                        ((EditText) d.findViewById(R.id.wol_host_add_name)).getText().toString(),
                                        ((EditText) d.findViewById(R.id.wol_host_add_mac_addr)).getText().toString()))
                                .listener(AddWOLHostDialogFragment.this)
                                .token(token)
                                .show();
                    }
                    ///else dialog stays open. 'Cancel' button can still close it.
                }
            });
        }
    }

    private boolean validateForm(AlertDialog d) {
        final EditText macAddrView = (EditText) d.findViewById(R.id.wol_host_add_mac_addr);

        final Editable macAddrViewText = macAddrView.getText();
        if (Strings.isNullOrEmpty(macAddrViewText.toString())) {
            displayMessage("Invalid MAC Address:" + macAddrViewText, ALERT);
            macAddrView.requestFocus();
            openKeyboard(macAddrView);
            return false;
        }

        return true;
    }

    private void openKeyboard(final TextView mTextView) {
        final InputMethodManager imm = (InputMethodManager)
                activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // only will trigger it if no physical keyboard is open
            imm.showSoftInput(mTextView, 0);
        }
    }

    private void displayMessage(final String msg, final Style style) {
        if (isNullOrEmpty(msg)) {
            return;
        }
        final AlertDialog d = (AlertDialog) getDialog();
        Crouton.makeText(activity, msg, style,
                (ViewGroup) (d == null ? getView() : d.findViewById(R.id.wol_host_add_notification_viewgroup))).show();
    }

    @Override
    public void onHide(Parcelable parcelable) {
        if (parcelable instanceof Bundle) {
            final Bundle token = (Bundle) parcelable;
            final String routerAction = token.getString(ROUTER_ACTION);
            Log.d(LOG_TAG, "routerAction: [" + routerAction + "]");
            if (isNullOrEmpty(routerAction)) {
                return;
            }
            try {
                switch (RouterAction.valueOf(routerAction)) {
                    case WAKE_ON_LAN:
                        if (bcastAddresses == null) {
                            Utils.displayMessage(activity,
                                    "WOL Internal Error: unable to fetch broadcast addresses. Try again later.",
                                    Style.ALERT);
                            Utils.reportException(new IllegalStateException("WOL Internal Error: unable to fetch broadcast addresses. Try again later."));
                            return;
                        }

                        final AlertDialog d = (AlertDialog) getDialog();
                        if (d == null) {
                            Utils.displayMessage(activity,
                                    "WOL Internal Error. Try again later.",
                                    Style.ALERT);
                            Utils.reportException(new IllegalStateException("WOL Internal Error: Dialog is NULL."));
                            return;
                        }

                        new WakeOnLANRouterAction(activity, this, mGlobalPreferences, mDevice,
                                mDevice.getWolPort(),
                                bcastAddresses.toArray(new String[bcastAddresses.size()]))
                                .execute(mRouter);
                        break;
                    default:
                        //Ignored
                        break;
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                e.printStackTrace();
                Utils.displayMessage(activity,
                        "WOL Internal Error. Try again later.",
                        Style.ALERT);
                Utils.reportException(e);
            }
        }
    }

    @Override
    public void onClear(@NonNull Parcelable[] parcelables) {

    }

    @Override
    public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
        //Save (and dismiss)
        try {
            final AlertDialog d = (AlertDialog) getDialog();
            if (d == null) {
                Utils.displayMessage(activity,
                        "WOL Internal Error. Action succeeded, but failed to save entry",
                        Style.INFO);
                Utils.reportException(new IllegalStateException("WOL Internal Error: Dialog is NULL."));
                return;
            }

            Utils.displayMessage(activity,
                    String.format("Action '%s' executed successfully on host '%s'", routerAction.toString(), router.getRemoteIpAddress()),
                    Style.CONFIRM);

            if (mRouterPreferences == null) {
                return;
            }

            final HashSet<String> wolHosts = new HashSet<>(mRouterPreferences.getStringSet(mPreferencesKey, new HashSet<String>()));

            final Gson gson = WakeOnLanTile.GSON_BUILDER.create();
            wolHosts.add(gson.toJson(mDevice));

            mRouterPreferences.edit()
                    .putStringSet(mPreferencesKey, wolHosts)
                    .apply();
        } catch (final Exception e) {
            Utils.reportException(e);
        } finally {
            dismiss();
        }
    }

    @Override
    public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {

        dismiss();
        Utils.displayMessage(activity,
                String.format("Error on action '%s': %s", routerAction.toString(), ExceptionUtils.getRootCauseMessage(exception)),
                Style.ALERT);
        Utils.reportException(exception);

    }

    @Override
    public void onUndo(@Nullable Parcelable parcelable) {

    }
}
