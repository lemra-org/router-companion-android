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

package org.rm3l.ddwrt.utils;

import com.google.common.collect.Maps;

import org.jetbrains.annotations.NotNull;
import org.rm3l.ddwrt.api.Device;

import java.util.List;
import java.util.Map;

public class DeviceManager {

    private static final int BUFFERED_READER_SIZE = 1024;
    private static final String DEVICES_FILENAME = "devices.csv";
    private static final String TAG = DeviceManager.class.getSimpleName();
    private static DeviceManager singleton = null;
    private final Map<String, Device> ipToDeviceMap = Maps.newConcurrentMap();
    private List<Device> devices;
    private Map<String, Device> macToDeviceMap = Maps.newConcurrentMap();

    private DeviceManager() {
    }

    @NotNull
    public static DeviceManager getDeviceManager() {
        if (singleton == null) {
            singleton = new DeviceManager();
        }
        return singleton;
    }

    private void reset() {
        this.devices.clear();
        this.macToDeviceMap.clear();
        this.ipToDeviceMap.clear();
        ;
    }


}
