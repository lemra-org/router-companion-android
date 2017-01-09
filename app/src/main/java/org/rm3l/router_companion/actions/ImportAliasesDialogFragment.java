package org.rm3l.router_companion.actions;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdView;
import com.google.common.base.Strings;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.AdUtils;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarCallback;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static de.keyboardsurfer.android.widget.crouton.Style.ALERT;
import static org.rm3l.router_companion.main.DDWRTMainActivity.MAIN_ACTIVITY_ACTION;
import static org.rm3l.router_companion.main.DDWRTMainActivity.MAIN_ACTIVITY_ACTION_EXPORT_ALIASES_MAX_RETRIES;
import static org.rm3l.router_companion.main.DDWRTMainActivity.MAIN_ACTIVITY_ACTION_EXPORT_ALIASES_NB_RETRIES;
import static org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED;

/**
 * Created by rm3l on 10/05/15.
 */
public class ImportAliasesDialogFragment extends DialogFragment {

    private static final String LOG_TAG = ImportAliasesDialogFragment.class.getSimpleName();
    private static final int READ_REQUEST_CODE = 52;
    File tempFile = null;
    private Router mRouter;
    private ManageRouterAliasesActivity mListener = null;
    private InputStream mSelectedBackupInputStream = null;
    private SharedPreferences mRouterPreferences;
    private Cursor mUriCursor = null;

    public static ImportAliasesDialogFragment newInstance(@NonNull final String routerUuid) {
        final ImportAliasesDialogFragment importAliasesDialogFragment =
                new ImportAliasesDialogFragment();

        final Bundle args = new Bundle();
        args.putString(ROUTER_SELECTED, routerUuid);

        importAliasesDialogFragment.setArguments(args);

        return importAliasesDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getContext();

        mRouter = RouterManagementActivity
                .getDao(context)
                .getRouter(getArguments().getString(ROUTER_SELECTED));

        if (mRouter == null) {
            ReportingUtils.reportException(context,
                    new IllegalStateException("Router passed to RestoreRouterDialogFragment is NULL"));
            Toast.makeText(context,
                    "Router is NULL - does it still exist?", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        mRouterPreferences = Router.getPreferences(mRouter, context);
        if (mRouterPreferences == null) {
            ReportingUtils.reportException(context,
                    new IllegalStateException("mRouterPreferences == NULL"));
            Toast.makeText(context,
                    "Internal Error - please try again later", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Get the layout inflater
        final LayoutInflater inflater = activity.getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        final View view = inflater.inflate(R.layout.activity_router_import_aliases, null);

        builder
                .setTitle(String.format("Import Aliases for '%s' (%s)",
                        mRouter.getDisplayName(), mRouter.getRemoteIpAddress()))
                .setMessage(String.format("Browse for a backup file " +
                                "to import aliases for '%s' (%s) .\n\n" +
                                "[CAUTION]\n" +
                                "- Make sure to *backup* your aliases first!!!\n" +
                                "- Aliases in the backup file will overwrite those already defined.",
                        mRouter.getDisplayName(), mRouter.getRemoteIpAddress()))
                .setView(view)
                // Add action buttons
                .setPositiveButton("Got it! Proceed!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //Do nothing here because we override this button later to change the close behaviour.
                        //However, we still need this because on older versions of Android unless we
                        //pass a handler the button doesn't get instantiated
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ImportAliasesDialogFragment.this.getDialog().cancel();
                    }
                });

        if (activity instanceof DDWRTMainActivity) {
            builder
                    .setNeutralButton("*Backup*", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dismiss();

                            final Bundle token = new Bundle();
                            token.putInt(MAIN_ACTIVITY_ACTION, RouterActions.EXPORT_ALIASES);
                            token.putInt(MAIN_ACTIVITY_ACTION_EXPORT_ALIASES_MAX_RETRIES, 3);
                            token.putInt(MAIN_ACTIVITY_ACTION_EXPORT_ALIASES_NB_RETRIES, 0);

                            SnackbarUtils.buildSnackbar(activity,
                                    String.format("Going to start exporting aliases for '%s' (%s)...",
                                            mRouter.getDisplayName(), mRouter.getRemoteIpAddress()),
                                    "Undo",
                                    Snackbar.LENGTH_SHORT,
                                    (DDWRTMainActivity) activity,
                                    token,
                                    true);
                        }
                    });
        }

        return builder.create();
    }

    private void displayMessage(final String msg, final Style style) {
        if (isNullOrEmpty(msg)) {
            return;
        }
        final AlertDialog d = (AlertDialog) getDialog();
        Crouton.makeText(getActivity(), msg, style, (ViewGroup) (d == null ? getView() :
                d.findViewById(R.id.router_import_aliases_notification_viewgroup))).show();
    }

