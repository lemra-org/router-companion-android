package org.lemra.dd_wrt.fragments;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lemra.dd_wrt.R;

import java.util.Random;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.lemra.dd_wrt.DDWRTManagementActivity.PlaceholderFragment.PARENT_SECTION_TITLE;

/**
 * Created by armel on 8/10/14.
 */
public abstract class DDWRTBaseFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<String> {

    private static final String LOG_TAG = DDWRTBaseFragment.class.getSimpleName();

    public static final String TAB_TITLE = "fragment_tab_title";
    public static final String FRAGMENT_CLASS = "fragment_class";

    private CharSequence mTabTitle;

    private CharSequence mParentSectionTitle;
    private View mLoadingView;

    @Nullable
    public static DDWRTBaseFragment newInstance(@NotNull final Class<? extends DDWRTBaseFragment> clazz,
                                                @NotNull final CharSequence parentSectionTitle, @NotNull final CharSequence tabTitle) {
        try {
            final DDWRTBaseFragment fragment = clazz.newInstance()
                    .setTabTitle(tabTitle)
                    .setParentSectionTitle(parentSectionTitle);

            Bundle args = new Bundle();
            args.putCharSequence(TAB_TITLE, tabTitle);
            args.putCharSequence(PARENT_SECTION_TITLE, parentSectionTitle);
            args.putString(FRAGMENT_CLASS, clazz.getCanonicalName());
            fragment.setArguments(args);

            return fragment;

        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DDWRTBaseFragment setParentSectionTitle(@NotNull final CharSequence parentSectionTitle) {
        this.mParentSectionTitle = parentSectionTitle;
        return this;
    }

    public DDWRTBaseFragment setTabTitle(@NotNull final CharSequence tabTitle) {
        this.mTabTitle = tabTitle;
        return this;
    }

    public CharSequence getTabTitle() {
        return mTabTitle;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Bundle arguments = getArguments();
        final CharSequence parentSectionTitle = arguments.getCharSequence(PARENT_SECTION_TITLE);
        String tabTitle = arguments.getString(TAB_TITLE);

        Log.d(LOG_TAG, "onCreateView #" + parentSectionTitle + " > " + tabTitle);

        final String text = getResources().getString(R.string.app_name) + " > " +
                parentSectionTitle + " > " + tabTitle;
//        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

        FrameLayout fl = new FrameLayout(getActivity());
        fl.setLayoutParams(params);

        final int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources()
                .getDisplayMetrics());

        mLoadingView = new TextView(getActivity());
        params.setMargins(margin, margin, margin, margin);
        mLoadingView.setLayoutParams(params);
        mLoadingView.setLayoutParams(params);
        ((TextView) mLoadingView).setGravity(Gravity.CENTER);
        mLoadingView.setBackgroundResource(R.drawable.background_card);
        ((TextView) mLoadingView).setText(text);

        fl.addView(mLoadingView);
        return fl;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //Android disagrees with that: "Can't retain fragments that are nested in other fragments"
//        // this is really important in order to save the state across screen
//        // configuration changes for example
//        setRetainInstance(true);

        // initiate the loader to do the background work
        getLoaderManager().initLoader(0, null, this);
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<String> onCreateLoader(int id, Bundle args) {
        final AsyncTaskLoader<String> loader = new AsyncTaskLoader<String>(getActivity()) {

            @Override
            public String loadInBackground() {
                long i = 0l;
                try {
                    // TODO SSH Operation will run over here
                    String msg;
                    while(i < new Random().nextInt(10) + 10) {
                        msg = "[Run #" + (++i) + "] I am " +
                                DDWRTBaseFragment.this.getArguments().getString(FRAGMENT_CLASS) + " and I am doing time-consuming operations in the background\n";
                        Log.d(LOG_TAG, msg);
                        Thread.sleep(7000l);
                    }
                } catch (InterruptedException e) {
                }

                //FIXME Dummy texts
                if (i % 5 == 0) {
                    return "Spaces are the lieutenant commanders of the dead adventure.";
                }

                if (i % 5 == 1) {
                    return "Est grandis nix, cesaris.";
                }

                if (i % 5 == 2) {
                    return "Lotus, visitors, and separate lamas will always protect them.";
                }

                if (i % 5 == 3) {
                    return "For a sliced quartered paste, add some bourbon and baking powder.";
                }

                if (i % 5 == 4) {
                    return "The reef vandalizes with hunger, rob the galley before it rises.";
                }

                return null;
            }
        };
        // somehow the AsyncTaskLoader doesn't want to start its job without
        // calling this method
        loader.forceLoad();
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<String> loader, String data) {

        //Use data over here
        //
        if (mLoadingView != null) {
            ((TextView) mLoadingView).setText(isNullOrEmpty(data) ?
                    getResources().getText(R.string.no_data) : data);
        }
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
    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<String> loader) {
        if (mLoadingView != null) {
            ((TextView) mLoadingView).setText(getResources().getText(R.string.no_data));
        }
    }
}
