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

package org.rm3l.router_companion.fragments.admin

import android.os.Bundle
import java.util.Arrays
import org.rm3l.router_companion.fragments.AbstractBaseFragment
import org.rm3l.router_companion.tiles.DDWRTTile
import org.rm3l.router_companion.tiles.admin.commands.AdminCommandsTile

/**
 * 'Admin > Commands' fragment
 */
class AdminCommandsFragment : AbstractBaseFragment<Void>() {

    override fun getTiles(savedInstanceState: Bundle?) =
        listOf(AdminCommandsTile(this, savedInstanceState, this.router))

    //Disabled, as swipe refresh actually does not make sense in this kind of fragment
    override fun isSwipeRefreshLayoutEnabled() = false
}