    /**
     * Receive the result from a previous call to
     * {@link #startActivityForResult(Intent, int)}.  This follows the
     * related Activity API as described there in
     * {@link Activity#onActivityResult(int, int, Intent)}.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param resultData  An Intent, which can return result data to the caller
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                Crashlytics.log(Log.INFO, LOG_TAG, "Uri: " + uri.toString());
                final AlertDialog d = (AlertDialog) getDialog();

                if (d != null) {

                    try {
                        if (mUriCursor != null) {
                            mUriCursor.close();
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                        ReportingUtils.reportException(getContext(), e);
                    } finally {
                        mUriCursor = null;
                    }

                    final ContentResolver contentResolver = this.getActivity().getContentResolver();

                    if (contentResolver == null || (mUriCursor =
                            contentResolver.query(uri, null, null, null, null)) == null) {
                        displayMessage("Unknown Content Provider - please select a different location!",
                                ALERT);
                        return;
                    }

                    /*
                     * Get the column indexes of the data in the Cursor,
                     * move to the first row in the Cursor, get the data,
                     * and display it.
                     */
                    final int nameIndex = mUriCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    final int sizeIndex = mUriCursor.getColumnIndex(OpenableColumns.SIZE);

                    mUriCursor.moveToFirst();

                    //File size in bytes
                    final long fileSize = mUriCursor.getLong(sizeIndex);
                    final String filename = mUriCursor.getString(nameIndex);

                    //TODO Figure out if a check on file size is needed! Check file size
//                        if (fileSize > MAX_PRIVKEY_SIZE_BYTES) {
//                            displayMessage(String
//                                    .format("File '%s' too big (%s). Limit is %s", filename,
//                                            toHumanReadableByteCount(fileSize),
//                                            toHumanReadableByteCount(MAX_PRIVKEY_SIZE_BYTES)), ALERT);
//                            return;
//                        }

                    //Replace button hint message with file name
                    final Button fileSelectorButton = (Button)
                            d.findViewById(R.id.router_import_aliases_select_button);
                    final CharSequence fileSelectorOriginalHint = fileSelectorButton.getHint();
                    final TextView backupFilePath = (TextView) d.findViewById(R.id.router_import_aliases_path);
                    if (!Strings.isNullOrEmpty(filename)) {
                        fileSelectorButton.setHint(filename + " (" +
                                Utils.toHumanReadableByteCount(fileSize) + ")");
                        backupFilePath.setText(filename);
                    } else {
                        backupFilePath.setText(null);
                    }

                    //Set file actual content in hidden field
                    try {
                        mSelectedBackupInputStream = contentResolver.openInputStream(uri);
                    } catch (IOException e) {
                        displayMessage("Error: " + e.getMessage(), ALERT);
                        e.printStackTrace();
                        fileSelectorButton.setHint(fileSelectorOriginalHint);
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }

