package org.rm3l.router_companion.actions;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.DEFAULT_SHARED_PREFERENCES_KEY;
import static org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style.ALERT;

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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import com.google.common.base.Strings;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.io.IOException;
import java.io.InputStream;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.actions.RestoreRouterFromBackupAction.AgreementToRestoreRouterFromBackup;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.Utils;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

/** Created by rm3l on 10/05/15. */
public class RestoreRouterDialogFragment extends DialogFragment {

  private static final String LOG_TAG = RestoreRouterDialogFragment.class.getSimpleName();

  private static final int READ_REQUEST_CODE = 52;

  private SharedPreferences mGlobalSharedPreferences;

  private RouterRestoreDialogListener mListener = null;

  private Router mRouter;

  private InputStream mSelectedBackupInputStream = null;

  private Cursor mUriCursor = null;

  public static RestoreRouterDialogFragment newInstance(@NonNull final String routerUuid) {
    final RestoreRouterDialogFragment restoreRouterDialogFragment =
        new RestoreRouterDialogFragment();

    final Bundle args = new Bundle();
    args.putString(RouterManagementActivity.ROUTER_SELECTED, routerUuid);

    restoreRouterDialogFragment.setArguments(args);
    return restoreRouterDialogFragment;
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
      ReportingUtils.reportException(getContext(), e);
      Toast.makeText(
              activity,
              "Whoops - Internal error! " + "The issue has been reported. Please try again later",
              Toast.LENGTH_SHORT)
          .show();
      getDialog().cancel();
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mRouter =
        RouterManagementActivity.Companion.getDao(getActivity())
            .getRouter(getArguments().getString(RouterManagementActivity.ROUTER_SELECTED));

    if (mRouter == null) {
      ReportingUtils.reportException(
          getContext(),
          new IllegalStateException("Router passed to RestoreRouterDialogFragment is NULL"));
      Toast.makeText(getActivity(), "Router is NULL - does it still exist?", Toast.LENGTH_SHORT)
          .show();
      dismiss();
      return;
    }

    mGlobalSharedPreferences =
        getActivity().getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
  }

  @Override
  public void onStart() {
    super.onStart(); // super.onStart() is where dialog.show() is actually called on the underlying
    // dialog, so we have to do it after this point

    final AlertDialog d = (AlertDialog) getDialog();
    if (d != null) {
      d.findViewById(R.id.router_restore_backup_select_button)
          .setOnClickListener(
              new View.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.KITKAT)
                @Override
                public void onClick(View view) {
                  // Open up file picker

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

                  RestoreRouterDialogFragment.this.startActivityForResult(
                      intent, READ_REQUEST_CODE);
                }
              });

