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
package org.rm3l.router_companion.actions;

import static org.rm3l.router_companion.actions.RouterAction.WAKE_ON_LAN;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.rm3l.router_companion.common.resources.audit.ActionLog;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.SSHUtils;

public class WakeOnLANRouterAction extends AbstractRouterAction<Void> {

    private final Context context;

    @NonNull
    private final List<String> mBroadcastAddressCandidates;

    @NonNull
    private final Device mDevice;

    private final int port;

    public WakeOnLANRouterAction(Router router, @NonNull Context context,
            @Nullable RouterActionListener listener, @NonNull SharedPreferences globalSharedPreferences,
            @NonNull Device device, @Nullable String... broadcastAddressCandidates) {
        this(router, context, listener, globalSharedPreferences, device, device.getWolPort(),
                broadcastAddressCandidates);
    }

    public WakeOnLANRouterAction(Router router, @NonNull Context ctx,
            @Nullable RouterActionListener listener, @NonNull SharedPreferences globalSharedPreferences,
            @NonNull Device device, int port, @Nullable String... broadcastAddressCandidates) {
        super(router, listener, WAKE_ON_LAN, globalSharedPreferences);
        this.context = ctx;
        if (broadcastAddressCandidates != null) {
            this.mBroadcastAddressCandidates = Arrays.asList(broadcastAddressCandidates);
        } else {
            this.mBroadcastAddressCandidates = new ArrayList<>();
        }
        this.mDevice = device;
        this.port = port;
    }

    @NonNull
    @Override
    protected RouterActionResult<Void> doActionInBackground() {
        Exception exception = null;
        try {
            if (mBroadcastAddressCandidates.isEmpty()) {
                throw new IllegalArgumentException("No Broadcast Address for WOL Feature");
            }

            ///usr/sbin/wol -i 192.168.1.255 -p PP AA:BB:CC:DD:EE:FF
            final String[] wolCmd = new String[mBroadcastAddressCandidates.size()];
            int i = 0;
            for (final String mBroadcastAddressCandidate : mBroadcastAddressCandidates) {
                wolCmd[i++] = String.format("/usr/sbin/wol -i %s %s %s", mBroadcastAddressCandidate,
                        port > 0 ? String.format("-p %d", port) : "", mDevice.getMacAddress());
            }
            final int exitStatus = SSHUtils.runCommands(context, globalSharedPreferences, router,
                    Joiner.on(" ; ").skipNulls(), wolCmd);
            if (exitStatus != 0) {
                throw new IllegalStateException();
            }
        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        }

        return new RouterActionResult<>(null, exception);
    }

    @Override
    protected ActionLog getActionLog() {
        return super.getActionLog()
                .setActionData(
                        String.format("- Device: %s (%s)\n" + "- Port: %d", mDevice.getAliasOrSystemName(),
                                mDevice.getMacAddress(), port));
    }

    @Nullable
    @Override
    protected Context getContext() {
        return context;
    }
}
