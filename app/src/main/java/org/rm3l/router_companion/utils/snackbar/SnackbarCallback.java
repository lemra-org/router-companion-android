package org.rm3l.router_companion.utils.snackbar;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;

/**
 * Created by rm3l on 09/12/15.
 */
public interface SnackbarCallback {

    /**
     * Snackbar was dismissed via an action click
     *
     * @param event  Event is {@link Snackbar.Callback#DISMISS_EVENT_ACTION}
     * @param bundle data passed to the callback
     */
    default void onDismissEventActionClick(int event, @Nullable final Bundle bundle) throws Exception {}

    /**
     * Snackbar was dismissed from a new Snackbar being shown
     *
     * @param event  Event is {@link Snackbar.Callback#DISMISS_EVENT_CONSECUTIVE}
     * @param bundle data passed to the callback
     */
    default void onDismissEventConsecutive(int event, @Nullable final Bundle bundle) throws Exception {}

    /**
     * Snackbar was dismissed via a call to
     * {@link Snackbar#dismiss()}
     *
     * @param event  Event is {@link Snackbar.Callback#DISMISS_EVENT_MANUAL}
     * @param bundle data passed to the callback
     */
    default void onDismissEventManual(int event, @Nullable final Bundle bundle) throws Exception {}

    /**
     * Snackbar was dismissed via a swipe
     *
     * @param event  Event is {@link Snackbar.Callback#DISMISS_EVENT_SWIPE}
     * @param bundle data passed to the callback
     */
    default void onDismissEventSwipe(int event, @Nullable final Bundle bundle) throws Exception {}

    /**
     * Snackbar was dismissed via a timeout
     *
     * @param event Event is {@link Snackbar.Callback#DISMISS_EVENT_TIMEOUT}
     * @param token data passed to the callback
     */
    default void onDismissEventTimeout(int event, @Nullable final Bundle token) throws Exception {}

    /**
     * Snackbar is now visible
     *
     * @param bundle data passed to the callback
     */
    default void onShowEvent(@Nullable final Bundle bundle) throws Exception {}
}
