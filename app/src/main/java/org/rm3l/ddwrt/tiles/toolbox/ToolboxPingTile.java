package org.rm3l.ddwrt.tiles.toolbox;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Throwables;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.PingFromRouterAction;
import org.rm3l.ddwrt.actions.RouterAction;
import org.rm3l.ddwrt.actions.RouterStreamActionListener;
import org.rm3l.ddwrt.resources.None;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.EMPTY_STRING;

public class ToolboxPingTile extends DDWRTTile<None> {

    private static final String LAST_HOST = "lastHost";

    public ToolboxPingTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_toolbox_ping, null);

        final View button = layout.findViewById(R.id.tile_toolbox_ping_ping);
        final View progressBar = layout.findViewById(R.id.tile_toolbox_ping_content_loading_view);
        final TextView outputView = (TextView) layout.findViewById(R.id.tile_toolbox_ping_content);

        //Set button handlers over here
        //Handle for Search EditText
        final EditText pingEditText = (EditText) this.layout.findViewById(R.id.tile_toolbox_ping_filter);
        //Initialize with existing search data
        pingEditText.setText(mParentFragmentPreferences != null ?
                mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_HOST), EMPTY_STRING) : EMPTY_STRING);

        final TextView errorView = (TextView) layout.findViewById(R.id.tile_toolbox_ping_error);

        pingEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (pingEditText.getRight() - pingEditText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        errorView.setVisibility(View.GONE);
                        //'Clear' button - clear data, and reset everything out
                        //Reset everything
                        pingEditText.setText(EMPTY_STRING);

                        if (mParentFragmentPreferences != null) {
                            final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
                            editor.putString(getFormattedPrefKey(LAST_HOST), EMPTY_STRING);
                            editor.apply();
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        final RouterStreamActionListener routerActionListener = new RouterStreamActionListener() {
            @Override
            public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, final Object returnData) {
                mParentFragmentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        if (returnData instanceof String[]) {
////                            outputView.setVisibility(View.VISIBLE);
//                            outputView.setText(Joiner.on("\n").skipNulls().join((String[]) returnData));
//                        }
                        progressBar.setVisibility(View.GONE);
                        button.setEnabled(true);
                    }
                });
            }

            @Override
            public void onRouterActionFailure(@NonNull RouterAction routerAction, @NonNull Router router,
                                              @Nullable final Exception exception) {
                mParentFragmentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final String errorMsgToDisplay;
                        if (exception != null) {
                            final Throwable rootCause = Throwables.getRootCause(exception);
                            errorMsgToDisplay = "Error: " + (rootCause != null ? rootCause.getMessage() : "null");
                        } else {
                            errorMsgToDisplay = "Internal error! Please try again later.";
                        }
                        errorView.setText(errorMsgToDisplay);
                        errorView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(final View v) {
                                Toast.makeText(mParentFragmentActivity,
                                        exception != null ?
                                                ExceptionUtils.getRootCauseMessage(exception) : errorMsgToDisplay,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                        errorView.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        button.setEnabled(true);
                    }
                });
            }

            @Override
            public void notifyRouterActionProgress(@NonNull RouterAction routerAction, @NonNull Router router,
                                                   final int progress, final String partialOutput) {
                mParentFragmentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progress <= 0) {
                            outputView.setText(null);
                            return;
                        }
                        outputView.append(partialOutput);
                    }
                });
            }
        };

        pingEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    errorView.setVisibility(View.GONE);

                    final String textToFind = pingEditText.getText().toString();
                    if (isNullOrEmpty(textToFind)) {
                        //extra-check, even though we can be pretty sure the button is enabled only if textToFind is present
                        return true;
                    }

                    if (!(Patterns.IP_ADDRESS.matcher(textToFind).matches()
                            || Patterns.DOMAIN_NAME.matcher(textToFind).matches())) {
                        errorView.setText(mParentFragmentActivity.getResources()
                                .getString(R.string.router_add_dns_or_ip_invalid) + ":" + textToFind);
                        errorView.setVisibility(View.VISIBLE);
                        pingEditText.requestFocus();
                        openKeyboard(pingEditText);
                        return true;
                    }

                    final String existingSearch = mParentFragmentPreferences != null ?
                            mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_HOST), null) : null;

                    if (mParentFragmentPreferences != null) {
                        if (!textToFind.equalsIgnoreCase(existingSearch)) {
                            final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
                            editor.putString(getFormattedPrefKey(LAST_HOST), textToFind);
                            editor.apply();
                        }
                    }

                    //Run ping command
                    progressBar.setVisibility(View.VISIBLE);
                    button.setEnabled(false);

                    new PingFromRouterAction(mParentFragmentActivity, routerActionListener, mGlobalPreferences, textToFind).execute(mRouter);

                    return true;
                }
                return false;
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorView.setVisibility(View.GONE);
                final String textToFind = pingEditText.getText().toString();
                if (isNullOrEmpty(textToFind)) {
                    //extra-check, even though we can be pretty sure the button is enabled only if textToFind is present
                    return;
                }

                if (!(Patterns.IP_ADDRESS.matcher(textToFind).matches()
                        || Patterns.DOMAIN_NAME.matcher(textToFind).matches())) {
                    errorView.setText(mParentFragmentActivity.getResources()
                            .getString(R.string.router_add_dns_or_ip_invalid) + ":" + textToFind);
                    errorView.setVisibility(View.VISIBLE);
                    pingEditText.requestFocus();
                    openKeyboard(pingEditText);
                    return;
                }

                final String existingSearch = mParentFragmentPreferences != null ?
                        mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_HOST), null) : null;

                if (mParentFragmentPreferences != null) {
                    if (!textToFind.equalsIgnoreCase(existingSearch)) {
                        final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
                        editor.putString(getFormattedPrefKey(LAST_HOST), textToFind);
                        editor.apply();
                    }
                }

                //Run ping command
                progressBar.setVisibility(View.VISIBLE);
                button.setEnabled(false);

                new PingFromRouterAction(mParentFragmentActivity, routerActionListener, mGlobalPreferences, textToFind).execute(mRouter);
            }
        });

    }

    private void openKeyboard(final TextView mTextView) {
        final InputMethodManager imm = (InputMethodManager)
                mParentFragmentActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // only will trigger it if no physical keyboard is open
            imm.showSoftInput(mTextView, 0);
        }
    }

    @Override
    public int getTileHeaderViewId() {
        return R.id.tile_toolbox_ping_title;
    }

    @Override
    public int getTileTitleViewId() {
        return -1;
    }

    @Nullable
    @Override
    protected Loader<None> getLoader(int id, Bundle args) {
        return null;
    }

    @Nullable
    @Override
    protected String getLogTag() {
        return null;
    }

    @Nullable
    @Override
    protected OnClickIntent getOnclickIntent() {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<None> loader, None data) {

    }

    @Override
    public boolean isEmbeddedWithinScrollView() {
        return false;
    }
}
