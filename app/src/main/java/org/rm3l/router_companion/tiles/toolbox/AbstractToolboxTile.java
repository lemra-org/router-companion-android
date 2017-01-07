/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */
package org.rm3l.router_companion.tiles.toolbox;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Throwables;

import org.rm3l.router_companion.BuildConfig;
import org.rm3l.router_companion.R;
import org.rm3l.router_companion.actions.AbstractRouterAction;
import org.rm3l.router_companion.actions.ActionManager;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterStreamActionListener;
import org.rm3l.router_companion.mgmt.RouterManagementActivity;
import org.rm3l.router_companion.resources.None;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.dashboard.network.IPGeoActivity;
import org.rm3l.router_companion.utils.Utils;

import java.util.HashSet;
import java.util.Set;

import static android.widget.TextView.BufferType.EDITABLE;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.router_companion.utils.DDWRTCompanionConstants.EMPTY_STRING;

public abstract class AbstractToolboxTile extends DDWRTTile<None> {

    public static final String LAST_HOSTS = "lastHosts";
    private static final String LAST_HOST = "lastHost";
    protected final RouterStreamActionListener mRouterActionListener;

    private AbstractRouterAction<?> mCurrentRouterActionTask = null;

    public AbstractToolboxTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments,
                               @Nullable Router router) {
        super(parentFragment, arguments, router, R.layout.tile_toolbox_abstract, null);

        ((TextView) layout.findViewById(R.id.tile_toolbox_abstract_title))
                .setText(this.getTileTitle());

        final TextView infoTextView = (TextView) layout.findViewById(R.id.tile_toolbox_abstract_info);
        final Integer infoText = this.getInfoText();
        if (infoText == null) {
            infoTextView.setVisibility(View.GONE);
        } else {
            infoTextView.setVisibility(View.VISIBLE);
            infoTextView.setText(infoText);
        }

        final Button button = (Button) layout.findViewById(R.id.tile_toolbox_abstract_submit_button);
        button.setText(this.getSubmitButtonText());

        final Button geolocateButton = (Button)
                layout.findViewById(R.id.tile_toolbox_abstract_geolocate_button);

        final Button cancelButton = (Button) layout.findViewById(R.id.tile_toolbox_abstract_cancel_button);

        final View progressBar = layout.findViewById(R.id.tile_toolbox_ping_abstract_loading_view);

        final TextView outputView = (TextView) layout.findViewById(R.id.tile_toolbox_abstract_content);

        //Handle for Search EditText
        final AutoCompleteTextView editText = (AutoCompleteTextView) this.layout
                .findViewById(R.id.tile_toolbox_abstract_edittext);
        editText.setHint(this.getEditTextHint());
        //Initialize with existing search data
        final Set<String> lastHosts = mParentFragmentPreferences != null ? mParentFragmentPreferences
                .getStringSet(LAST_HOSTS, new HashSet<String>()) : new HashSet<String>();
        editText
                .setAdapter(new ArrayAdapter<>(mParentFragmentActivity, android.R.layout.simple_list_item_1,
                        lastHosts.toArray(new String[lastHosts.size()])));
        editText.setText(mParentFragmentPreferences != null ?
                mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_HOST), EMPTY_STRING) : EMPTY_STRING, EDITABLE);

        final TextView errorView = (TextView) layout.findViewById(R.id.tile_toolbox_abstract_error);

        geolocateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String textToFind = editText.getText().toString();
                if (isNullOrEmpty(textToFind)
                        || !(Patterns.IP_ADDRESS.matcher(textToFind).matches()
                                || Patterns.DOMAIN_NAME.matcher(textToFind).matches())) {
                    Toast.makeText(mParentFragmentActivity,
                            "Invalid host: '" + textToFind + "'", Toast.LENGTH_LONG).show();
                    return;
                }

                final Intent intent = new Intent(mParentFragmentActivity, IPGeoActivity.class);
                intent.putExtra(RouterManagementActivity.ROUTER_SELECTED, mRouter.getUuid());
                intent.putExtra(IPGeoActivity.PUBLIC_IP_TO_DISPLAY, textToFind);
                mParentFragmentActivity.startActivity(intent);
                mParentFragmentActivity.overridePendingTransition(
                        R.anim.right_in, R.anim.left_out);
            }
        });

        this.mRouterActionListener = new RouterStreamActionListener() {
            @Override
            public void onRouterActionSuccess(@NonNull RouterAction routerAction, @NonNull Router router, final Object returnData) {
                mParentFragmentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        button.setEnabled(true);
                        if (isGeoLocateButtonEnabled()) {
                            geolocateButton.setVisibility(View.VISIBLE);
                        } else {
                            geolocateButton.setVisibility(View.GONE);
                        }
                        cancelButton.setEnabled(false);
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
                            //noinspection ThrowableResultOfMethodCallIgnored
                            if (exception instanceof InterruptedException) {
                                errorMsgToDisplay = "Action Aborted.";
                            } else {
                                final Throwable rootCause = Throwables.getRootCause(exception);
                                errorMsgToDisplay = "Error: " + (rootCause != null ? rootCause.getMessage() : "null");
                            }
                        } else {
                            errorMsgToDisplay = "Internal error! Please try again later.";
                        }
                        errorView.setText(errorMsgToDisplay);
                        errorView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(final View v) {
                                Toast.makeText(mParentFragmentActivity,
                                        (exception == null || exception instanceof InterruptedException) ?
                                                errorMsgToDisplay :
                                                Utils.handleException(exception).first,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                        errorView.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        button.setEnabled(true);
                        geolocateButton.setVisibility(View.GONE);
                        cancelButton.setEnabled(false);
                    }
                });
            }

            @Override
            public void notifyRouterActionProgress(@NonNull RouterAction routerAction, @NonNull Router router,
                                                   final int progress, final String partialOutput) {
                mParentFragmentActivity.runOnUiThread(new Runnable() {
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
        };

        editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (editText.getRight() - editText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        errorView.setVisibility(View.GONE);
                        //'Clear' button - clear data, and reset everything out
                        //Reset everything
                        editText.setText(EMPTY_STRING);

                        if (mParentFragmentPreferences != null) {
                            final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
                            editor
                                    .putString(getFormattedPrefKey(LAST_HOST), EMPTY_STRING)
                                    .putStringSet(LAST_HOSTS, new HashSet<String>())
                                    .apply();
                        }

                        final Set<String> lastHosts = mParentFragmentPreferences != null ? mParentFragmentPreferences
                                .getStringSet(LAST_HOSTS, new HashSet<String>()) : new HashSet<String>();
                        editText
                                .setAdapter(new ArrayAdapter<>(mParentFragmentActivity, android.R.layout.simple_list_item_1,
                                        lastHosts.toArray(new String[lastHosts.size()])));
                        return true;
                    }
                }
                return false;
            }
        });

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    errorView.setVisibility(View.GONE);

                    final String textToFind = editText.getText().toString();
                    if (isNullOrEmpty(textToFind)) {
                        editText.requestFocus();
                        openKeyboard(editText);
                        return true;
                    }

                    if (!(Patterns.IP_ADDRESS.matcher(textToFind).matches()
                            || Patterns.DOMAIN_NAME.matcher(textToFind).matches())) {
                        errorView.setText(mParentFragmentActivity.getResources()
                                .getString(R.string.router_add_dns_or_ip_invalid) + ":" + textToFind);
                        errorView.setVisibility(View.VISIBLE);
                        editText.requestFocus();
                        openKeyboard(editText);
                        return true;
                    }

                    final String existingSearch = mParentFragmentPreferences != null ?
                            mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_HOST), null) : null;

                    if (mParentFragmentPreferences != null) {
                        final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
                        if (!textToFind.equalsIgnoreCase(existingSearch)) {
                            editor.putString(getFormattedPrefKey(LAST_HOST), textToFind);
                        }
                        final Set<String> mSharedPreferencesStringSet = new HashSet<>(mParentFragmentPreferences.getStringSet(LAST_HOSTS,
                                new HashSet<String>()));
                        if (!mSharedPreferencesStringSet.contains(textToFind)) {
                            mSharedPreferencesStringSet.add(textToFind);
                            editor.putStringSet(LAST_HOSTS, mSharedPreferencesStringSet);
                        }
                        editor.apply();

                    }
                    final Set<String> lastHosts = mParentFragmentPreferences != null ? mParentFragmentPreferences
                            .getStringSet(LAST_HOSTS, new HashSet<String>()) : new HashSet<String>();
                    editText
                            .setAdapter(new ArrayAdapter<>(mParentFragmentActivity, android.R.layout.simple_list_item_1,
                                    lastHosts.toArray(new String[lastHosts.size()])));

                    //Run ping command
                    progressBar.setVisibility(View.VISIBLE);
                    button.setEnabled(false);
                    geolocateButton.setVisibility(View.GONE);

                    mCurrentRouterActionTask = getRouterAction(textToFind);
                    ActionManager.runTasks(mCurrentRouterActionTask);

                    return true;
                }
                return false;
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorView.setVisibility(View.GONE);
                final String textToFind = editText.getText().toString();
                if (isNullOrEmpty(textToFind)) {
                    editText.requestFocus();
                    openKeyboard(editText);
                    return;
                }

                final CharSequence errorMessageIfAny = checkInputAnReturnErrorMessage(textToFind);
                if (errorMessageIfAny != null) {
                    errorView.setText(errorMessageIfAny);
                    errorView.setVisibility(View.VISIBLE);
                    editText.requestFocus();
                    openKeyboard(editText);
                    return;
                }

