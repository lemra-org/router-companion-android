package org.rm3l.router_companion.tiles.status.wireless.share.nfc;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.rm3l.router_companion.BuildConfig;
import org.rm3l.router_companion.R;
import org.rm3l.router_companion.tiles.status.wireless.share.WifiSharingActivity;
import org.rm3l.router_companion.tiles.status.wireless.share.WifiSharingViewPagerAdapter.WifiSharingData;
import org.rm3l.router_companion.utils.Utils;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.tiles.status.wireless.WirelessIfaceTile.WirelessEncryptionTypeForQrCode.WEP;
import static org.rm3l.router_companion.tiles.status.wireless.share.WifiSharingActivity.WIFI_SHARING_DATA;

/**
 * Created by rm3l on 11/11/2016.
 */

public class WifiSharingNfcFragment extends Fragment {

    private Button writeTagButton;
    private TextView nfcStatusTextView;
    private Button nfcSettingsButton;
    private String wifiPassword;
    private String mSsid;
    private String wifiEncryptionType;

    public static Fragment newInstance(WifiSharingData wifiSharingData) {
        final WifiSharingNfcFragment fragment = new WifiSharingNfcFragment();
        final Bundle args = new Bundle();
        args.putSerializable(WIFI_SHARING_DATA, wifiSharingData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.activity_wifi_sharing_nfc, container, false);

        final FragmentActivity activity = getActivity();

        final WifiSharingData wifiSharingData =
                (WifiSharingData) getArguments().getSerializable(WIFI_SHARING_DATA);

        mSsid = nullToEmpty(wifiSharingData.mSsid);
        wifiEncryptionType = wifiSharingData.mWifiEncType;
        wifiPassword = nullToEmpty(wifiSharingData.mWifiPassword);

        writeTagButton = (Button) rootView.findViewById(R.id.nfc_write_button);
        writeTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BuildConfig.DONATIONS || BuildConfig.WITH_ADS) {
                    //Download the full version to unlock this version
                    Utils.displayUpgradeMessage(activity, "Write Wi-Fi to NFC tag");
                    return;
                }
//                ((WifiSharingActivity) activity).enableTagWriteMode();
                if (wifiEncryptionType == null || (isNullOrEmpty(mSsid) && wifiPassword == null)) {
                    //menu item should have been disabled, but anyways, you never know :)
                    Toast.makeText(activity,
                            "Missing parameters to write NFC tag - try again later", Toast.LENGTH_SHORT).show();
                    return;
                }
//                new WriteWifiConfigToNfcDialog(activity,
//                        mSsid, nullToEmpty(wifiPassword)).show();

                ((WifiSharingActivity) getActivity()).enableTagWriteMode();
            }
        });

        nfcStatusTextView = (TextView) rootView.findViewById(R.id.nfc_status);

        nfcSettingsButton = (Button) rootView.findViewById(R.id.open_nfc_settings);
        nfcSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                } else {
                    intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                }
                startActivity(intent);
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (WEP.name().equalsIgnoreCase(wifiEncryptionType)) {
            writeTagButton.setEnabled(false);
            nfcStatusTextView.setText(R.string.error_wep_to_nfc_not_supported);
            nfcStatusTextView.setVisibility(View.VISIBLE);
        } else {
            final FragmentActivity activity = getActivity();
            boolean isNfcAvailable = ((WifiSharingActivity) activity).isNfcAvailable();
            boolean isNfcEnabled = (((WifiSharingActivity) activity).isNfcEnabled());

            if (!isNfcAvailable) {
                setNfcStateAvailable(false);
            } else if (!isNfcEnabled) {
                setNfcStateEnabled(false);
            } else {
                setNfcStateAvailable(true);
                setNfcStateEnabled(true);
            }
        }
    }

    public void setNfcStateEnabled(boolean enabled) {
        writeTagButton.setEnabled(enabled);
        if (enabled) {
            nfcSettingsButton.setVisibility(View.GONE);
            nfcStatusTextView.setVisibility(View.GONE);
            nfcStatusTextView.setText(null);
        } else {
            nfcStatusTextView.setText(R.string.error_turn_nfc_on);
            nfcStatusTextView.setVisibility(View.VISIBLE);
            nfcSettingsButton.setVisibility(View.VISIBLE);
        }
    }

    public void setNfcStateAvailable(boolean available) {
        writeTagButton.setEnabled(available);
        if (available) {
            nfcStatusTextView.setVisibility(View.GONE);
            nfcStatusTextView.setText(null);
        } else {
            nfcStatusTextView.setText(R.string.error_nfc_not_available);
            nfcStatusTextView.setVisibility(View.VISIBLE);
        }
    }

}
