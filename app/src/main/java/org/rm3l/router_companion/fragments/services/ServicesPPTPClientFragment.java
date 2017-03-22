package org.rm3l.router_companion.fragments.services;

import android.os.Bundle;
import android.support.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import org.rm3l.router_companion.fragments.AbstractBaseFragment;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.services.vpn.client.PPTPClientTile;

/**
 * Created by rm3l on 06/09/15.
 */
public class ServicesPPTPClientFragment extends AbstractBaseFragment {

  @Nullable @Override protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
    return Collections.<DDWRTTile>singletonList(
        new PPTPClientTile(this, savedInstanceState, this.router));
  }
}
