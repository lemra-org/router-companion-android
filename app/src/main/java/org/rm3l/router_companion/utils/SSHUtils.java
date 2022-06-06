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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.RouterCompanionAppConstants.EMPTY_STRING;
import static org.rm3l.router_companion.resources.conn.Router.RouterFirmware;
import static org.rm3l.router_companion.resources.conn.Router.RouterFirmware.DDWRT;
import static org.rm3l.router_companion.resources.conn.Router.RouterFirmware.TOMATO;
import static org.rm3l.router_companion.utils.Utils.checkDataSyncAlllowedByUsagePreference;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Patterns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.rm3l.ddwrt.BuildConfig;
import org.rm3l.router_companion.actions.RouterAction;
import org.rm3l.router_companion.actions.RouterStreamActionListener;
import org.rm3l.router_companion.exceptions.UnknownRouterFirmwareException;
import org.rm3l.router_companion.firmwares.RemoteDataRetrievalListener;
import org.rm3l.router_companion.firmwares.RouterFirmwareConnectorManager;
import org.rm3l.router_companion.resources.conn.NVRAMInfo;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.resources.conn.openwrt.UCIInfo;

/** SSH Utilities */
public final class SSHUtils {

  private enum SSHLogger implements com.jcraft.jsch.Logger {
    INSTANCE;

    private static final String LOG_TAG = TAG + "." + SSHLogger.class.getSimpleName();

    public boolean isEnabled(int level) {
      if (BuildConfig.DEBUG) {
        // All levels are on
        return true;
      }

      // Otherwise, just WARN, ERROR and FATAL are on
      return (level == WARN || level == ERROR || level == FATAL);
    }

    public void log(int level, String message) {
      switch (level) {
        case INFO:
          FirebaseCrashlytics.getInstance().log("[INFO] " + message);
          break;
        case WARN:
          FirebaseCrashlytics.getInstance().log("[WARN] " + message);
          break;
        case ERROR:
          FirebaseCrashlytics.getInstance().log("[ERROR] " + message);
          break;
        case FATAL:
          FirebaseCrashlytics.getInstance().log("[FATAL] " + message);
          break;
        case DEBUG:
          FirebaseCrashlytics.getInstance().log("[DEBUG] " + message);
          break;
        default:
          FirebaseCrashlytics.getInstance().log("[" + level + "] " + message);
          break;
      }
    }
  }

  public static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";

  public static final String YES = "yes";

  public static final String NO = "no";

  public static final Joiner JOINER_CARRIAGE_RETURN = Joiner.on("\n");

  public static final int CONNECT_TIMEOUT_MILLIS = 15000; // Timeout of 15s

  public static final int CONNECTION_KEEP_ALIVE_INTERVAL_MILLIS = 1000;

  // Send a Keep-Alive msg every 1s
  public static final int MAX_NUMBER_OF_CONCURRENT_SSH_SESSIONS_PER_ROUTER = 3;

  private static final String TAG = SSHUtils.class.getSimpleName();

  private static final List<String> MULTI_OUTPUT_NVRAM_VARS =
      Arrays.asList(
          NVRAMInfo.Companion.getSSHD_RSA_HOST_KEY(),
          NVRAMInfo.Companion.getSSHD_DSS_HOST_KEY(),
          NVRAMInfo.Companion.getOPENVPNCL_CA(),
          NVRAMInfo.Companion.getOPENVPNCL_CLIENT(),
          NVRAMInfo.Companion.getOPENVPNCL_KEY(),
          NVRAMInfo.Companion.getOPENVPNCL_TLSAUTH(),
          NVRAMInfo.Companion.getOPENVPNCL_STATIC(),
          NVRAMInfo.Companion.getOPENVPNCL_ROUTE(),
          NVRAMInfo.Companion.getOPENVPN_CA(),
          NVRAMInfo.Companion.getOPENVPN_CLIENT(),
          NVRAMInfo.Companion.getOPENVPN_KEY(),
          NVRAMInfo.Companion.getOPENVPN_TLSAUTH(),
          NVRAMInfo.Companion.getOPENVPN_CRT(),
          NVRAMInfo.Companion.getOPENVPN_CRL(),
          NVRAMInfo.Companion.getOPENVPN_STATIC());

  private static final Map<RouterFirmware, String> FIRMWARE_AUTODETECT_CMDS =
      Maps.newHashMapWithExpectedSize(RouterFirmware.values().length);

