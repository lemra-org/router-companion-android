package org.rm3l.ddwrt.resources;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile;

import java.util.Set;

public class ClientDevices {
    @NotNull
    private final Set<Device> devices = Sets.newHashSet();

    @Nullable
    private Exception exception;

    @NotNull
    public Set<Device> getDevices() {
        return devices;
    }

    @NotNull
    public int getDevicesCount() {
        return devices.size();
    }

    @NotNull
    public Set<Device> getDevices(int max) {
        return FluentIterable
                .from(devices)
                .limit(max)
                .toSortedSet(WirelessClientsTile.COMPARATOR);
    }

    @NotNull
    public ClientDevices addDevice(Device device) {
        this.devices.add(device);
        return this;
    }

    @Nullable
    public Exception getException() {
        return exception;
    }

    @NotNull
    public ClientDevices setException(Exception exception) {
        this.exception = exception;
        return this;
    }

    @Override
    public String toString() {
        return "Devices{" +
                "devices=" + devices +
                ", exception=" + exception +
                '}';
    }
}
