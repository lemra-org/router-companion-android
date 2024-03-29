package org.rm3l.router_companion.tiles.admin.commands;

import static android.widget.TextView.BufferType.EDITABLE;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.actions.AbstractRouterAction;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.ExecStreamableCommandRouterAction;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterStreamActionListener;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.Utils;

public class AdminCommandsTile extends DDWRTTile<Void> {

  private static final String LOG_TAG = AdminCommandsTile.class.getSimpleName();

  private static final String LAST_COMMANDS = "lastCommands";

  private AbstractRouterAction<?> mCurrentRouterActionTask = null;

  private RouterStreamActionListener mRouterActionListener;

  public AdminCommandsTile(
      @NonNull Fragment parentFragment, @Nullable Bundle arguments, @Nullable Router router) {
    super(parentFragment, arguments, router, R.layout.tile_admin_commands, null);
  }

  @Override
  public void onStop() {
    if (mCurrentRouterActionTask == null) {
      return;
    }
    try {
      mCurrentRouterActionTask.cancel();
    } catch (final Exception e) {
      ReportingUtils.reportException(null, e);
    } finally {
      layout.findViewById(R.id.tile_toolbox_ping_abstract_loading_view).setVisibility(View.GONE);
      layout.findViewById(R.id.tile_admin_commands_submit_button).setEnabled(true);
      layout.findViewById(R.id.tile_admin_commands_cancel_button).setEnabled(false);
    }
  }

  @Override
  public Integer getTileHeaderViewId() {
    return -1;
  }

  @Override
  public Integer getTileTitleViewId() {
    return R.id.tile_admin_commands_title;
  }

