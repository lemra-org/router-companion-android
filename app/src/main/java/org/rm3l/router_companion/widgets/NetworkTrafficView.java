/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <apps+ddwrt@rm3l.org>
 */

package org.rm3l.router_companion.widgets;

import android.content.Context;
import android.util.TypedValue;
import android.widget.TextView;
import java.text.DecimalFormat;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.resources.Device;

public class NetworkTrafficView extends TextView {

  public static final String NO_DATA = "-";

  public static final String PER_SEC = "/s";

  private static final int KILOBIT = 1000;

  private static final int KILOBYTE = 1024;

  private static DecimalFormat decimalFormat = new DecimalFormat("##0.#");

  private int KB = KILOBIT;

  private int MB = KB * KB;

  private int GB = MB * KB;

  private final Context mContext;

  private final Device mDevice;

  private final boolean mIsThemeLight;

  private final String mRouterUuid;

  public NetworkTrafficView(
      final Context context, final boolean isThemeLight, final String routerUuid, Device device) {
    super(context);
    this.mContext = context;
    this.mIsThemeLight = isThemeLight;
    this.mRouterUuid = routerUuid;
    this.mDevice = device;
  }

  public void setRxAndTxBytes(final long rxBytes, final long txBytes) {
    setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        (float)
            mContext.getResources().getDimensionPixelSize(R.dimen.net_traffic_single_text_size));
    String dataToShow =
        (txBytes < 0l
            ? NO_DATA
            : org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(txBytes) + PER_SEC);
    dataToShow +=
        ("\n"
            + (rxBytes < 0l
                ? NO_DATA
                : org.rm3l.router_companion.utils.FileUtils.byteCountToDisplaySize(rxBytes)
                    + PER_SEC));
    super.setText(dataToShow);
    updateTrafficDrawable();
  }

  private void updateTrafficDrawable() {
    setCompoundDrawablesWithIntrinsicBounds(
        0,
        0,
        mIsThemeLight
            ? R.drawable.stat_sys_network_traffic_updown_light
            : R.drawable.stat_sys_network_traffic_updown,
        0);
  }

  static {
    decimalFormat.setMaximumIntegerDigits(3);
    decimalFormat.setMaximumFractionDigits(1);
  }
}
