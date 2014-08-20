package org.lemra.dd_wrt.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by armel on 8/19/14.
 */
public class ProcMountPoint {

    /*
    The 1st column specifies the device that is mounted.
The 2nd column reveals the mount point.
The 3rd column tells the file-system type.
The 4th column tells you if it is mounted read-only (ro) or read-write (rw).
The 5th and 6th columns are dummy values designed to match the format used in /etc/mtab.
     */

    private final List<String> permissions = new ArrayList<String>();
    private final List<String> otherAttributes = new ArrayList<String>();
    private String deviceType;
    private String mountPoint;
    private String fsType;

    public String getDeviceType() {
        return deviceType;
    }

    public ProcMountPoint setDeviceType(String deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public String getMountPoint() {
        return mountPoint;
    }

    public ProcMountPoint setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
        return this;
    }

    public String getFsType() {
        return fsType;
    }

    public ProcMountPoint setFsType(String fsType) {
        this.fsType = fsType;
        return this;
    }

    public ProcMountPoint addPermission(String perm) {
        this.permissions.add(perm);
        return this;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public List<String> getOtherAttributes() {
        return otherAttributes;
    }

    public ProcMountPoint addOtherAttr(String attr) {
        this.otherAttributes.add(attr);
        return this;
    }

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProcMountPoint that = (ProcMountPoint) o;

        if (deviceType != null ? !deviceType.equals(that.deviceType) : that.deviceType != null)
            return false;
        if (fsType != null ? !fsType.equals(that.fsType) : that.fsType != null) return false;
        if (mountPoint != null ? !mountPoint.equals(that.mountPoint) : that.mountPoint != null)
            return false;
        if (otherAttributes != null ? !otherAttributes.equals(that.otherAttributes) : that.otherAttributes != null)
            return false;
        if (permissions != null ? !permissions.equals(that.permissions) : that.permissions != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = deviceType != null ? deviceType.hashCode() : 0;
        result = 31 * result + (mountPoint != null ? mountPoint.hashCode() : 0);
        result = 31 * result + (fsType != null ? fsType.hashCode() : 0);
        result = 31 * result + (permissions != null ? permissions.hashCode() : 0);
        result = 31 * result + (otherAttributes != null ? otherAttributes.hashCode() : 0);
        return result;
    }
}