  public static void checkConnection(
      @NonNull final Context ctx,
      @NonNull SharedPreferences globalSharedPreferences,
      @NonNull final Router router,
      final int connectTimeoutMillis)
      throws Exception {
    // This is used that for a temporary connection check
    // at this point, we can just make a copy of the existing router and assign it a random UUID
    final Router routerCopy = new Router(ctx, router);
    final String tempUuid = UUID.randomUUID().toString();

    try {
      routerCopy.setUuid(tempUuid);

      final Session jschSession = routerCopy.getSSHSession();

      if (jschSession == null) {
        throw new IllegalStateException("Unable to retrieve session - please retry again later!");
      }
      if (!jschSession.isConnected()) {
        jschSession.connect(connectTimeoutMillis);
      }

      final RouterFirmware routerFirmware = router.getRouterFirmware();
      if (routerFirmware == null || RouterFirmware.AUTO.equals(routerFirmware)) {
        // AutoDetect firmware
        ChannelExec channelExec = null;
        InputStream in = null;
        InputStream err = null;
        try {
          channelExec = (ChannelExec) jschSession.openChannel("exec");

          // Build command to execute
          final String[] autoDetectCommand = new String[FIRMWARE_AUTODETECT_CMDS.size() + 1];
          int i = 0;
          for (Map.Entry<RouterFirmware, String> routerFirmwareCmdEntry :
              FIRMWARE_AUTODETECT_CMDS.entrySet()) {
            final String cmd = routerFirmwareCmdEntry.getValue();
            final RouterFirmware fw = routerFirmwareCmdEntry.getKey();
            autoDetectCommand[i++] = String.format("( %s && echo \"%s\" )", cmd, fw.name());
          }
          autoDetectCommand[i] = ("( echo \"" + RouterFirmware.UNKNOWN + "\" )");

          channelExec.setCommand(Joiner.on(" || ").skipNulls().join(autoDetectCommand));
          channelExec.setInputStream(null);
          in = channelExec.getInputStream();
          err = channelExec.getErrStream();
          channelExec.connect();

          final String[] output = Utils.getLines(new BufferedReader(new InputStreamReader(in)));
          if (output.length == 0) {
            router.setRouterFirmware(RouterFirmware.UNKNOWN);
          } else {
            router.setRouterFirmware(RouterFirmware.valueOf(output[0]));
          }
        } catch (final Exception e) {
          // No worries
          e.printStackTrace();
          router.setRouterFirmware(RouterFirmware.UNKNOWN);
        } finally {
          if (in != null) {
            Closeables.closeQuietly(in);
          }
          if (err != null) {
            Closeables.closeQuietly(err);
          }
          if (channelExec != null) {
            channelExec.disconnect();
          }
        }
      } else if (Utils.isDemoRouter(router)) {
        router.setRouterFirmware(RouterFirmware.DEMO);
      }

      if (RouterFirmware.UNKNOWN.equals(router.getRouterFirmware())) {
        throw new UnknownRouterFirmwareException(
            "Could not detect router firmware. Please go back and pick one from the list.");
      }

      try {
        RouterFirmwareConnectorManager.getConnector(router).getRouterModel(ctx, router);
      } catch (final Exception e) {
        e.printStackTrace();
        // No worries
      }
    } finally {
      // Now delete session. Since ths is a copy, sessions will not be reused
      routerCopy.destroyAllSessions();
    }
  }

  public static void destroySessions(@Nullable final Router router) {
    //        Router.destroySession(router);
    if (router == null) {
      return;
    }
    router.destroyAllSessions();
  }

  public static String[] execCommandOverTelnet(
      Context ctx,
      @NonNull final Router router,
      SharedPreferences globalPreferences,
      final int telnetPort,
      @NonNull final String... cmdToExecute)
      throws Exception {

    checkDataSyncAlllowedByUsagePreference(ctx);

    // ( echo "log 15"; sleep 1 ) | telnet localhost 16
    final List<String> cmdToRun = Lists.newArrayList();
    if (cmdToExecute.length > 0) {
      for (String cmdToExec : cmdToExecute) {
        cmdToRun.add(String.format("echo \"%s\"", cmdToExec));
      }
    }

    return getManualProperty(
        ctx,
        router,
        globalPreferences,
        String.format(
            "( %s ; sleep 1 ) | telnet localhost %d",
            Joiner.on(";").skipNulls().join(cmdToRun), telnetPort));
  }

