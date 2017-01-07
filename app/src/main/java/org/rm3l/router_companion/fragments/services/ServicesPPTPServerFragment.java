package org.rm3l.router_companion.fragments.services;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.rm3l.router_companion.fragments.AbstractBaseFragment;
import org.rm3l.router_companion.tiles.DDWRTTile;
import org.rm3l.router_companion.tiles.services.vpn.server.PPTPServerTile;

import java.util.Collections;
import java.util.List;

/**
 * Created by rm3l on 06/09/15.
 */
public class ServicesPPTPServerFragment extends AbstractBaseFragment {

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles(@Nullable Bundle savedInstanceState) {
        return Collections.<DDWRTTile> singletonList(
                new PPTPServerTile(this, savedInstanceState, this.router));
    }
}
