package org.rm3l.ddwrt.tiles.status.wireless.share;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.rm3l.ddwrt.tiles.status.wireless.share.fragments.WifiSharingNfcFragment;
import org.rm3l.ddwrt.tiles.status.wireless.share.fragments.WifiSharingQrCodeFragment;

import java.io.Serializable;

/**
 * Created by rm3l on 11/11/2016.
 */

public class WifiSharingViewPagerAdapter extends FragmentStatePagerAdapter {

    private final WifiSharingData mWifiSharingData;

    public WifiSharingViewPagerAdapter(FragmentManager fm,
                                       String mRouterUuid, String mSsid,
                                       String mWifiEncType, String mWifiPassword) {
        super(fm);
        this.mWifiSharingData =
                new WifiSharingData(mRouterUuid, mSsid, mWifiEncType, mWifiPassword);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0 :
                return WifiSharingQrCodeFragment.newInstance(mWifiSharingData);
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

    public static final class WifiSharingData implements Serializable {

        public final String mRouterUuid;
        public final String mSsid;
        public final String mWifiEncType;
        public final String mWifiPassword;

        public WifiSharingData(String mRouterUuid, String mSsid, String mWifiEncType, String mWifiPassword) {
            this.mRouterUuid = mRouterUuid;
            this.mSsid = mSsid;
            this.mWifiEncType = mWifiEncType;
            this.mWifiPassword = mWifiPassword;
        }
    }
}
