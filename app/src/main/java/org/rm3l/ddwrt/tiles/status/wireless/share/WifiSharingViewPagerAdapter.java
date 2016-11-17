package org.rm3l.ddwrt.tiles.status.wireless.share;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;

import org.rm3l.ddwrt.tiles.status.wireless.share.nfc.WifiSharingNfcFragment;
import org.rm3l.ddwrt.tiles.status.wireless.share.qrcode.WifiSharingQrCodeFragment;

import java.io.Serializable;

/**
 * Created by rm3l on 11/11/2016.
 */

public class WifiSharingViewPagerAdapter extends FragmentStatePagerAdapter {

    private final Context mContext;

    private final WifiSharingData mWifiSharingData;

    public WifiSharingViewPagerAdapter(AppCompatActivity activity,
                                       String mRouterUuid, String mSsid,
                                       String mWifiEncType, String mWifiPassword) {
        super(activity.getSupportFragmentManager());
        this.mContext = activity;
        this.mWifiSharingData =
                new WifiSharingData(mRouterUuid, mSsid, mWifiEncType, mWifiPassword);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0 :
                return WifiSharingQrCodeFragment.newInstance(mWifiSharingData);
            case 1:
                return WifiSharingNfcFragment.newInstance(mWifiSharingData);
            default:
                break;
        }
        throw new IllegalStateException("position is @" + position);
    }

    @Override
    public int getCount() {
        // NFC isn't available on the device - just QR Code
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
            return 1;
        }
        return 2; //QR-Code + NFC
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