  public static int execStreamableCommand(
      Context ctx,
      @NonNull final Router router,
      SharedPreferences globalPreferences,
      @NonNull final RouterAction routerAction,
      @Nullable final RouterStreamActionListener routerStreamActionListener,
      @NonNull final String... cmdToExecute)
      throws Exception {

    final Router routerCopy = new Router(ctx, router);
    final String tempUuid = UUID.randomUUID().toString();
    routerCopy.setUuid(tempUuid);

    if (routerStreamActionListener != null) {
      routerStreamActionListener.notifyRouterActionProgress(routerAction, routerCopy, 0, null);
    }

    FirebaseCrashlytics.getInstance()
        .log(
            "getManualProperty: <router="
                + router
                + " / cmdToExecute="
                + Arrays.toString(cmdToExecute)
                + ">");

    checkDataSyncAlllowedByUsagePreference(ctx);

    ChannelExec channelExec = null;
    InputStream in = null;
    InputStream err = null;

    Integer exitStatus = null;
    try {

      final Session jschSession = router.getSSHSession();

      if (jschSession == null) {
        throw new IllegalStateException("Unable to retrieve session - please retry again later!");
      }
      if (!jschSession.isConnected()) {
        jschSession.connect(CONNECT_TIMEOUT_MILLIS);
      }

      channelExec = (ChannelExec) jschSession.openChannel("exec");
      channelExec.setCommand(Joiner.on(" && ").skipNulls().join(cmdToExecute));
      channelExec.setInputStream(null);
      channelExec.setErrStream(System.err);
      in = channelExec.getInputStream();
      err = channelExec.getErrStream();

      channelExec.connect();

      byte[] tmp = new byte[1024];
      int progress = 1;
      while (true) {
        while (in.available() > 0) {
          int i = in.read(tmp, 0, 1024);
          if (i < 0) {
            break;
          }
          if (routerStreamActionListener != null) {
            routerStreamActionListener.notifyRouterActionProgress(
                routerAction, routerCopy, progress++, new String(tmp, 0, i));
          }
        }
        if (channelExec.isClosed()) {
          if (in.available() > 0) {
            continue;
          }
          exitStatus = channelExec.getExitStatus();
          break;
        }
        Thread.sleep(100l);
      }
    } finally {
      Closeables.closeQuietly(in);
      Closeables.closeQuietly(err);
      if (channelExec != null) {
        channelExec.disconnect();
      }
    }

    return (exitStatus != null ? exitStatus : -1);
  }

  @Nullable
  public static String[] getManualProperty(
      Context ctx,
      @NonNull final Router router,
      SharedPreferences globalPreferences,
      @NonNull final String... cmdToExecute)
      throws Exception {
    return getManualProperty(ctx, router, globalPreferences, null, cmdToExecute);
  }

  @Nullable
  public static String[] getManualProperty(
      Context ctx,
      @NonNull final Router router,
      SharedPreferences globalPreferences,
      @Nullable final Joiner cmdSeparatorJoiner,
      @NonNull final String... cmdToExecute)
      throws Exception {
    FirebaseCrashlytics.getInstance()
        .log(
            "getManualProperty: <router="
                + router
                + " / cmdToExecute="
                + Arrays.toString(cmdToExecute)
                + ">");

    checkDataSyncAlllowedByUsagePreference(ctx);

    ChannelExec channelExec = null;
    InputStream in = null;
    InputStream err = null;

    try {

      final Session jschSession = router.getSSHSession();
      if (jschSession == null) {
        throw new IllegalStateException("Unable to retrieve session - please retry again later!");
      }
      if (!jschSession.isConnected()) {
        jschSession.connect(CONNECT_TIMEOUT_MILLIS);
      }

      channelExec = (ChannelExec) jschSession.openChannel("exec");

      final Joiner separatorJoiner =
          (cmdSeparatorJoiner == null ? Joiner.on(" ; ").skipNulls() : cmdSeparatorJoiner);

      channelExec.setCommand(separatorJoiner.join(cmdToExecute));
      channelExec.setInputStream(null);
      in = channelExec.getInputStream();
      err = channelExec.getErrStream();
      channelExec.connect();

      return Utils.getLines(new BufferedReader(new InputStreamReader(in)));
    } finally {
      Closeables.closeQuietly(in);
      Closeables.closeQuietly(err);
      if (channelExec != null) {
        channelExec.disconnect();
      }
    }
  }

