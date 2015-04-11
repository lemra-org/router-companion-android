package org.rm3l.ddwrt.tiles.toolbox;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.actions.AbstractRouterAction;
import org.rm3l.ddwrt.actions.MACOUILookupAction;
import org.rm3l.ddwrt.resources.conn.Router;

public class ToolboxMACOUITile extends AbstractToolboxTile {

    public ToolboxMACOUITile(@NonNull Fragment parentFragment, @NonNull Bundle arguments, @Nullable Router router) {
        super(parentFragment, arguments, router);
    }

    @Nullable
    @Override
    protected Integer getInfoText() {
        return R.string.oui_lookup_info;
    }

    @Override
    protected int getEditTextHint() {
        return R.string.oui_lookup_edit_text_hint;
    }

    @Override
    protected int getSubmitButtonText() {
        return R.string.toolbox_oui_lookup_submit;
    }

    @Override
    protected int getTileTitle() {
        return R.string.oui_lookup;
    }

    @Override
    @Nullable
    protected CharSequence checkInputAnReturnErrorMessage(@NonNull final String inputText) {
        //Accept all inputs
        return null;
    }

    @NonNull
    @Override
    protected AbstractRouterAction<?> getRouterAction(String textToFind) {
        return new MACOUILookupAction(mParentFragmentActivity, mRouterActionListener, mGlobalPreferences, textToFind);
    }
}