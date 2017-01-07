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

package org.rm3l.router_companion.resources;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Information about a /proc/mount endpoint (as retrieved from the router)
 *
 * @author <a href="mailto:apps+ddwrt@rm3l.org">Armel S.</a>
 */
public class ProcMountPoint {

    /*
    The 1st column specifies the device that is mounted.
    The 2nd column reveals the mount point.
    The 3rd column tells the file-system type.
    The 4th column tells you if it is mounted read-only (ro) or read-write (rw).
    The 5th and 6th columns are dummy values designed to match the format used in /etc/mtab.
     */

    @NonNull
    private final List<String> permissions = new ArrayList<String>();

    @NonNull
    private final List<String> otherAttributes = new ArrayList<String>();

    @Nullable
    private String deviceType;

    @Nullable
    private String mountPoint;

    @Nullable
    private String fsType;

    public String getDeviceType() {
        return deviceType;
    }

    @NonNull
    public ProcMountPoint setDeviceType(final String deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public String getMountPoint() {
        return mountPoint;
    }

    @NonNull
    public ProcMountPoint setMountPoint(final String mountPoint) {
        this.mountPoint = mountPoint;
        return this;
    }

    public String getFsType() {
        return fsType;
    }

    @NonNull
    public ProcMountPoint setFsType(final String fsType) {
        this.fsType = fsType;
        return this;
    }

    @NonNull
    public ProcMountPoint addPermission(final String perm) {
        this.permissions.add(perm);
        return this;
    }

    @NonNull
    public List<String> getPermissions() {
        return permissions;
    }

    @NonNull
    public List<String> getOtherAttributes() {
        return otherAttributes;
    }

    @NonNull
    public ProcMountPoint addOtherAttr(@NonNull final String attr) {
        this.otherAttributes.add(attr);
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return "ProcMountPoint{" +
                "deviceType='" + deviceType + '\'' +
                ", mountPoint='" + mountPoint + '\'' +
                ", fsType='" + fsType + '\'' +
                ", permissions=" + permissions +
                ", otherAttributes=" + otherAttributes +
                '}';
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ProcMountPoint that = (ProcMountPoint) o;

        if (deviceType != null ? !deviceType.equals(that.deviceType) : that.deviceType != null)
            return false;
        if (fsType != null ? !fsType.equals(that.fsType) : that.fsType != null) return false;
        if (mountPoint != null ? !mountPoint.equals(that.mountPoint) : that.mountPoint != null)
            return false;
        if (!otherAttributes.equals(that.otherAttributes))
            return false;
        if (!permissions.equals(that.permissions))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = deviceType != null ? deviceType.hashCode() : 0;
        result = 31 * result + (mountPoint != null ? mountPoint.hashCode() : 0);
        result = 31 * result + (fsType != null ? fsType.hashCode() : 0);
        result = 31 * result + permissions.hashCode();
        result = 31 * result + otherAttributes.hashCode();
        return result;
    }
}
