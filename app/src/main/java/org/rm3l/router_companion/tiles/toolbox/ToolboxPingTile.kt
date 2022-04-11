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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */
package org.rm3l.router_companion.tiles.toolbox

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import org.rm3l.ddwrt.R
import org.rm3l.router_companion.actions.AbstractRouterAction
import org.rm3l.router_companion.actions.PingFromRouterAction
import org.rm3l.router_companion.resources.conn.Router
import org.rm3l.router_companion.utils.ReportingUtils

class ToolboxPingTile(
    parentFragment: Fragment,
    arguments: Bundle?,
    router: Router?
) : AbstractToolboxTile(parentFragment, arguments, router) {

    init {
        layout.findViewById<View>(R.id.tile_toolbox_abstract_ping_packets_inputlayout).visibility = View.VISIBLE
    }

    override fun getEditTextHint() = R.string.ping_edit_text_hint

    override fun getInfoText() = R.string.ping_info

    override fun getRouterAction(textToFind: String): AbstractRouterAction<*> {
        val packetCountStr = (layout.findViewById<View>(R.id.tile_toolbox_abstract_ping_packets) as EditText).text
            .toString()
        var packetCount: Int? = null
        try {
            packetCount = Integer.parseInt(packetCountStr)
        } catch (e: Exception) {
            ReportingUtils.reportException(mParentFragmentActivity, e)
            // No worries
        }

        return PingFromRouterAction(
            mRouter, mParentFragmentActivity, mRouterActionListener,
            mGlobalPreferences, textToFind, packetCount
        )
    }

    override fun getSubmitButtonText() = R.string.toolbox_ping

    override fun getTileTitle() = R.string.ping
}
