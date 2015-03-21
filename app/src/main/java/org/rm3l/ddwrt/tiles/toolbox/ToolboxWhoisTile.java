package org.rm3l.ddwrt.tiles.toolbox;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.AbstractRouterAction;
import org.rm3l.ddwrt.actions.WhoisFromLocalDeviceAction;
import org.rm3l.ddwrt.resources.conn.Router;

public class ToolboxWhoisTile extends AbstractToolboxTile {

    public ToolboxWhoisTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router);
    }

    @Nullable
    @Override
    protected Integer getInfoText() {
        return R.string.whois_info;
    }

    @Override
    protected int getEditTextHint() {
        return R.string.host_edit_text_hint;
    }

    @Override
    protected int getSubmitButtonText() {
        return R.string.toolbox_whois;
    }

    @Override
    protected int getTileTitle() {
        return R.string.whois;
    }

    @NonNull
    @Override
    protected AbstractRouterAction<?> getRouterAction(String textToFind) {
        return new WhoisFromLocalDeviceAction(mParentFragmentActivity, mRouterActionListener, mGlobalPreferences, textToFind);
    }
}
