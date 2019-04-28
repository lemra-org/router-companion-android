package org.rm3l.router_companion.tiles.status.wireless.share.nfc;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import be.brunoparmentier.wifikeyshare.model.WifiAuthType;
import be.brunoparmentier.wifikeyshare.model.WifiNetwork;
import be.brunoparmentier.wifikeyshare.utils.NfcUtils;
import com.crashlytics.android.Crashlytics;
import java.util.Arrays;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.tiles.status.wireless.share.WifiSharingActivity;
import org.rm3l.router_companion.utils.ReportingUtils;

/**
 * Created by rm3l on 10/11/2016.
 */
public class WriteWifiConfigToNfcDialog extends AlertDialog implements View.OnClickListener {

    public static final String ENC_TYPE = "ENC_TYPE";

    private static final String TAG = WriteWifiConfigToNfcDialog.class.getSimpleName();

    private static final String NETWORK_ID = "NETWORK_ID";

    private static final String SECURITY = "SECURITY";

    private Button mCancelButton;

    private final Activity mContext;

    private TextView mLabelView;

    private final String mPassword;

    private ProgressBar mProgressBar;

    private final String mSsid;

    private Button mSubmitButton;

    private View mView;

    private final PowerManager.WakeLock mWakeLock;

    private final String mWifiEncType;

    public WriteWifiConfigToNfcDialog(Activity context, final String ssid, final String password,
            final String mWifiEncType) {
        super(context);
        mContext = context;
        mWakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE)).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "WriteWifiConfigToNfcDialog:wakeLock");
        mSsid = ssid;
        mPassword = password;
        this.mWifiEncType = mWifiEncType;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.write_wifi_config_to_nfc, null);

        setView(mView);
        //        setInverseBackgroundForced(true);
        setTitle(R.string.write_to_nfc_tag);
        setCancelable(false);
        setButton(DialogInterface.BUTTON_NEUTRAL, mContext.getResources().getString(R.string.write_tag),
                (OnClickListener) null);
        setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getResources().getString(R.string.cancel),
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ((WifiSharingActivity) mContext).disableTagWriteMode();
                        dialogInterface.dismiss();
                    }
                });

        mLabelView = (TextView) mView.findViewById(R.id.password_label);
        mProgressBar = (ProgressBar) mView.findViewById(R.id.progress_bar);

        super.onCreate(savedInstanceState);

        mSubmitButton = getButton(DialogInterface.BUTTON_NEUTRAL);
        mSubmitButton.setOnClickListener(this);
        mSubmitButton.setEnabled(true);
        mSubmitButton.performClick();

        mCancelButton = getButton(DialogInterface.BUTTON_NEGATIVE);
    }

    @Override
    public void dismiss() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        super.dismiss();
    }

    @Override
    public void onClick(View v) {
        mWakeLock.acquire();

        final NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(mContext);

        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(mContext, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            dismiss();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            nfcAdapter.enableReaderMode(mContext, new NfcAdapter.ReaderCallback() {
                @Override
                public void onTagDiscovered(Tag tag) {
                    handleWriteNfcEvent(tag);
                }
            }, NfcAdapter.FLAG_READER_NFC_A
                    | NfcAdapter.FLAG_READER_NFC_B
                    | NfcAdapter.FLAG_READER_NFC_BARCODE
                    | NfcAdapter.FLAG_READER_NFC_F
                    | NfcAdapter.FLAG_READER_NFC_V, null);
        } else {
            //Pre-KK devices
            final WifiNetwork wifiNetwork =
                    new WifiNetwork(mSsid, WifiAuthType.valueOf(mWifiEncType), mPassword, false);
            nfcAdapter.setNdefPushMessage(NfcUtils.generateNdefMessage(wifiNetwork), mContext);
            //            //TODO Pre-KK devices
            //            // Stop here, we definitely need NFC
            //            Toast.makeText(mContext, "This device doesn't support writing NFC tags.", Toast.LENGTH_LONG).show();
            //            dismiss();
        }

        mSubmitButton.setVisibility(View.GONE);

        mLabelView.setText(R.string.status_awaiting_tap);

        mProgressBar.setVisibility(View.VISIBLE);
    }

    public void saveState(Bundle state) {
        state.putString(NETWORK_ID, mSsid);
        state.putString(SECURITY, mPassword);
        state.putString(ENC_TYPE, mWifiEncType);
    }

    private void handleWriteNfcEvent(Tag tag) {
        Ndef ndef = Ndef.get(tag);

        try {
            if (ndef != null) {
                ndef.connect();

                if (ndef.isWritable()) {
                    final WifiAuthType wifiAuthType = WifiAuthType.valueOf(mWifiEncType);

                    //                    final WirelessEncryptionTypeForQrCode encryptionType =
                    //                            WirelessEncryptionTypeForQrCode.valueOf(mWifiEncType);
                    //                    switch (encryptionType) {
                    //                        case NONE:
                    //                            wifiAuthType = WifiAuthType.OPEN;
                    //                            break;
                    //                        case WEP:
                    //                            wifiAuthType = WifiAuthType.WEP;
                    //                            break;
                    //                        case WPA:
                    //                            wifiAuthType = WifiAuthType.WPA2_PSK;
                    //                            break;
                    //                        default:
                    //                            throw new IllegalStateException();
                    //                    }

                    final WifiNetwork wifiNetwork = new WifiNetwork(mSsid, wifiAuthType, mPassword, false);
                    final NdefMessage ndefMessage = NfcUtils.generateNdefMessage(wifiNetwork);

                    final int size = ndefMessage.toByteArray().length;

                    if (ndef.getMaxSize() < size) {
                        final String errorMsg = "Cannot write to this tag. Message size ("
                                + size
                                + " bytes) exceeds this tag's capacity of "
                                + ndef.getMaxSize()
                                + " bytes.";
                        setViewText(mLabelView, errorMsg);
                        Crashlytics.log(Log.ERROR, TAG, errorMsg);
                        return;
                    }

                    ndef.writeNdefMessage(ndefMessage);
                    mContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setVisibility(View.GONE);
                        }
                    });
                    setViewText(mLabelView, R.string.status_write_success);
                    setViewText(mCancelButton, R.string.done_label);
                } else {
                    setViewText(mLabelView, R.string.status_tag_not_writable);
                    Crashlytics.log(Log.ERROR, TAG, "Tag is not writable");
                }
            } else {
                setViewText(mLabelView, "NFC tag is not writeable because it does not support NDEF. "
                        + "Please use a different one supporting NDEF technology.");
                final String[] techList = tag.getTechList();
                Crashlytics.log(Log.ERROR, TAG,
                        "Tag does not support NDEF. Tech List: " + Arrays.toString(techList));
            }
        } catch (Exception e) {
            setViewText(mLabelView, R.string.status_failed_to_write);
            Log.e(TAG, "Unable to write Wi-Fi config to NFC tag.", e);
            ReportingUtils.reportException(getContext(), e);
        }
    }

    private void setViewText(final TextView view, final int resid) {
        setViewText(view, mContext.getString(resid));
    }

    private void setViewText(final TextView view, final CharSequence text) {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setText(text);
            }
        });
    }
}