  @Nullable
  public static NVRAMInfo getNVRamInfoFromRouter(
      Context ctx,
      @Nullable final Router router,
      SharedPreferences globalPreferences,
      @Nullable final String... fieldsToFetch)
      throws Exception {

    if (router == null) {
      throw new IllegalArgumentException("No connection parameters");
    }

    checkDataSyncAlllowedByUsagePreference(ctx);

    boolean getMultiOutput = false;
    final List<String> grep = Lists.newArrayList();
    if (fieldsToFetch == null || fieldsToFetch.length == 0) {
      getMultiOutput = true;
    } else {
      for (final String fieldToFetch : fieldsToFetch) {
        if (isNullOrEmpty(fieldToFetch)) {
          continue;
        }
        for (final String multiOutputNvramVar : MULTI_OUTPUT_NVRAM_VARS) {
          if (multiOutputNvramVar != null
              && fieldToFetch.toLowerCase().contains(multiOutputNvramVar.toLowerCase())) {
            getMultiOutput = true;
            break;
          }
        }
        grep.add("^" + fieldToFetch + "=.*");
      }
    }

    final RouterFirmware routerFirmware = router.getRouterFirmware();
    if (routerFirmware == null) {
      throw new UnknownRouterFirmwareException();
    }
    final String nvramPath = routerFirmware.nvramPath;
    if (TextUtils.isEmpty(nvramPath)) {
      throw new UnknownRouterFirmwareException("Unknwon NVRAM path for firmware");
    }

    final String[] nvramShow =
        SSHUtils.getManualProperty(
            ctx,
            router,
            globalPreferences,
            nvramPath
                + " show"
                + (grep.isEmpty() ? "" : (" | grep -E \"" + Joiner.on("|").join(grep) + "\"")));

    final String[] varsToFix = new String[MULTI_OUTPUT_NVRAM_VARS.size()];
    int i = 0;
    for (final String multiOutputNvramVar : MULTI_OUTPUT_NVRAM_VARS) {
      final String[] completeValue =
          getMultiOutput
              ? SSHUtils.getManualProperty(
                  ctx, router, globalPreferences, nvramPath + " get " + multiOutputNvramVar)
              : null;
      varsToFix[i++] =
          (multiOutputNvramVar
              + "="
              + (completeValue != null
                  ? JOINER_CARRIAGE_RETURN.join(completeValue)
                  : EMPTY_STRING));
    }

    String[] outputArray = null;
    if (nvramShow != null) {
      outputArray = new String[nvramShow.length + varsToFix.length];
      int k = 0;
      for (final String varToFix : varsToFix) {
        if (isNullOrEmpty(varToFix)) {
          continue;
        }
        outputArray[k++] = varToFix;
      }
      for (final String aNvramShow : nvramShow) {
        boolean skip = false;
        for (final String varToFix : varsToFix) {
          if (varToFix != null && varToFix.contains(aNvramShow)) {
            skip = true;
            break;
          }
        }
        if (skip) {
          continue;
        }
        outputArray[k] = aNvramShow;
        k++;
      }
    }

    return NVRAMParser.parseNVRAMOutput(outputArray);
  }

  @Nullable
  public static UCIInfo getUCIInfoFromOpenWrtRouter(
      Context ctx,
      @Nullable final Router router,
      SharedPreferences globalPreferences,
      @Nullable final String... uciCommandsToRun)
      throws Exception {

    if (router == null) {
      throw new IllegalArgumentException("No connection parameters");
    }

    if (uciCommandsToRun == null) {
      return null;
    }

    checkDataSyncAlllowedByUsagePreference(ctx);

    final String[] uciOutputLines =
        SSHUtils.getManualProperty(
            ctx, router, globalPreferences, Joiner.on(" ; ").join(uciCommandsToRun));

    if (uciOutputLines == null || uciOutputLines.length == 0) {
      return null;
    }

    final UCIInfo uciInfo = new UCIInfo();

    int size;
    for (final String uciOutputLine : uciOutputLines) {
      if (uciOutputLine == null) {
        continue;
      }
      final List<String> strings = NVRAMParser.SPLITTER.splitToList(uciOutputLine);
      size = strings.size();
      if (size == 1) {
        uciInfo.setProperty(strings.get(0), EMPTY_STRING);
      } else if (size >= 2) {
        uciInfo.setProperty(strings.get(0), nullToEmpty(strings.get(1)));
      }
    }

    return uciInfo;
  }

