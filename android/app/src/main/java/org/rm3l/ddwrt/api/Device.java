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

package org.rm3l.ddwrt.api;

import static com.google.common.base.Strings.isNullOrEmpty;

public class Device implements Comparable<Device> {

    private final String macAddress;
    private String alias;
    private String ipAddress;
    private State state = State.UNKNOWN;
    private String systemName;

    public Device(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getAlias() {
        return alias;
    }

    public Device setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Device setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public State getState() {
        return state;
    }

    public Device setState(State state) {
        this.state = state;
        return this;
    }

    public String getSystemName() {
        return systemName;
    }

    public Device setSystemName(String systemName) {
        this.systemName = systemName;
        return this;
    }

    @Override
    public int compareTo(Device device) {
        if (this.alias == null) {
            if (device.alias == null) {
                return 0;
            }
            return -1;
        }

        if (device.alias == null) {
            return 1;
        }

        return this.alias.compareTo(device.alias);
    }

    public String getName() {
        if (!isNullOrEmpty(this.alias)) {
            return this.alias;
        }
        if (!isNullOrEmpty(this.systemName)) {
            return this.systemName;
        }
        return this.macAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Device device = (Device) o;

        if (macAddress != null ? !macAddress.equals(device.macAddress) : device.macAddress != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return macAddress != null ? macAddress.hashCode() : 0;
    }

    public boolean isEnabled() {
        return this.state != State.DISABLED;
    }

    public String toString() {
        return "Device[alias=" + this.alias + ",mac=" + this.macAddress + ",ip=" + this.ipAddress + "]";
    }

    enum State {
        UNKNOWN, ENABLED, DISABLED;
    }
}
