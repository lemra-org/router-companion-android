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
package org.rm3l.ddwrt.utils;

import android.support.annotation.NonNull;

import org.rm3l.maoni.common.model.DeviceInfo;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by rm3l on 01/08/16.
 */
public final class MaoniUtils {

    private MaoniUtils() {}

    public static Map<String, Object> deviceInfoToMap(@NonNull final DeviceInfo deviceInfo) {
        final SortedMap<String, Object> output = new TreeMap<>();
        //Introspect to get all fields
        final Field[] fields = DeviceInfo.class.getDeclaredFields();
        for (final Field field : fields) {
            final Object fieldValue;
            try {
                fieldValue = field.get(deviceInfo);
            } catch (IllegalAccessException e) {
                //No worries
                continue;
            }
            if (fieldValue == null) {
                continue;
            }
            output.put(field.getName(), fieldValue);
        }
        return Collections.unmodifiableMap(output);
    }
}
