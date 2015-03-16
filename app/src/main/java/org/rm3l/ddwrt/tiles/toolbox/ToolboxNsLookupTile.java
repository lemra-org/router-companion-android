package org.rm3l.ddwrt.tiles.toolbox;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.AbstractRouterAction;
import org.rm3l.ddwrt.actions.NsLookupFromRouterAction;
import org.rm3l.ddwrt.resources.conn.Router;

public class ToolboxNsLookupTile extends AbstractToolboxTile {

    public ToolboxNsLookupTile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router);
    }

    @Override
    protected int getEditTextHint() {
        return R.string.nslookup_edit_text_hint;
    }

    @Override
    protected int getTileTitle() {
        return R.string.nslookup;
    }

    @NonNull
    @Override
    protected AbstractRouterAction getRouterAction(String textToFind) {
        return new NsLookupFromRouterAction(mParentFragmentActivity, mRouterActionListener, mGlobalPreferences, textToFind);
    }

    @Override
    protected int getSubmitButtonText() {
        return R.string.toolbox_nslookup;
    }

}