  @Nullable
  public static String loadWanPublicIPFrom(
      @NonNull Context context,
      @NonNull Router router,
      @Nullable final String ncCmdPath,
      @Nullable RemoteDataRetrievalListener dataRetrievalListener)
      throws Exception {

    if (dataRetrievalListener != null) {
      dataRetrievalListener.onProgressUpdate(40);
    }

    final SharedPreferences globalSharedPreferences = Utils.getGlobalSharedPreferences(context);

    // Check actual connections to the outside from the router
    final CharSequence applicationName = Utils.getApplicationName(context);
    final String[] wanPublicIpCmdStatus =
        SSHUtils.getManualProperty(
            context,
            router,
            globalSharedPreferences,
            Utils.getCommandForInternetIPResolution(context, ncCmdPath)

            // String.format("(echo -e \""
            //         + "GET / HTTP/1.1\\r\\n"
            //         + "Host:%s\\r\\n"
            //         + "User-Agent:%s/%s\\r\\n\" "
            //         + "| %s %s %d", PublicIPInfo.ICANHAZIP_HOST,
            //     applicationName != null ? applicationName : BuildConfig.APPLICATION_ID,
            //     BuildConfig.VERSION_NAME, TextUtils.isEmpty(ncCmdPath) ? "/usr/bin/nc" :
            // ncCmdPath,
            //     PublicIPInfo.ICANHAZIP_HOST, PublicIPInfo.ICANHAZIP_PORT)
            );

    String mWanPublicIP = null;
    if (wanPublicIpCmdStatus != null && wanPublicIpCmdStatus.length > 0) {
      final String wanPublicIp = wanPublicIpCmdStatus[wanPublicIpCmdStatus.length - 1].trim();
      if (Patterns.IP_ADDRESS.matcher(wanPublicIp).matches()) {
        mWanPublicIP = wanPublicIp;
      } else {
        mWanPublicIP = null;
      }
    }
    return mWanPublicIP;
  }

  public static int runCommands(
      Context context,
      @NonNull SharedPreferences globalSharedPreferences,
      @NonNull final Router router,
      @NonNull final Joiner commandsJoiner,
      @NonNull final String... cmdToExecute)
      throws Exception {
    FirebaseCrashlytics.getInstance()
        .log(
            "runCommands: <router="
                + router
                + " / cmdToExecute="
                + Arrays.toString(cmdToExecute)
                + ">");

    checkDataSyncAlllowedByUsagePreference(context);

    ChannelExec channelExec = null;
    InputStream in = null;
    InputStream err = null;

    try {
      final Session jschSession = router.getSSHSession();
      if (jschSession == null) {
        throw new IllegalStateException("Unable to retrieve session - please retry again later!");
      }
      if (!jschSession.isConnected()) {
        jschSession.connect(CONNECT_TIMEOUT_MILLIS);
      }

      channelExec = (ChannelExec) jschSession.openChannel("exec");

      channelExec.setCommand("( " + commandsJoiner.join(cmdToExecute) + " ) ; echo $?");
      channelExec.setInputStream(null);
      in = channelExec.getInputStream();
      err = channelExec.getErrStream();
      channelExec.connect();

      final String[] output = Utils.getLines(new BufferedReader(new InputStreamReader(in)));
      if (output == null || output.length == 0) {
        return -10;
      }
      try {
        // Last line is the status code
        return Integer.parseInt(output[output.length - 1]);
      } catch (final Exception e) {
        e.printStackTrace();
        return -100;
      }
      //            FirebaseCrashlytics.getInstance().log("output: " + Arrays.toString(output));

      // Line below does not return the actual status
      //            return channelExec.getExitStatus();
      //            return 0;

    } finally {
      Closeables.closeQuietly(in);
      Closeables.closeQuietly(err);
      if (channelExec != null) {
        channelExec.disconnect();
      }
    }
  }