      d.getButton(Dialog.BUTTON_POSITIVE)
          .setOnClickListener(
              new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                  // Validate form
                  boolean validForm = validateForm(d);

                  if (validForm) {

                    final FragmentActivity activity = getActivity();

                    // For reporting
                    ReportingUtils.reportException(
                        getContext(), new AgreementToRestoreRouterFromBackup(activity));
                    ReportingUtils.reportEvent(
                        ReportingUtils.EVENT_AGREEMENT_TO_RESTORE_ROUTER, null);

                    // Now check actual connection to router ...
                    final AlertDialog alertDialog =
                        Utils.buildAlertDialog(
                            activity,
                            null,
                            String.format(
                                "Restoring from '%s' - please hold on...",
                                ((TextView) d.findViewById(R.id.router_restore_backup_path))
                                    .getText()),
                            false,
                            false);
                    alertDialog.show();
                    ((TextView) alertDialog.findViewById(android.R.id.message))
                        .setGravity(Gravity.CENTER_HORIZONTAL);
                    ActionManager.runTasks(
                        new RestoreRouterFromBackupAction(
                            mRouter,
                            activity,
                            new RouterActionListener() {
                              @Override
                              public void onRouterActionFailure(
                                  @NonNull RouterAction routerAction,
                                  @NonNull Router router,
                                  @Nullable Exception exception) {
                                try {
                                  if (mUriCursor != null) {
                                    mUriCursor.close();
                                  }
                                } catch (Exception e) {
                                  // No worries
                                } finally {
                                  alertDialog.cancel();

                                  mUriCursor = null;

                                  activity.runOnUiThread(
                                      new Runnable() {
                                        @Override
                                        public void run() {
                                          // Reset everything
                                          final Button fileSelectorButton =
                                              (Button)
                                                  d.findViewById(
                                                      R.id.router_restore_backup_select_button);
                                          fileSelectorButton.setHint(
                                              "Select Backup File to restore");
                                          mSelectedBackupInputStream = null;
                                          ((TextView)
                                                  d.findViewById(R.id.router_restore_backup_path))
                                              .setText(null);
                                          Utils.scrollToView(
                                              (ScrollView)
                                                  d.findViewById(R.id.router_restore_scrollview),
                                              fileSelectorButton);
                                          fileSelectorButton.requestFocus();
                                        }
                                      });

                                  displayMessage(
                                      String.format(
                                          "Error on action '%s': %s",
                                          routerAction.toString(),
                                          Utils.handleException(exception).first),
                                      Style.ALERT);
                                }
                              }

                              @Override
                              public void onRouterActionSuccess(
                                  @NonNull RouterAction routerAction,
                                  @NonNull Router router,
                                  Object returnData) {
                                try {
                                  Utils.displayMessage(
                                      activity,
                                      String.format(
                                          "Action '%s' executed successfully on host '%s'",
                                          routerAction.toString(), router.getRemoteIpAddress()),
                                      Style.CONFIRM);
                                } finally {
                                  alertDialog.cancel();
                                  // Also dismiss main activity
                                  dismiss();
                                }
                              }
                            },
                            mGlobalSharedPreferences,
                            mSelectedBackupInputStream));
                  }
                  /// else dialog stays open. 'Cancel' button can still close it.
                }
              });
    }
  }

  /**
   * Receive the result from a previous call to {@link
   * #startActivityForResult(android.content.Intent, int)}. This follows the related Activity API as
   * described there in {@link android.app.Activity#onActivityResult(int, int,
   * android.content.Intent)}.
   *
   * @param requestCode The integer request code originally supplied to startActivityForResult(),
   *     allowing you to identify who this result came from.
   * @param resultCode The integer result code returned by the child activity through its
   *     setResult().
   * @param resultData An Intent, which can return result data to the caller
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
        FirebaseCrashlytics.getInstance().log("Uri: " + uri.toString());
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

          if (contentResolver == null
              || (mUriCursor = contentResolver.query(uri, null, null, null, null)) == null) {
            displayMessage("Unknown Content Provider - please select a different location!", ALERT);
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

          // File size in bytes
          final long fileSize = mUriCursor.getLong(sizeIndex);
          final String filename = mUriCursor.getString(nameIndex);

          // TODO Figure out if a check on file size is needed! Check file size
          //                        if (fileSize > MAX_PRIVKEY_SIZE_BYTES) {
          //                            displayMessage(String
          //                                    .format("File '%s' too big (%s). Limit is %s",
          // filename,
          //                                            toHumanReadableByteCount(fileSize),
          //
          // toHumanReadableByteCount(MAX_PRIVKEY_SIZE_BYTES)), ALERT);
          //                            return;
          //                        }

          // Replace button hint message with file name
          final Button fileSelectorButton =
              (Button) d.findViewById(R.id.router_restore_backup_select_button);
          final CharSequence fileSelectorOriginalHint = fileSelectorButton.getHint();
          final TextView backupFilePath =
              (TextView) d.findViewById(R.id.router_restore_backup_path);
          if (!Strings.isNullOrEmpty(filename)) {
            fileSelectorButton.setHint(
                filename + " (" + Utils.toHumanReadableByteCount(fileSize) + ")");
            backupFilePath.setText(filename);
          } else {
            backupFilePath.setText(null);
          }

          // Set file actual content in hidden field
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
        .setTitle(
            String.format(
                "Restore '%s' (%s)", mRouter.getDisplayName(), mRouter.getRemoteIpAddress()))
        .setMessage(
            String.format(
                "Browse for a configuration file "
                    + "to overwrite all current configurations of '%s' (%s) "
                    + "with the ones in the backup file.\n\n"
                    + "[CAUTION]\n"
                    + "- Make sure to *backup* your settings first!!!\n"
                    + "- It is your responsibility to ensure the backup file to restore is fully compatible "
                    + "with the firmware and model of your router.\n"
                    + "- The Router will be rebooted at the end of the operation, and "
                    + "you might have to wait some time before connection is re-established.",
                mRouter.getDisplayName(), mRouter.getRemoteIpAddress()))
        .setView(view)
        // Add action buttons
        .setPositiveButton(
            "Got it! Proceed!",
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                // Do nothing here because we override this button later to change the close
                // behaviour.
                // However, we still need this because on older versions of Android unless we
                // pass a handler the button doesn't get instantiated
              }
            })
        .setNegativeButton(
            R.string.cancel,
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                RestoreRouterDialogFragment.this.getDialog().cancel();
              }
            });

    if (activity instanceof DDWRTMainActivity) {
      builder.setNeutralButton(
          "*Backup*",
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
              ((DDWRTMainActivity) activity)
                  .displayBackupDialog(
                      String.format(
                          "'%s' (%s)", mRouter.getDisplayName(), mRouter.getRemoteIpAddress()));
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
    Utils.displayMessage(
        getActivity(),
        msg,
        style,
        (ViewGroup)
            (d == null ? getView() : d.findViewById(R.id.router_restore_notification_viewgroup)));
  }

  private boolean validateForm(@NonNull AlertDialog d) {
    if (mSelectedBackupInputStream == null) {
      displayMessage("Please select a file to restore", ALERT);

      final View viewById = d.findViewById(R.id.router_restore_backup_select_button);
      Utils.scrollToView((ScrollView) d.findViewById(R.id.router_restore_scrollview), viewById);
      viewById.requestFocus();
      return false;
    }
    return true;
  }
}
