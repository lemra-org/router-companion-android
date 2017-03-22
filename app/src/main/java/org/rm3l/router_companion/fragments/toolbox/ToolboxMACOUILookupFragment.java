package org.rm3l.router_companion.fragments.toolbox;

import android.os.Bundle;
import android.support.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.toolbox.ToolboxMACOUITile;

public class ToolboxMACOUILookupFragment extends AbstractToolboxFragment {

  @Nullable @Override protected List<DDWRTTile> doGetTiles(@Nullable Bundle savedInstanceState) {
    return Arrays.<DDWRTTile>asList(new ToolboxMACOUITile(this, savedInstanceState, this.router));
  }
}