//                if (!(Patterns.IP_ADDRESS.matcher(textToFind).matches()
//                        || Patterns.DOMAIN_NAME.matcher(textToFind).matches())) {
//                    errorView.setText(mParentFragmentActivity.getResources()
//                            .getString(R.string.router_add_dns_or_ip_invalid) + ":" + textToFind);
//                    errorView.setVisibility(View.VISIBLE);
//                    editText.requestFocus();
//                    openKeyboard(editText);
//                    return;
//                }

                final String existingSearch = mParentFragmentPreferences != null ?
                        mParentFragmentPreferences.getString(getFormattedPrefKey(LAST_HOST), null) : null;

                if (mParentFragmentPreferences != null) {
                    if (!textToFind.equalsIgnoreCase(existingSearch)) {
                        final SharedPreferences.Editor editor = mParentFragmentPreferences.edit();
                        editor.putString(getFormattedPrefKey(LAST_HOST), textToFind);
                        editor.apply();
                    }
                }

                final Set<String> lastHosts = mParentFragmentPreferences != null ? mParentFragmentPreferences
                        .getStringSet(LAST_HOSTS, new HashSet<String>()) : new HashSet<String>();
                editText
                        .setAdapter(new ArrayAdapter<>(mParentFragmentActivity, android.R.layout.simple_list_item_1,
                                lastHosts.toArray(new String[lastHosts.size()])));

                //Run command
                progressBar.setVisibility(View.VISIBLE);
                button.setEnabled(false);
                geolocateButton.setVisibility(View.GONE);

                Utils.hideSoftKeyboard(mParentFragmentActivity);
                mCurrentRouterActionTask = getRouterAction(textToFind);
                ActionManager.runTasks(mCurrentRouterActionTask);

                cancelButton.setEnabled(true);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                geolocateButton.setVisibility(View.GONE);
                if (mCurrentRouterActionTask == null) {
                    return;
                }
                try {
                    mCurrentRouterActionTask.cancel();
                    Toast.makeText(mParentFragmentActivity, "Stopping...", Toast.LENGTH_SHORT)
                            .show();
                } catch (final Exception e) {
                    Utils.reportException(null, e);
                } finally {
                    progressBar.setVisibility(View.GONE);
                    button.setEnabled(true);
                    cancelButton.setEnabled(false);
                }
            }
        });
    }

    protected boolean isGeoLocateButtonEnabled() {
        return true;
    }

    public boolean canChildScrollUp() {
        final View contentScrollView = layout
                .findViewById(R.id.tile_toolbox_abstract_content_scrollview);
        final boolean canScrollVertically = ViewCompat.canScrollVertically(
                contentScrollView,
                -1);
        if (!canScrollVertically) {
            return canScrollVertically;
        }

        //TODO ScrollView can scroll vertically,
        // but detect whether the touch was done outside of the scroll view
        // (in which case we should return false)

        return canScrollVertically;
    }

    @Nullable
    protected CharSequence checkInputAnReturnErrorMessage(@NonNull final String inputText) {
        if (!(Patterns.IP_ADDRESS.matcher(inputText).matches()
                || Patterns.DOMAIN_NAME.matcher(inputText).matches())) {
            return (mParentFragmentActivity.getResources()
                    .getString(R.string.router_add_dns_or_ip_invalid) + ":" + inputText);
        }
        return null;
    }

    @Override
    public void onStop() {
        if (mCurrentRouterActionTask == null) {
            return;
        }
        try {
            mCurrentRouterActionTask.cancel();
        } catch (final Exception e) {
            Utils.reportException(null, e);
        } finally {
            layout.findViewById(R.id.tile_toolbox_ping_abstract_loading_view).setVisibility(View.GONE);
            layout.findViewById(R.id.tile_toolbox_abstract_submit_button).setEnabled(true);
            layout.findViewById(R.id.tile_toolbox_abstract_cancel_button).setEnabled(false);
        }
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
        return R.id.tile_toolbox_abstract_title;
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
//        return false;
        return BuildConfig.WITH_ADS && super.isEmbeddedWithinScrollView();
    }

    @Nullable
    protected abstract Integer getInfoText();

    protected abstract int getEditTextHint();

    protected abstract int getSubmitButtonText();

    protected abstract int getTileTitle();

    @NonNull
    protected abstract AbstractRouterAction<?> getRouterAction(String textToFind);

}
