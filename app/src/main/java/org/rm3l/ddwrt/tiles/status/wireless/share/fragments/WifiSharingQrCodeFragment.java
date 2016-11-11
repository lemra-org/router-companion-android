package org.rm3l.ddwrt.tiles.status.wireless.share.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.rm3l.ddwrt.R;

/**
 * Created by rm3l on 11/11/2016.
 */

public class WifiSharingQrCodeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_wifi_sharing_qrcode, container, false);
    }

}
