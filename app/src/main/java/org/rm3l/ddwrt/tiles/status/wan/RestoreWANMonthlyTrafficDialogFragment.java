package org.rm3l.ddwrt.tiles.status.wan;

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
import android.os.Handler;
import android.os.Parcelable;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.google.android.gms.ads.AdView;
import com.google.common.base.Strings;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.BackupWANMonthlyTrafficRouterAction;
import org.rm3l.ddwrt.actions.BackupWANMonthlyTrafficRouterAction.BackupFileType;
import org.rm3l.ddwrt.actions.RestoreWANMonthlyTrafficFromBackupAction;
import org.rm3l.ddwrt.actions.RestoreWANMonthlyTrafficFromBackupAction.AgreementToRestoreWANTraffDataFromBackup;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterActionListener;
import org.rm3l.ddwrt.actions.RouterRestoreDialogListener;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.utils.AdUtils;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.keyboardsurfer.android.widget.crouton.Style.ALERT;
import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficTile.WAN_MONTHLY_TRAFFIC_ACTION;
import static org.rm3l.ddwrt.tiles.status.wan.WANMonthlyTrafficTile.WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;

/**
 * Created by rm3l on 10/05/15.
 */
public class RestoreWANMonthlyTrafficDialogFragment extends DialogFragment
        implements UndoBarController.AdvancedUndoListener {

    private static final String LOG_TAG = RestoreWANMonthlyTrafficDialogFragment.class.getSimpleName();
    private static final int READ_REQUEST_CODE = 525;

    private Router mRouter;
    private RouterRestoreDialogListener mListener = null;
    private InputStream mSelectedBackupInputStream = null;
    private SharedPreferences mGlobalSharedPreferences;

    private Cursor mUriCursor = null;

    public static RestoreWANMonthlyTrafficDialogFragment newInstance(@NonNull final String routerUuid) {
        final RestoreWANMonthlyTrafficDialogFragment restoreWANMonthlyTrafficDialogFragment = new RestoreWANMonthlyTrafficDialogFragment();

        final Bundle args = new Bundle();
        args.putString(ROUTER_SELECTED, routerUuid);

        restoreWANMonthlyTrafficDialogFragment.setArguments(args);
        return restoreWANMonthlyTrafficDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRouter = RouterManagementActivity
                .getDao(getActivity())
                .getRouter(getArguments().getString(ROUTER_SELECTED));

        if (mRouter == null) {
            Utils.reportException(new IllegalStateException("Router passed to RestoreWANMonthlyTrafficDialogFragment is NULL"));
            Toast.makeText(getActivity(),
                    "Router is NULL - does it still exist?", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        mGlobalSharedPreferences = getActivity()
                .getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
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
        final View view = inflater.inflate(R.layout.activity_router_restore, null);

        builder
                .setTitle(String.format("Restore WAN Traffic Data on '%s' (%s)",
                        mRouter.getDisplayName(), mRouter.getRemoteIpAddress()))
                .setMessage(String.format("Browse for a raw backup file and" +
                                " restore the WAN Monthly Traffic Data on '%s' (%s) " +
                                "with the ones in the backup file.\n\n" +
                                "[CAUTION]\n" +
                                "- Make sure to *backup* your settings first!!!\n" +
                                "- It is your responsibility to ensure the backup file to restore is fully compatible " +
                                "with the firmware and model of your router.",
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
                .setNeutralButton("*Backup*", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        displayBackupDialog(String.format("'%s' (%s)",
                                        mRouter.getDisplayName(), mRouter.getRemoteIpAddress()),
                                BackupFileType.RAW);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                RestoreWANMonthlyTrafficDialogFragment.this.getDialog().cancel();
            }
        });

        return builder.create();
    }

    public void displayBackupDialog(final String displayName,
                                    @NonNull final BackupFileType backupFileType) {
        final Bundle token = new Bundle();
        token.putString(WAN_MONTHLY_TRAFFIC_ACTION, RouterAction.BACKUP_WAN_TRAFF.name());
        token.putSerializable(WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE, backupFileType);

        new UndoBarController.UndoBar(getActivity())
                .message(String.format("Backup of WAN Traffic Data (as %s) is going to start on %s...",
                        backupFileType, displayName))
                .listener(RestoreWANMonthlyTrafficDialogFragment.this)
                .token(token)
                .show();
    }

    private void displayMessage(final String msg, final Style style) {
        if (isNullOrEmpty(msg)) {
            return;
        }
        final AlertDialog d = (AlertDialog) getDialog();
        Crouton.makeText(getActivity(), msg, style, (ViewGroup) (d == null ? getView() :
                d.findViewById(R.id.router_restore_notification_viewgroup))).show();
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
                Log.i(LOG_TAG, "Uri: " + uri.toString());
                final AlertDialog d = (AlertDialog) getDialog();

                if (d != null) {

                    try {
                        if (mUriCursor != null) {
                            mUriCursor.close();
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                        Utils.reportException(e);
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
                            d.findViewById(R.id.router_restore_backup_select_button);
                    final CharSequence fileSelectorOriginalHint = fileSelectorButton.getHint();
                    final TextView backupFilePath = (TextView) d.findViewById(R.id.router_restore_backup_path);
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
            Utils.reportException(e);
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
            mListener = (RouterRestoreDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            Utils.reportException(e);
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
                    (AdView) d.findViewById(R.id.activity_router_restore_adView));

            d.findViewById(R.id.router_restore_backup_select_button).setOnClickListener(new View.OnClickListener() {
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

                    RestoreWANMonthlyTrafficDialogFragment.this.startActivityForResult(intent, READ_REQUEST_CODE);
                }
            });

            d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Validate form
                    boolean validForm = validateForm(d);

                    if (validForm) {

                        final FragmentActivity activity = getActivity();

                        //For reporting
                        Utils.reportException(new AgreementToRestoreWANTraffDataFromBackup(activity));

                        // Now check actual connection to router ...
                        final AlertDialog alertDialog = Utils.
                                buildAlertDialog(activity, null,
                                        String.format("Restoring WAN Monthly Traffic from '%s' - please hold on...",
                                                ((TextView) d.findViewById(R.id.router_restore_backup_path)).getText()),
                                        false, false);
                        alertDialog.show();
                        ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                new RestoreWANMonthlyTrafficFromBackupAction(activity, new RouterActionListener() {
                                    @Override
                                    public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
                                        try {
                                            Utils.displayMessage(activity,
                                                    String.format("Action '%s' executed successfully on host '%s'. " +
                                                                    "Data will refresh upon next sync.",
                                                            routerAction.toString(), router.getRemoteIpAddress()),
                                                    Style.CONFIRM);
                                        } finally {
                                            alertDialog.cancel();
                                            //Also dismiss main activity
                                            dismiss();
                                        }

                                    }

                                    @Override
                                    public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
                                        try {
                                            if (mUriCursor != null) {
                                                mUriCursor.close();
                                            }
                                        } catch (Exception e) {
                                            //No worries
                                        } finally {
                                            alertDialog.cancel();

                                            mUriCursor = null;

                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    //Reset everything
                                                    final Button fileSelectorButton = (Button)
                                                            d.findViewById(R.id.router_restore_backup_select_button);
                                                    fileSelectorButton.setHint("Select Backup File to restore");
                                                    mSelectedBackupInputStream = null;
                                                    ((TextView) d.findViewById(R.id.router_restore_backup_path)).setText(null);

                                                    fileSelectorButton.requestFocus();
                                                }
                                            });

                                            displayMessage(String.format("Error on action '%s': %s", routerAction.toString(), ExceptionUtils.getRootCauseMessage(exception)),
                                                    Style.ALERT);

                                        }
                                    }
                                }, mGlobalSharedPreferences, mSelectedBackupInputStream)
                                        .execute(mRouter);
                            }
                        }, 2000);
                    }
                    ///else dialog stays open. 'Cancel' button can still close it.
                }
            });
        }
    }

    private boolean validateForm(@NonNull AlertDialog d) {
        if (mSelectedBackupInputStream == null) {
            displayMessage("Please select a file to restore", ALERT);
            d.findViewById(R.id.router_restore_backup_select_button).requestFocus();
            return false;
        }
        return true;
    }

    @Override
    public void onHide(@Nullable Parcelable parcelable) {
        if (parcelable instanceof Bundle) {
            final Bundle token = (Bundle) parcelable;
            final String routerAction = token.getString(WAN_MONTHLY_TRAFFIC_ACTION);
            Log.d(LOG_TAG, "WAN Monthly Traffic Data Action: [" + routerAction + "]");
            if (isNullOrEmpty(routerAction)) {
                return;
            }

            final FragmentActivity activity = getActivity();

            try {
                switch (RouterAction.valueOf(routerAction)) {
                    case BACKUP_WAN_TRAFF:
                        final BackupFileType fileType =
                                (BackupFileType) token.getSerializable(WAN_MONTHLY_TRAFFIC_BACKUP_FILETYPE);
                        final AlertDialog alertDialog = Utils.
                                buildAlertDialog(activity,
                                        null, "Backing up WAN Traffic Data - please hold on...", false, false);
                        alertDialog.show();
                        ((TextView) alertDialog.findViewById(android.R.id.message)).setGravity(Gravity.CENTER_HORIZONTAL);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                new BackupWANMonthlyTrafficRouterAction(fileType, activity,
                                        new RouterActionListener() {

                                            @Override
                                            public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, Object returnData) {
                                                try {
                                                    String msg;
                                                    if (!((returnData instanceof Object[]) &&
                                                            ((Object[]) returnData).length >= 2)) {
                                                        msg = String.format("Action '%s' executed " +
                                                                        "successfully on host '%s', but an internal error occurred. " +
                                                                        "The issue will be reported. Please try again later.",
                                                                routerAction.toString(),
                                                                router.getRemoteIpAddress());
                                                        Utils.displayMessage(activity,
                                                                msg,
                                                                Style.INFO);
                                                        Utils.reportException(new IllegalStateException(msg));
                                                        return;
                                                    }

                                                    final Object[] returnDataObjectArray = ((Object[]) returnData);
                                                    final Object backupDateObject = returnDataObjectArray[0];
                                                    final Object localBackupFileObject = returnDataObjectArray[1];

                                                    if (!((backupDateObject instanceof Date) &&
                                                            (localBackupFileObject instanceof File))) {
                                                        msg = String.format("Action '%s' executed " +
                                                                        "successfully on host '%s', but could not determine where " +
                                                                        "local backup file has been saved. Please try again later.",
                                                                routerAction.toString(),
                                                                router.getRemoteIpAddress());
                                                        Utils.displayMessage(activity,
                                                                msg,
                                                                Style.INFO);
                                                        Utils.reportException(new IllegalStateException(msg));
                                                        return;
                                                    }

                                                    Utils.displayMessage(activity,
                                                            String.format("Action '%s' executed successfully on host '%s'. " +
                                                                            "Now loading the file sharing activity chooser...",
                                                                    routerAction.toString(), router.getRemoteIpAddress()),
                                                            Style.CONFIRM);

                                                    final File localBackupFile = (File) (((Object[]) returnData)[1]);
                                                    final Date backupDate = (Date) (((Object[]) returnData)[0]);

                                                    final Uri uriForFile = FileProvider.getUriForFile(activity,
                                                            DDWRTCompanionConstants.FILEPROVIDER_AUTHORITY,
                                                            localBackupFile);
                                                    activity.grantUriPermission(
                                                            activity.getPackageName(),
                                                            uriForFile, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                                    final Intent shareIntent = new Intent();
                                                    shareIntent.setAction(Intent.ACTION_SEND);
                                                    shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                                                            String.format("Backup of WAN Monthly Traffic on Router '%s' (%s)",
                                                                    mRouter.getDisplayName(), mRouter.getRemoteIpAddress()));
                                                    shareIntent.setType("text/html");
                                                    shareIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(
                                                            ("Backup Date: " + backupDate + "\n\n").replaceAll("\n", "<br/>") +
                                                                    Utils.getShareIntentFooter()));
                                                    shareIntent.putExtra(Intent.EXTRA_STREAM, uriForFile);
                                                    activity.startActivity(Intent.createChooser(shareIntent,
                                                            activity.getResources().getText(R.string.share_backup)));

                                                } finally {
                                                    activity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            alertDialog.cancel();
                                                        }
                                                    });
                                                }
                                            }

                                            @Override
                                            public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router, @Nullable Exception exception) {
                                                try {
                                                    Utils.displayMessage(activity,
                                                            String.format("Error on action '%s': %s",
                                                                    routerAction.toString(),
                                                                    ExceptionUtils.getRootCauseMessage(exception)),
                                                            Style.ALERT);
                                                } finally {
                                                    activity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            alertDialog.cancel();
                                                        }
                                                    });
                                                }
                                            }
                                        },
                                        activity
                                            .getSharedPreferences(
                                                    DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                                                    Context.MODE_PRIVATE))
                                        .execute(mRouter);
                            }
                        }, 1500);
                        return;
                    default:
                        break;
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                e.printStackTrace();
                Utils.reportException(e);
            }

        }
    }

    @Override
    public void onClear(@NonNull Parcelable[] parcelables) {

    }

    @Override
    public void onUndo(@Nullable Parcelable parcelable) {

    }
}
