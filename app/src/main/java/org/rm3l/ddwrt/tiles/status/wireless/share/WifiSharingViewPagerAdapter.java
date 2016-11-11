package org.rm3l.ddwrt.tiles.status.wireless.share;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.rm3l.ddwrt.tiles.status.wireless.share.fragments.WifiSharingNfcFragment;
import org.rm3l.ddwrt.tiles.status.wireless.share.fragments.WifiSharingQrCodeFragment;

/**
 * Created by rm3l on 11/11/2016.
 */

public class WifiSharingViewPagerAdapter extends FragmentStatePagerAdapter {

    public WifiSharingViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0 :
                return new WifiSharingQrCodeFragment();
            case 1:
                return new WifiSharingNfcFragment();
            default:
                break;
        }
        throw new IllegalStateException("position is @" + position);
    }

    @Override
    public int getCount() {
        return 2;           // As there are only 2 Tabs
    }
}
