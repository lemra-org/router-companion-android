package org.rm3l.ddwrt.widgets;

import android.content.Context;
import android.util.TypedValue;
import android.widget.TextView;

import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.resources.Device;

import java.text.DecimalFormat;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

public class NetworkTrafficView extends TextView {

    public static final String NO_DATA = "-";
    public static final String PER_SEC = "/s";
    private static final int KILOBIT = 1000;
    private int KB = KILOBIT;
    private int MB = KB * KB;
    private int GB = MB * KB;
    private static final int KILOBYTE = 1024;
    private static DecimalFormat decimalFormat = new DecimalFormat("##0.#");

    static {
        decimalFormat.setMaximumIntegerDigits(3);
        decimalFormat.setMaximumFractionDigits(1);
    }

    private final boolean mIsThemeLight;
    private final String mRouterUuid;
    private final Context mContext;
    private final Device mDevice;

    public NetworkTrafficView(final Context context, final boolean isThemeLight,
                              final String routerUuid, Device device) {
        super(context);
        this.mContext = context;
        this.mIsThemeLight = isThemeLight;
        this.mRouterUuid = routerUuid;
        this.mDevice = device;
    }

    public void setRxAndTxBytes(final long rxBytes, final long txBytes) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) mContext.getResources()
                .getDimensionPixelSize(R.dimen.net_traffic_single_text_size));
        String dataToShow = (txBytes < 0l ? NO_DATA : byteCountToDisplaySize(txBytes) + PER_SEC);
        dataToShow += ("\n" + (rxBytes < 0l ? NO_DATA : byteCountToDisplaySize(rxBytes) + PER_SEC));
        super.setText(dataToShow);
        updateTrafficDrawable();
    }

    private void updateTrafficDrawable() {
        setCompoundDrawablesWithIntrinsicBounds(0, 0,
                mIsThemeLight ?
                        R.drawable.stat_sys_network_traffic_updown_light :
                        R.drawable.stat_sys_network_traffic_updown,
                0);
    }
}
