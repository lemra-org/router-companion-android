/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
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
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */
package org.rm3l.router_companion.utils;

import static com.google.common.base.Strings.nullToEmpty;

import android.app.Activity;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import org.rm3l.router_companion.resources.Device;
import org.rm3l.router_companion.utils.snackbar.SnackbarUtils.Style;

public class WoLUtils {

  public static class SendWoLMagicPacketAsyncTask
      extends AsyncTask<String, Void, SendWoLMagicPacketAsyncTask.Result<Void>> {

    class Result<T> {

      private final Exception exception;

      private final T result;

      private Result(T result, Exception exception) {
        this.result = result;
        this.exception = exception;
      }

      public Exception getException() {
        return exception;
      }

      public T getResult() {
        return result;
      }
    }

    @NonNull final Activity activity;

    @NonNull final Device device;

    public SendWoLMagicPacketAsyncTask(@NonNull final Activity activity, @NonNull Device device) {
      this.activity = activity;
      this.device = device;
    }

    @Override
    protected SendWoLMagicPacketAsyncTask.Result<Void> doInBackground(String... params) {

      Exception exception = null;

      try {
        if (params == null || params.length < 2) {
          throw new IllegalArgumentException(
              "Wrong number of args - need at least MAC and Broadcast addresses");
        }
        sendWoLMagicPacket(params[0], params[1]);
      } catch (Exception e) {
        e.printStackTrace();
        exception = e;
      }

      return new SendWoLMagicPacketAsyncTask.Result<>(null, exception);
    }

    @Override
    protected void onPostExecute(SendWoLMagicPacketAsyncTask.Result result) {
      final Exception exception = result.getException();
      try {
        final String deviceName = nullToEmpty(device.getName());
        final String macAddress = device.getMacAddress();
        if (exception == null) {
          Utils.displayMessage(
              activity,
              String.format("Magic packet sent to '%s' (%s)", deviceName, macAddress),
              Style.CONFIRM);
        } else {
          Utils.displayMessage(
              activity,
              String.format("Error - magic packet not sent to '%s' (%s)", deviceName, macAddress),
              Style.ALERT);
        }
      } catch (Exception e) {
        // No worries
      }
    }
  }

  public static final int PORT = 9;

  public static void sendWoLMagicPacket(
      @NonNull final String macStr, @NonNull final String bcastIpStr) throws IOException {

    byte[] macBytes = getMacBytes(macStr);
    byte[] bytes = new byte[6 + 16 * macBytes.length];
    for (int i = 0; i < 6; i++) {
      bytes[i] = (byte) 0xff;
    }
    for (int i = 6; i < bytes.length; i += macBytes.length) {
      System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
    }

    final InetAddress address = InetAddress.getByName(bcastIpStr);
    final DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, PORT);
    final DatagramSocket socket = new DatagramSocket();
    socket.send(packet);
    socket.close();
  }

  private static byte[] getMacBytes(String macStr) throws IllegalArgumentException {
    byte[] bytes = new byte[6];
    String[] hex = macStr.split("(\\:|\\-)");
    if (hex.length != 6) {
      throw new IllegalArgumentException("Invalid MAC address.");
    }
    try {
      for (int i = 0; i < 6; i++) {
        bytes[i] = (byte) Integer.parseInt(hex[i], 16);
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid hex digit in MAC address.");
    }
    return bytes;
  }
}
