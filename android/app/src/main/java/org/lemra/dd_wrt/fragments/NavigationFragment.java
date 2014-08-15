package org.lemra.dd_wrt.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.actionbarsherlock.app.SherlockFragment;

import org.lemra.dd_wrt.R;

import java.util.Locale;

/**
 * Created by armel on 8/13/14.
 */
public class NavigationFragment extends SherlockFragment {

    public static final String ARG_SECTION_NUMBER = "section_number";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_navigation_item, container,
                false);
        final int i = getArguments().getInt(ARG_SECTION_NUMBER);
        final String section = getResources().getStringArray(R.array.navigation_drawer_items_array)[i];

        final int imageId = getResources().getIdentifier(
                section.toLowerCase(Locale.getDefault()), "drawable",
                getActivity().getPackageName());
        ((ImageView) rootView.findViewById(R.id.image))
                .setImageResource(imageId);
        getActivity().setTitle(section);
        return rootView;
    }

}