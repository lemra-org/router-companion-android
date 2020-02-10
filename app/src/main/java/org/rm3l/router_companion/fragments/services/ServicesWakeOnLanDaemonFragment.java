package org.rm3l.router_companion.fragments.services;

import android.os.Bundle;
import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import org.rm3l.router_companion.fragments.AbstractBaseFragment;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.services.wol.WakeOnLanDaemonTile;

/** Created by rm3l on 10/04/15. */
public class ServicesWakeOnLanDaemonFragment extends AbstractBaseFragment {

  @Nullable
  @Override
  protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
    return Arrays.<DDWRTTile>asList(new WakeOnLanDaemonTile(this, savedInstanceState, this.router));
  }
}
