package org.rm3l.router_companion.tiles.toolbox

import android.os.Bundle
import androidx.fragment.app.Fragment
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.actions.MACOUILookupAction
import org.rm3l.router_companion.resources.conn.Router

class ToolboxMACOUITile(
    parentFragment: Fragment,
    arguments: Bundle?,
    router: Router?
) : AbstractToolboxTile(parentFragment, arguments, router) {

    override fun checkInputAnReturnErrorMessage(inputText: String): CharSequence? {
        // Accept all inputs
        return null
    }

    override fun getEditTextHint() = R.string.oui_lookup_edit_text_hint

    override fun getInfoText() = R.string.oui_lookup_info

    override fun getRouterAction(textToFind: String) = MACOUILookupAction(
            mRouter, mParentFragmentActivity, mRouterActionListener,
            mGlobalPreferences, textToFind
        )

    override fun getSubmitButtonText() = R.string.toolbox_oui_lookup_submit

    override fun getTileTitle() = R.string.oui_lookup

    override fun isGeoLocateButtonEnabled() = false
}