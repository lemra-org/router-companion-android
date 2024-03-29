package org.rm3l.router_companion.tiles.status.wireless.share;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import be.brunoparmentier.wifikeyshare.utils.NfcUtils;
import java.io.Serializable;
import org.rm3l.router_companion.tiles.status.wireless.share.nfc.WifiSharingNfcFragment;
import org.rm3l.router_companion.tiles.status.wireless.share.qrcode.WifiSharingQrCodeFragment;

/** Created by rm3l on 11/11/2016. */
public class WifiSharingViewPagerAdapter extends FragmentStatePagerAdapter {

  public static final class WifiSharingData implements Serializable {

    public final String mRouterUuid;

    public final String mSsid;

    public final String mWifiEncType;

    public final String mWifiPassword;

    public WifiSharingData(
        String mRouterUuid, String mSsid, String mWifiEncType, String mWifiPassword) {
      this.mRouterUuid = mRouterUuid;
      this.mSsid = mSsid;
      this.mWifiEncType = mWifiEncType;
      this.mWifiPassword = mWifiPassword;
    }
  }

  private final Context mContext;

  private final WifiSharingData mWifiSharingData;

  public WifiSharingViewPagerAdapter(
      AppCompatActivity activity,
      String mRouterUuid,
      String mSsid,
      String mWifiEncType,
      String mWifiPassword) {
    super(activity.getSupportFragmentManager());
    this.mContext = activity;
    this.mWifiSharingData = new WifiSharingData(mRouterUuid, mSsid, mWifiEncType, mWifiPassword);
  }

  @Override
  public int getCount() {
    // NFC isn't available on the device - just QR Code
    if (!NfcUtils.hasNFCHardware(mContext)) {
      return 1;
    }
    return 2; // QR-Code + NFC
  }

  @Override
  public Fragment getItem(int position) {
    switch (position) {
      case 0:
        return WifiSharingQrCodeFragment.newInstance(mWifiSharingData);
      case 1:
        return WifiSharingNfcFragment.newInstance(mWifiSharingData);
      default:
        break;
    }
    throw new IllegalStateException("position is @" + position);
  }
}
