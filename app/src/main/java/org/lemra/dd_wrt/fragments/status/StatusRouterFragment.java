package org.lemra.dd_wrt.fragments.status;

import android.os.Bundle;

import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.fragments.DDWRTBaseFragment;
import org.lemra.dd_wrt.tiles.DDWRTTile;
import org.lemra.dd_wrt.tiles.status.StatusRouterStateTile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by armel on 8/10/14.
 */
public class StatusRouterFragment extends DDWRTBaseFragment {

    private final List<DDWRTTile> tiles = new ArrayList<DDWRTTile>();

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach(Activity)} and before
     * {@link #onCreateView(LayoutInflater, ViewGroup, android.os.Bundle)}.
     * <p/>
     * <p>Note that this can be called while the fragment's activity is
     * still in the process of being created.  As such, you can not rely
     * on things like the activity's content view hierarchy being initialized
     * at this point.  If you want to do work once the activity itself is
     * created, see {@link #onActivityCreated(android.os.Bundle)}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Important to clear everything here!
        tiles.clear();
        tiles.add(new StatusRouterStateTile(getSherlockActivity(), getArguments()));
        tiles.add(new StatusRouterStateTile(getSherlockActivity(), getArguments()));

    }

    @Nullable
    @Override
    protected List<DDWRTTile> getTiles() {
        return this.tiles;
    }
}
