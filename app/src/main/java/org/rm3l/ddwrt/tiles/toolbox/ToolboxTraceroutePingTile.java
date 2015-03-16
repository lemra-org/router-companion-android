package org.rm3l.ddwrt.tiles.toolbox;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.AbstractRouterAction;
import org.rm3l.ddwrt.actions.TracerouteFromRouterAction;
import org.rm3l.ddwrt.resources.conn.Router;

public class ToolboxTraceroutePingTile extends AbstractToolboxTile {

    public ToolboxTraceroutePingTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router);

    }

    @Override
    protected int getEditTextHint() {
        return R.string.traceroute_edit_text_hint;
    }

    @Override
    protected int getSubmitButtonText() {
        return R.string.toolbox_traceroute;
    }

    @Override
    protected int getTileTitle() {
        return R.string.traceroute;
    }

    @NonNull
    @Override
    protected AbstractRouterAction<?> getRouterAction(String textToFind) {
        return new TracerouteFromRouterAction(mParentFragmentActivity, mRouterActionListener, mGlobalPreferences, textToFind);
    }
}
