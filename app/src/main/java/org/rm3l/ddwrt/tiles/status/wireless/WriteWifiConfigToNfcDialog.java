package org.rm3l.ddwrt.tiles.status.wireless;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.rm3l.ddwrt.R;

import java.io.IOException;

/**
 * Created by rm3l on 10/11/2016.
 */
class WriteWifiConfigToNfcDialog extends AlertDialog implements View.OnClickListener {

    private static final String NFC_TOKEN_MIME_TYPE = "application/vnd.wfa.wsc";

    private static final String TAG = WriteWifiConfigToNfcDialog.class.getName();
    private static final String PASSWORD_FORMAT = "102700%s%s";
    private static final int HEX_RADIX = 16;
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static final String NETWORK_ID = "network_id";
    private static final String SECURITY = "security";

    private final PowerManager.WakeLock mWakeLock;
    private final String mSsid;
    private final String mPassword;
    private final Activity mContext;

    private View mView;
    private Button mSubmitButton;
    private Button mCancelButton;
    private TextView mLabelView;
    private ProgressBar mProgressBar;

    WriteWifiConfigToNfcDialog(Activity context, final String ssid, final String password) {
        super(context);
        mContext = context;
        mWakeLock = ((PowerManager) context.getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WriteWifiConfigToNfcDialog:wakeLock");
        mSsid = ssid;
        mPassword = password;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.write_wifi_config_to_nfc, null);

        setView(mView);
        setInverseBackgroundForced(true);
        setTitle(R.string.setup_wifi_nfc_tag);
        setCancelable(true);
        setButton(DialogInterface.BUTTON_NEUTRAL,
                mContext.getResources().getString(R.string.write_tag), (OnClickListener) null);
        setButton(DialogInterface.BUTTON_NEGATIVE,
                mContext.getResources().getString(R.string.cancel),
                (OnClickListener) null);

        mLabelView = (TextView) mView.findViewById(R.id.password_label);
        mProgressBar = (ProgressBar) mView.findViewById(R.id.progress_bar);

        super.onCreate(savedInstanceState);

        mSubmitButton = getButton(DialogInterface.BUTTON_NEUTRAL);
        mSubmitButton.setOnClickListener(this);
        mSubmitButton.setEnabled(true);
        mSubmitButton.performClick();

        mCancelButton = getButton(DialogInterface.BUTTON_NEGATIVE);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onClick(View v) {
        mWakeLock.acquire();

        String passwordHex = byteArrayToHexString(mPassword.getBytes());

        final int pwdLength = mPassword.length();
        String passwordLength = pwdLength >= HEX_RADIX
                ? Integer.toString(pwdLength, HEX_RADIX)
                : "0" + Character.forDigit(pwdLength, HEX_RADIX);

        passwordHex = String.format(PASSWORD_FORMAT, passwordLength, passwordHex).toUpperCase();

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(mContext);

        nfcAdapter.enableReaderMode(mContext, new NfcAdapter.ReaderCallback() {
                    @Override
                    public void onTagDiscovered(Tag tag) {
                        handleWriteNfcEvent(tag);
                    }
                }, NfcAdapter.FLAG_READER_NFC_A |
                        NfcAdapter.FLAG_READER_NFC_B |
                        NfcAdapter.FLAG_READER_NFC_BARCODE |
                        NfcAdapter.FLAG_READER_NFC_F |
                        NfcAdapter.FLAG_READER_NFC_V,
                null);

        mSubmitButton.setVisibility(View.GONE);

        mLabelView.setText(R.string.status_awaiting_tap);

        mProgressBar.setVisibility(View.VISIBLE);
    }

//    public void saveState(Bundle state) {
//        state.putInt(NETWORK_ID, mNetworkId);
//        state.putInt(SECURITY, mSecurity);
//    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void handleWriteNfcEvent(Tag tag) {
        Ndef ndef = Ndef.get(tag);

        if (ndef != null) {
            if (ndef.isWritable()) {
                NdefRecord record = NdefRecord.createMime(
                        NFC_TOKEN_MIME_TYPE,
                        hexStringToByteArray(mPassword));
                try {
                    ndef.connect();
                    ndef.writeNdefMessage(new NdefMessage(record));
                    mContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressBar.setVisibility(View.GONE);
                        }
                    });
                    setViewText(mLabelView, R.string.status_write_success);
                    setViewText(mCancelButton, R.string.done_label);
                } catch (IOException e) {
                    setViewText(mLabelView, R.string.status_failed_to_write);
                    Log.e(TAG, "Unable to write Wi-Fi config to NFC tag.", e);
                    return;
                } catch (FormatException e) {
                    setViewText(mLabelView, R.string.status_failed_to_write);
                    Log.e(TAG, "Unable to write Wi-Fi config to NFC tag.", e);
                    return;
                }
            } else {
                setViewText(mLabelView, R.string.status_tag_not_writable);
                Log.e(TAG, "Tag is not writable");
            }
        } else {
            setViewText(mLabelView, R.string.status_tag_not_writable);
            Log.e(TAG, "Tag does not support NDEF");
        }
    }

    @Override
    public void dismiss() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        super.dismiss();
    }


    private void setViewText(final TextView view, final int resid) {
        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setText(resid);
            }
        });
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), HEX_RADIX) << 4)
                    + Character.digit(s.charAt(i + 1), HEX_RADIX));
        }

        return data;
    }

    private static String byteArrayToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
