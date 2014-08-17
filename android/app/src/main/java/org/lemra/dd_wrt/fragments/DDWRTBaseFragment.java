package org.lemra.dd_wrt.fragments;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.R;
import org.lemra.dd_wrt.api.conn.Router;
import org.lemra.dd_wrt.tiles.DDWRTTile;

import java.util.ArrayList;
import java.util.List;

import static android.widget.FrameLayout.LayoutParams;
import static org.lemra.dd_wrt.DDWRTManagementActivity.PlaceholderFragment.PARENT_SECTION_TITLE;

/**
 * Created by armel on 8/10/14.
 */
public abstract class DDWRTBaseFragment extends SherlockFragment {

    private static final String LOG_TAG = DDWRTBaseFragment.class.getSimpleName();

    public static final String TAB_TITLE = "fragment_tab_title";
    public static final String FRAGMENT_CLASS = "fragment_class";
    public static final String ROUTER_CONNECTION_INFO = "router_info";

    private CharSequence mTabTitle;

    private CharSequence mParentSectionTitle;

    private List<DDWRTTile> fragmentTiles;

    @Nullable
    protected Router router;

//    WeakReference<View> mView;

    @Nullable
    public static DDWRTBaseFragment newInstance(@NotNull final Class<? extends DDWRTBaseFragment> clazz,
                                                @NotNull final CharSequence parentSectionTitle, @NotNull final CharSequence tabTitle,
                                                @Nullable final Router router) {
        try {
            final DDWRTBaseFragment fragment = clazz.newInstance()
                    .setTabTitle(tabTitle)
                    .setParentSectionTitle(parentSectionTitle);

            Bundle args = new Bundle();
            args.putCharSequence(TAB_TITLE, tabTitle);
            args.putCharSequence(PARENT_SECTION_TITLE, parentSectionTitle);
            args.putString(FRAGMENT_CLASS, clazz.getCanonicalName());
            args.putSerializable(ROUTER_CONNECTION_INFO, router);
            fragment.setArguments(args);

            fragment.router = router;

            return fragment;

        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public final DDWRTBaseFragment setParentSectionTitle(@NotNull final CharSequence parentSectionTitle) {
        this.mParentSectionTitle = parentSectionTitle;
        return this;
    }

    public final  DDWRTBaseFragment setTabTitle(@NotNull final CharSequence tabTitle) {
        this.mTabTitle = tabTitle;
        return this;
    }

    public final CharSequence getTabTitle() {
        return mTabTitle;
    }

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach(Activity)} and before
     * {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}.
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
        this.fragmentTiles = this.getTiles();
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null (which
     * is the default implementation).  This will be called between
     * {@link #onCreate(android.os.Bundle)} and {@link #onActivityCreated(android.os.Bundle)}.
     * <p/>
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        final CharSequence parentSectionTitle = arguments.getCharSequence(PARENT_SECTION_TITLE);
        String tabTitle = arguments.getString(TAB_TITLE);

        Log.d(LOG_TAG, "onCreateView: " + parentSectionTitle + " > " + tabTitle);

        return this.getLayout();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //Android disagrees with that: "Can't retain fragments that are nested in other fragments"
//        // this is really important in order to save the state across screen
//        // configuration changes for example
//        setRetainInstance(true);

        // initiate the loaders to do the background work
        if (this.fragmentTiles != null && !this.fragmentTiles.isEmpty()) {
            final LoaderManager loaderManager = getLoaderManager();
            int i = 0;
            for (final DDWRTTile ddwrtTile : this.fragmentTiles) {
                loaderManager.initLoader(i++, null, ddwrtTile);
            }
        }
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.  The next time the fragment needs
     * to be displayed, a new view will be created.  This is called
     * after {@link #onStop()} and before {@link #onDestroy()}.  It is called
     * <em>regardless</em> of whether {@link #onCreateView} returned a
     * non-null view.  Internally it is called after the view's state has
     * been saved but before it has been removed from its parent.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.fragmentTiles != null && !this.fragmentTiles.isEmpty()) {
            final LoaderManager loaderManager = getLoaderManager();
            for (int i = 0; i < this.fragmentTiles.size(); i++) {
                loaderManager.destroyLoader(i);
            }
        }
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
//    @Override
//    public Loader<String> onCreateLoader(int id, Bundle args) {
//        final AsyncTaskLoader<String> loader = new AsyncTaskLoader<String>(getActivity()) {
//
//            @Override
//            public String loadInBackground() {
//                long i = 0l;
//                try {
//                    // TODO SSH Operation will run over here
//                    String msg;
//                    while(i < new Random().nextInt(10) + 10) {
//                        msg = "[Run #" + (++i) + "] I am " +
//                                DDWRTBaseFragment.this.getArguments().getString(FRAGMENT_CLASS) + " and I am doing time-consuming operations in the background\n";
//                        Log.d(LOG_TAG, msg);
//                        Thread.sleep(7000l);
//                    }
//                } catch (InterruptedException e) {
//                }
//
//                //FIXME Dummy texts
//                if (i % 5 == 0) {
//                    return "Spaces are the lieutenant commanders of the dead adventure.";
//                }
//
//                if (i % 5 == 1) {
//                    return "Est grandis nix, cesaris.";
//                }
//
//                if (i % 5 == 2) {
//                    return "Lotus, visitors, and separate lamas will always protect them.";
//                }
//
//                if (i % 5 == 3) {
//                    return "For a sliced quartered paste, add some bourbon and baking powder.";
//                }
//
//                if (i % 5 == 4) {
//                    return "The reef vandalizes with hunger, rob the galley before it rises.";
//                }
//
//                return null;
//            }
//        };
//        // somehow the AsyncTaskLoader doesn't want to start its job without
//        // calling this method
//        loader.forceLoad();
//        return loader;
//    }

//    @Override
//    public void onLoadFinished(Loader<String> loader, String data) {
//
//        //Use data over here
//        //
//        final View ddwrtSectionView = getDDWRTSectionView(getActivity(), getArguments());
//        if (ddwrtSectionView instanceof TextView) {
//            ((TextView) ddwrtSectionView).setText(isNullOrEmpty(data) ?
//                    getResources().getText(R.string.no_data) : data);
//        }
/*
// add the new item and let the adapter know in order to refresh the
		// views
		mItems.add(TabsFragment.TAB_WORDS.equals(mTag) ? WORDS[mPosition]
				: NUMBERS[mPosition]);
		mAdapter.notifyDataSetChanged();

		// advance in your list with one step
		mPosition++;
		if (mPosition < mTotal - 1) {
			getLoaderManager().restartLoader(0, null, this);
			Log.d(TAG, "onLoadFinished(): loading next...");
		} else {
			Log.d(TAG, "onLoadFinished(): done loading!");
		}
 */
//    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
//     * @param loader The Loader that is being reset.
     */
//    @Override
//    public void onLoaderReset(Loader<String> loader) {
//        final View ddwrtSectionView = getDDWRTSectionView(getActivity(), getArguments());
//        if (ddwrtSectionView instanceof TextView) {
//            ((TextView) ddwrtSectionView).setText(getResources().getText(R.string.no_data));
//        }
//    }

    @NotNull
    private ViewGroup getLayout() {
        final LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        final int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources()
                .getDisplayMetrics());
        params.setMargins(margin, margin, margin, margin);

        ViewGroup viewGroup = null;

        boolean atLeastOneTileAdded = false;

        if (this.fragmentTiles != null && !this.fragmentTiles.isEmpty()) {

            final LayoutInflater layoutInflater = getSherlockActivity().getLayoutInflater();
            viewGroup = (ScrollView) layoutInflater.inflate(R.layout.base_tiles_container_scrollview, null);

            final List<TableRow> rows = new ArrayList<TableRow>();

            final TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
            for (final DDWRTTile ddwrtTile : this.fragmentTiles) {
                final ViewGroup viewGroupLayout = ddwrtTile.getViewGroupLayout();
                atLeastOneTileAdded |= (viewGroupLayout != null);

                if (viewGroupLayout == null) {
                    continue;
                }

                final TableRow tableRow = new TableRow(getSherlockActivity());
                tableRow.setOnClickListener(ddwrtTile);
                viewGroupLayout.setOnClickListener(ddwrtTile);
                tableRow.setLayoutParams(tableRowParams);
                tableRow.addView(viewGroupLayout);

                rows.add(tableRow);
            }

            atLeastOneTileAdded = (!rows.isEmpty());

            Log.d(LOG_TAG, "atLeastOneTileAdded: "+atLeastOneTileAdded+", rows: "+rows.size());

            if (atLeastOneTileAdded) {
                //Drop Everything
//                viewGroup.removeAllViews();

//                final TableLayout tableLayout = new TableLayout(getSherlockActivity());

                final TableLayout tableLayout = (TableLayout) viewGroup.findViewById(R.id.tiles_container_scrollview_table);

//                tableLayout.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
//                tableLayout.setStretchAllColumns(true);
                tableLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                for (final TableRow row : rows) {
                    tableLayout.addView(row);
                }

//                viewGroup.addView(tableLayout);
            }

            ((ScrollView) viewGroup).setFillViewport(true);
        }

        if (viewGroup == null || !atLeastOneTileAdded) {
            viewGroup = new FrameLayout(getSherlockActivity());
            final TextView view = new TextView(getSherlockActivity());
            view.setGravity(Gravity.CENTER);
            view.setText(getResources().getString(R.string.no_data));
            view.setBackgroundResource(R.drawable.background_card);
            view.setLayoutParams(params);

            viewGroup.addView(view);
        }

        viewGroup.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        viewGroup.setLayoutParams(params);

        return viewGroup;

    }

    @Nullable
    protected abstract List<DDWRTTile> getTiles();

}
