/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Armel S.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.rm3l.ddwrt.resources;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @NotNull
    private final List<String> permissions = new ArrayList<String>();

    @NotNull
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

    @NotNull
    public ProcMountPoint setDeviceType(final String deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public String getMountPoint() {
        return mountPoint;
    }

    @NotNull
    public ProcMountPoint setMountPoint(final String mountPoint) {
        this.mountPoint = mountPoint;
        return this;
    }

    public String getFsType() {
        return fsType;
    }

    @NotNull
    public ProcMountPoint setFsType(final String fsType) {
        this.fsType = fsType;
        return this;
    }

    @NotNull
    public ProcMountPoint addPermission(final String perm) {
        this.permissions.add(perm);
        return this;
    }

    @NotNull
    public List<String> getPermissions() {
        return permissions;
    }

    @NotNull
    public List<String> getOtherAttributes() {
        return otherAttributes;
    }

    @NotNull
    public ProcMountPoint addOtherAttr(@NotNull final String attr) {
        this.otherAttributes.add(attr);
        return this;
    }

    @NotNull
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

        @NotNull ProcMountPoint that = (ProcMountPoint) o;

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
