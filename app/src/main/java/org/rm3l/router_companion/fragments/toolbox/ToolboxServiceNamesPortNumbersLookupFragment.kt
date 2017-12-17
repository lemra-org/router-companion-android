package org.rm3l.router_companion.fragments.toolbox

import android.os.Bundle
import org.rm3l.router_companion.tiles.toolbox.ToolboxServiceNamesPortNumbersTile

class ToolboxServiceNamesPortNumbersLookupFragment : AbstractToolboxFragment() {

    override fun doGetTiles(savedInstanceState: Bundle?) =
            listOf(
                    ToolboxServiceNamesPortNumbersTile(this, savedInstanceState?:Bundle.EMPTY, this.router))
}