  public static int runCommands(
      Context context,
      @NonNull SharedPreferences globalSharedPreferences,
      @NonNull final Router router,
      @NonNull final String... cmdToExecute)
      throws Exception {

    checkDataSyncAlllowedByUsagePreference(context);

    return runCommands(
        context, globalSharedPreferences, router, Joiner.on(" && ").skipNulls(), cmdToExecute);
  }

  public static boolean scpFrom(
      Context ctx,
      @Nullable final Router router,
      SharedPreferences globalPreferences,
      @NonNull final String fromRemotePath,
      @NonNull final String toLocalPath,
      boolean skipDataSyncPreferene)
      throws Exception {
    FirebaseCrashlytics.getInstance()
        .log(
            "scpFrom: <router="
                + router
                + " / fromRemotePath="
                + fromRemotePath
                + ", toLocalPath="
                + toLocalPath
                + ">");
    if (router == null) {
      throw new IllegalArgumentException("No connection parameters");
    }
    //
    if (!skipDataSyncPreferene) {
      checkDataSyncAlllowedByUsagePreference(ctx);
    }

    FileInputStream fis = null;
    FileOutputStream fos = null;
    ChannelExec channelExec = null;
    OutputStream out = null;
    InputStream in = null;

    String command = "scp -q -o StrictHostKeyChecking=no -f " + fromRemotePath;
    FirebaseCrashlytics.getInstance().log("scpTo: command=[" + command + "]");

    try {
      final Session jschSession = router.getSSHSession();
      if (jschSession == null) {
        throw new IllegalStateException("Unable to retrieve session - please retry again later!");
      }
      if (!jschSession.isConnected()) {
        jschSession.connect(CONNECT_TIMEOUT_MILLIS);
      }

      channelExec = (ChannelExec) jschSession.openChannel("exec");
      channelExec.setCommand(command);

      /*
       * Forcing allocation of a pseudo-TTY prevents SCP from working
       * correctly
       */
      channelExec.setPty(false);

      // get I/O streams for remote scp
      out = channelExec.getOutputStream();
      in = channelExec.getInputStream();

      channelExec.connect();

      byte[] buf = new byte[1024];

      // send '\0'
      buf[0] = 0;
      out.write(buf, 0, 1);
      out.flush();

      while (true) {
        int c = checkAck(in);
        if (c != 'C') {
          break;
        }

        // read '0644 '
        in.read(buf, 0, 5);

        long filesize = 0L;
        while (true) {
          if (in.read(buf, 0, 1) < 0) {
            // error
            break;
          }
          if (buf[0] == ' ') {
            break;
          }
          filesize = filesize * 10L + (long) (buf[0] - '0');
        }

        String file = null;
        for (int i = 0; ; i++) {
          in.read(buf, i, 1);
          if (buf[i] == (byte) 0x0a) {
            file = new String(buf, 0, i, Charset.defaultCharset());
            break;
          }
        }

        // System.out.println("filesize="+filesize+", file="+file);

        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();

        // read a content of lfile
        fos = new FileOutputStream(toLocalPath);
        int foo;
        while (true) {
          if (buf.length < filesize) {
            foo = buf.length;
          } else {
            foo = (int) filesize;
          }
          foo = in.read(buf, 0, foo);
          if (foo < 0) {
            // error
            break;
          }
          fos.write(buf, 0, foo);
          filesize -= foo;
          if (filesize == 0L) {
            break;
          }
        }
        fos.close();
        fos = null;

        if (checkAck(in) != 0) {
          return false;
        }

        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();
      }

      out.close();
      in.close();
      channelExec.disconnect();
    } catch (final IOException ioe) {
      try {
        if (fos != null) {
          fos.close();
        }
      } catch (final IOException ee) {
        ee.printStackTrace();
        // no worries
      }
      return false;
    } finally {
      Closeables.closeQuietly(in);
      //            Closeables.closeQuietly(err);
      if (channelExec != null && channelExec.isConnected()) {
        channelExec.disconnect();
      }
    }

    return true;
  }

