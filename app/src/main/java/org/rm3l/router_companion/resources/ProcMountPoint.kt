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

package org.rm3l.router_companion.resources

import java.util.ArrayList

/**
 * Information about a /proc/mount endpoint (as retrieved from the router)

 * @author [Armel S.](mailto:apps+ddwrt@rm3l.org)
 */
class ProcMountPoint {

    /*
      The 1st column specifies the device that is mounted.
      The 2nd column reveals the mount point.
      The 3rd column tells the file-system type.
      The 4th column tells you if it is mounted read-only (ro) or read-write (rw).
      The 5th and 6th columns are dummy values designed to match the format used in /etc/mtab.
       */

    private val permissions = ArrayList<String?>()

    private val otherAttributes = ArrayList<String?>()

    private var deviceType: String? = null

    private var mountPoint: String? = null

    private var fsType: String? = null

    fun getDeviceType(): String? {
        return deviceType
    }

    fun setDeviceType(deviceType: String?): ProcMountPoint {
        this.deviceType = deviceType
        return this
    }

    fun getMountPoint(): String? {
        return mountPoint
    }

    fun setMountPoint(mountPoint: String?): ProcMountPoint {
        this.mountPoint = mountPoint
        return this
    }

    fun getFsType(): String? {
        return fsType
    }

    fun setFsType(fsType: String?): ProcMountPoint {
        this.fsType = fsType
        return this
    }

    fun addPermission(perm: String?): ProcMountPoint {
        this.permissions.add(perm)
        return this
    }

    fun getPermissions(): List<String?> {
        return permissions
    }

    fun getOtherAttributes(): List<String?> {
        return otherAttributes
    }

    fun addOtherAttr(attr: String): ProcMountPoint {
        this.otherAttributes.add(attr)
        return this
    }

    override fun toString(): String {
        return "ProcMountPoint{deviceType='$deviceType', mountPoint='$mountPoint', fsType='$fsType', permissions=$permissions, otherAttributes=$otherAttributes}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as ProcMountPoint?

        if (if (deviceType != null) deviceType != that!!.deviceType else that!!.deviceType != null) {
            return false
        }
        if (if (fsType != null) fsType != that.fsType else that.fsType != null) return false
        if (if (mountPoint != null) mountPoint != that.mountPoint else that.mountPoint != null) {
            return false
        }
        if (otherAttributes != that.otherAttributes) return false
        if (permissions != that.permissions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = if (deviceType != null) deviceType!!.hashCode() else 0
        result = 31 * result + if (mountPoint != null) mountPoint!!.hashCode() else 0
        result = 31 * result + if (fsType != null) fsType!!.hashCode() else 0
        result = 31 * result + permissions.hashCode()
        result = 31 * result + otherAttributes.hashCode()
        return result
    }
}