  @Override
  public void onLoadFinished(Loader<Void> loader, Void data) {
    final Button button = (Button) layout.findViewById(R.id.tile_admin_commands_submit_button);
    final Button cancelButton =
        (Button) layout.findViewById(R.id.tile_admin_commands_cancel_button);

    final View progressBar = layout.findViewById(R.id.tile_toolbox_ping_abstract_loading_view);

    final TextView outputView = (TextView) layout.findViewById(R.id.tile_admin_commands_content);

    // Handle for Search EditText
    final EditText editText =
        (EditText) this.layout.findViewById(R.id.tile_admin_commands_edittext);
    // Initialize with existing search data
    editText.setText(
        mParentFragmentPreferences != null
            ? mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_COMMANDS), EMPTY_STRING)
            : EMPTY_STRING,
        EDITABLE);

    final TextView errorView = (TextView) layout.findViewById(R.id.tile_admin_commands_error);

    this.mRouterActionListener =
        new RouterStreamActionListener() {
          @Override
          public void notifyRouterActionProgress(
              @NonNull RouterAction routerAction,
              @NonNull Router router,
              final int progress,
              final String partialOutput) {
            mParentFragmentActivity.runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    if (!cancelButton.isEnabled()) {
                      cancelButton.setEnabled(true);
                    }
                    if (progress <= 0) {
                      outputView.setText(null);
                      return;
                    }
                    outputView.append(partialOutput);
                  }
                });
          }

          @Override
          public void onRouterActionFailure(
              @NonNull RouterAction routerAction,
              @NonNull Router router,
              @Nullable final Exception exception) {
            mParentFragmentActivity.runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    final String errorMsgToDisplay;
                    final Pair<String, String> exceptionHandled = Utils.handleException(exception);
                    if (exception != null) {
                      //noinspection ThrowableResultOfMethodCallIgnored
                      if (exception instanceof InterruptedException) {
                        errorMsgToDisplay = "Action Aborted.";
                      } else {
                        errorMsgToDisplay = ("Error: " + exceptionHandled.first);
                      }
                    } else {
                      errorMsgToDisplay = "Internal error! Please try again later.";
                    }
                    errorView.setText(errorMsgToDisplay);
                    errorView.setOnClickListener(
                        new View.OnClickListener() {
                          @Override
                          public void onClick(final View v) {
                            Toast.makeText(
                                    mParentFragmentActivity,
                                    (exception == null || exception instanceof InterruptedException)
                                        ? errorMsgToDisplay
                                        : exceptionHandled.second,
                                    Toast.LENGTH_LONG)
                                .show();
                          }
                        });
                    errorView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    button.setEnabled(true);
                    cancelButton.setEnabled(false);
                  }
                });
          }

          @Override
          public void onRouterActionSuccess(
              @NonNull RouterAction routerAction, @NonNull Router router, final Object returnData) {
            mParentFragmentActivity.runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    progressBar.setVisibility(View.GONE);
                    button.setEnabled(true);
                    cancelButton.setEnabled(false);
                  }
                });
          }
        };

    editText.setOnTouchListener(
        (v, event) -> {
          final int DRAWABLE_LEFT = 0;
          final int DRAWABLE_TOP = 1;
          final int DRAWABLE_RIGHT = 2;
          final int DRAWABLE_BOTTOM = 3;

          if (event.getAction() == MotionEvent.ACTION_UP) {
            if (event.getRawX()
                >= (editText.getRight()
                    - editText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
              errorView.setVisibility(View.GONE);
              // 'Clear' button - clear data, and reset everything out
              // Reset everything
              editText.setText(EMPTY_STRING);

              if (mParentFragmentPreferences != null) {
                final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
                editor.putString(getFormattedPrefKey(LAST_COMMANDS), EMPTY_STRING).apply();
              }

              return true;
            }
          }
          return false;
        });

    editText.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            errorView.setVisibility(View.GONE);

            final String textToFind = editText.getText().toString();
            if (isNullOrEmpty(textToFind)) {
              editText.requestFocus();
              openKeyboard(editText);
              return true;
            }

            final String existingSearch =
                mParentFragmentPreferences != null
                    ? mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_COMMANDS), null)
                    : null;

            if (mParentFragmentPreferences != null) {
              final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
              if (!textToFind.equalsIgnoreCase(existingSearch)) {
                editor.putString(getFormattedPrefKey(LAST_COMMANDS), textToFind);
              }
              editor.apply();
            }

            // Run command
            progressBar.setVisibility(View.VISIBLE);
            button.setEnabled(false);

            mCurrentRouterActionTask = getRouterAction(textToFind);
            ActionManager.runTasks(mCurrentRouterActionTask);

            return true;
          }
          return false;
        });

    button.setOnClickListener(
        v -> {
          errorView.setVisibility(View.GONE);
          final String textToFind = editText.getText().toString();
          if (isNullOrEmpty(textToFind)) {
            editText.requestFocus();
            openKeyboard(editText);
            return;
          }

          final String existingSearch =
              mParentFragmentPreferences != null
                  ? mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_COMMANDS), null)
                  : null;

          if (mParentFragmentPreferences != null) {
            if (!textToFind.equalsIgnoreCase(existingSearch)) {
              final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
              editor.putString(getFormattedPrefKey(LAST_COMMANDS), textToFind);
              editor.apply();
            }
          }

          // Run command
          progressBar.setVisibility(View.VISIBLE);
          button.setEnabled(false);

          mCurrentRouterActionTask = getRouterAction(textToFind);
          ActionManager.runTasks(mCurrentRouterActionTask);

          cancelButton.setEnabled(true);
        });

    cancelButton.setOnClickListener(v -> onStop());
  }

  @Nullable
  @Override
  protected Loader<Void> getLoader(int id, Bundle args) {
    return new AsyncTaskLoader<Void>(mParentFragmentActivity) {
      @Nullable
      @Override
      public Void loadInBackground() {
        return null;
      }
    };
  }

  @Nullable
  @Override
  protected String getLogTag() {
    return LOG_TAG;
  }

  @Nullable
  @Override
  protected OnClickIntent getOnclickIntent() {
    return null;
  }

  @NonNull
  private AbstractRouterAction getRouterAction(String cmdToRun) {
    return new ExecStreamableCommandRouterAction(
        mRouter, mParentFragmentActivity, mRouterActionListener, mGlobalPreferences, cmdToRun);
  }

  private void openKeyboard(final TextView mTextView) {
    final InputMethodManager imm =
        (InputMethodManager) mParentFragmentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm != null) {
      // only will trigger it if no physical keyboard is open
      imm.showSoftInput(mTextView, 0);
    }
  }
}