    @Override
    public void onDestroy() {
        try {
            if (mUriCursor != null) {
                mUriCursor.close();
            }
        } catch (final Exception e) {
            e.printStackTrace();
            ReportingUtils.reportException(getContext(), e);
        } finally {
            mUriCursor = null;
            super.onDestroy();
        }
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ManageRouterAliasesActivity) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            ReportingUtils.reportException(getContext(), e);
            Toast.makeText(activity, "Whoops - Internal error! " +
                    "The issue has been reported. Please try again later", Toast.LENGTH_SHORT).show();
            getDialog().cancel();
        }
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point

        final AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {

            if (BuildConfig.WITH_ADS) {
                //For Ads to show up, otherwise we get the following error message:
                //Not enough space to show ad. Needs 320x50 dp, but only has 288x597 dp.
                d.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT);
            }

            AdUtils.buildAndDisplayAdViewIfNeeded(d.getContext(),
                    (AdView) d.findViewById(R.id.router_import_aliases_adView));

            d.findViewById(R.id.router_import_aliases_select_button).setOnClickListener(new View.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.KITKAT)
                @Override
                public void onClick(View view) {
                    //Open up file picker

                    // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
                    // browser.
                    final Intent intent = new Intent();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                    } else {
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                    }

                    // Filter to only show results that can be "opened", such as a
                    // file (as opposed to a list of contacts or timezones)
                    intent.addCategory(Intent.CATEGORY_OPENABLE);

                    // search for all documents available via installed storage providers
                    intent.setType("*/*");

                    ImportAliasesDialogFragment.this.startActivityForResult(intent, READ_REQUEST_CODE);
                }
            });

            d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //Validate form
                    boolean validForm = validateForm(d);

                    if (validForm) {

                        final FragmentActivity activity = getActivity();

                        final Bundle token = new Bundle();
                        token.putInt(MAIN_ACTIVITY_ACTION, RouterActions.IMPORT_ALIASES);

                        SnackbarUtils.buildSnackbar(activity,
                                String.format("Going to start importing aliases for '%s' (%s)...",
                                        mRouter.getDisplayName(), mRouter.getRemoteIpAddress()),
                                "Undo",
                                Snackbar.LENGTH_SHORT,
                                new SnackbarCallback() {
                                    @Override
                                    public void onShowEvent(@Nullable Bundle bundle) throws Exception {

                                        //Save file choosen
                                        final Context context = getContext();
                                        try {
                                            tempFile = File.createTempFile("aliases_to_import_" + mRouter.getUuid(), ".json",
                                                    context.getCacheDir());

                                            FileUtils.copyInputStreamToFile(mSelectedBackupInputStream, tempFile);

                                            if (mUriCursor != null) {
                                                mUriCursor.close();
                                            }

                                            dismiss();
                                        } catch (final Exception e) {
                                            Utils.reportException(context, e);
                                            displayMessage("Error - please try again later",
                                                    Style.ALERT);
                                        } finally {
                                            mUriCursor = null;
                                        }
                                    }

                                    @Override
                                    public void onDismissEventSwipe(int event, @Nullable Bundle bundle) throws Exception {

                                    }

                                    @Override
                                    public void onDismissEventActionClick(int event, @Nullable Bundle bundle) throws Exception {

                                    }

                                    @Override
                                    public void onDismissEventTimeout(int event, @Nullable Bundle bundle) throws Exception {
                                        //Proceed with action
                                        final Context context = getContext();
                                        try {
                                            if (tempFile == null) {
                                                Utils.reportException(context,
                                                        new IllegalStateException("tempFile is null"));
                                                displayMessage("Internal Error - please try again later", Style.ALERT);
                                                return;
                                            }

                                            final Map<String, String> aliasesToPersist = new HashMap<>();

                                            try {
                                                final String fileToString = FileUtils.readFileToString(tempFile);

                                                final JSONObject jsonObject = new JSONObject(fileToString);

                                                final Iterator<String> keys = jsonObject.keys();
                                                while (keys.hasNext()) {
                                                    final String key = keys.next();
                                                    final String value = jsonObject.getString(key);
                                                    if (isNullOrEmpty(key)) {
                                                        continue;
                                                    }
                                                    //Check whether key is a MAC-Address
                                                    if (!Utils.MAC_ADDRESS.matcher(key).matches()) {
                                                        continue;
                                                    }
                                                    //This is a MAC Address - collect it right away!
                                                    aliasesToPersist.put(key, nullToEmpty(value));
                                                }

                                            } catch (final Exception e) {
                                                Utils.reportException(context,
                                                        e);
                                                displayMessage("Error - please check the file you provided; it must be a valid JSON file",
                                                        Style.ALERT);
                                                return;
                                            }

                                            //Check whether 'Clear' switch button is checked
                                            final boolean isClearChecked =
                                                    ((SwitchCompat) d.findViewById(R.id.router_import_aliases_clear_aliases))
                                                            .isChecked();
                                            if (isClearChecked) {
                                                final Map<String, ?> allRouterPrefs = mRouterPreferences.getAll();
                                                if (allRouterPrefs == null || allRouterPrefs.isEmpty()) {
                                                    return;
                                                }

                                                final SharedPreferences.Editor editor = mRouterPreferences.edit();
                                                for (final Map.Entry<String, ?> entry : allRouterPrefs.entrySet()) {
                                                    final String key = entry.getKey();
                                                    final Object value = entry.getValue();
                                                    if (isNullOrEmpty(key) || value == null) {
                                                        continue;
                                                    }
                                                    //Check whether key is a MAC-Address
                                                    if (!Utils.MAC_ADDRESS.matcher(key).matches()) {
                                                        continue;
                                                    }
                                                    //This is a MAC Address - collect it right away!
                                                    editor.remove(key);
                                                }
                                                editor.apply();
                                            }

                                            //Now inject new aliases
                                            final SharedPreferences.Editor editor = mRouterPreferences.edit();
                                            for (final Map.Entry<String, String> aliasEntry : aliasesToPersist.entrySet()) {
                                                editor.putString(aliasEntry.getKey(), aliasEntry.getValue());
                                            }
                                            editor.apply();

                                            Utils.displayMessage(activity,
                                                    String.format("Action 'Import Aliases' executed successfully on host '%s'",
                                                            mRouter.getRemoteIpAddress()),
                                                    Style.CONFIRM);

                                            if (mListener != null) {
                                                mListener.onRefresh();
                                            }

                                        } catch (final Exception e) {
                                            displayMessage("Error - please try again later", Style.ALERT);
                                            e.printStackTrace();
                                            Utils.reportException(context, e);
                                        } finally {
                                            d.cancel();
                                        }
                                    }

                                    @Override
                                    public void onDismissEventManual(int event, @Nullable Bundle bundle) throws Exception {

                                    }

                                    @Override
                                    public void onDismissEventConsecutive(int event, @Nullable Bundle bundle) throws Exception {

                                    }
                                },
                                token,
                                true);
                    }
                    ///else dialog stays open. 'Cancel' button can still close it.
                }
            });
        }
    }

    private boolean validateForm(@NonNull AlertDialog d) {
        if (mSelectedBackupInputStream == null) {
            displayMessage("Please select a file to import", ALERT);

            final View viewById = d.findViewById(R.id.router_import_aliases_select_button);
            Utils.scrollToView((ScrollView) d.findViewById(R.id.router_import_aliases_scrollview), viewById);
            viewById.requestFocus();
            return false;
        }
        return true;
    }

}