  public static boolean scpTo(
      Context ctx,
      @Nullable final Router router,
      SharedPreferences globalPreferences,
      @NonNull final String fromLocalPath,
      @NonNull final String toRemotePath)
      throws Exception {
    FirebaseCrashlytics.getInstance()
        .log(
            "scpTo: <router="
                + router
                + " / fromLocalPath="
                + fromLocalPath
                + ", toRemotePath="
                + toRemotePath
                + ">");
    if (router == null) {
      throw new IllegalArgumentException("No connection parameters");
    }

    checkDataSyncAlllowedByUsagePreference(ctx);

    FileInputStream fis = null;
    ChannelExec channelExec = null;
    OutputStream out = null;
    InputStream in = null;

    String command = "scp -q -o StrictHostKeyChecking=no -t " + toRemotePath;
    FirebaseCrashlytics.getInstance().log("scpTo: command=[" + command + "]");

    try {
      final Session jschSession = router.getSSHSession();
      if (jschSession == null) {
        throw new IllegalStateException("Unable to retrieve session - please retry again later!");
      }
      if (!jschSession.isConnected()) {
        jschSession.connect(CONNECT_TIMEOUT_MILLIS);
      }

      channelExec = (ChannelExec) jschSession.openChannel("exec");
      channelExec.setCommand(command);

      /*
       * Forcing allocation of a pseudo-TTY prevents SCP from working
       * correctly
       */
      channelExec.setPty(false);

      // get I/O streams for remote scp
      out = channelExec.getOutputStream();
      in = channelExec.getInputStream();

      channelExec.connect();

      final int checkAck = checkAck(in);
      if (checkAck != 0) {
        return closeChannel(channelExec, out);
      }

      final File _lfile = new File(fromLocalPath);
      // send "C0644 filesize filename", where filename should not include
      // '/'
      final long filesize = _lfile.length();
      command = "C0644 " + filesize + " ";
      if (fromLocalPath.lastIndexOf('/') > 0) {
        command += fromLocalPath.substring(fromLocalPath.lastIndexOf('/') + 1);
      } else {
        command += fromLocalPath;
      }
      command += "\n";

      out.write(command.getBytes(StandardCharsets.UTF_8));
      out.flush();
      if (checkAck(in) != 0) {
        return closeChannel(channelExec, out);
      }

      // send a content of lfile
      fis = new FileInputStream(fromLocalPath);
      final byte[] buf = new byte[1024];
      while (true) {
        final int len = fis.read(buf, 0, buf.length);
        if (len <= 0) {
          break;
        }
        out.write(buf, 0, len); // out.flush();
      }
      fis.close();
      fis = null;
      // send '\0'
      buf[0] = 0;
      out.write(buf, 0, 1);
      out.flush();

      // checkAck(in);
      if (checkAck(in) != 0) {
        return closeChannel(channelExec, out);
      }
      out.close();
    } catch (final IOException ioe) {
      try {
        if (fis != null) {
          fis.close();
        }
      } catch (final IOException ee) {
        ee.printStackTrace();
        // No worries
      }
      return false;
    } finally {
      Closeables.closeQuietly(in);
      //            Closeables.closeQuietly(err);
      if (channelExec != null && channelExec.isConnected()) {
        channelExec.disconnect();
      }
    }

    return true;
  }

  private SSHUtils() {}

  private static int checkAck(@NonNull final InputStream in) throws IOException {
    final int b = in.read();
    // b may be 0 for success,
    // 1 for error,
    // 2 for fatal error,
    // -1
    if (b == 0) {
      return b;
    }
    if (b == -1) {
      return b;
    }

    if (b == 1 || b == 2) {
      final StringBuilder sb = new StringBuilder();
      int c;
      do {
        c = in.read();
        sb.append((char) c);
      } while (c != '\n');
      if (b == 1) { // error
        FirebaseCrashlytics.getInstance().log(sb.toString());
      }
      if (b == 2) { // fatal error
        FirebaseCrashlytics.getInstance().log(sb.toString());
      }
    }
    return b;
  }

  /**
   * @throws java.io.IOException
   */
  private static boolean closeChannel(
      @NonNull final Channel channel, @Nullable final OutputStream out) throws IOException {

    if (out != null) {
      out.close();
    }
    channel.disconnect();
    return false;
  }

  static {
    FIRMWARE_AUTODETECT_CMDS.put(DDWRT, "grep -qi \"dd-wrt\" /tmp/loginprompt");
    FIRMWARE_AUTODETECT_CMDS.put(TOMATO, "grep -qi \"tomato\" /tmp/etc/motd");
    // TODO Add other firmware commands
    //        FIRMWARE_AUTODETECT_CMDS.put(OPENWRT, "uname -a | grep -qi \"openwrt\"");
  }

  static {
    JSch.setLogger(SSHLogger.INSTANCE);
  }
}
