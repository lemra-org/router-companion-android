package org.rm3l.ddwrt.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.mgmt.RouterManagementActivity;
import org.rm3l.ddwrt.mgmt.dao.DDWRTCompanionDAO;
import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.resources.MACOUIVendor;
import org.rm3l.ddwrt.resources.RouterData;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.SSHUtils;
import org.rm3l.ddwrt.utils.Utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.rm3l.ddwrt.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.ddwrt.resources.Encrypted.d;
import static org.rm3l.ddwrt.resources.Encrypted.e;
import static org.rm3l.ddwrt.tiles.services.wol.WakeOnLanTile.GSON_BUILDER;
import static org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile.CASE_INSENSITIVE_STRING_ORDERING;
import static org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile.CONNECTED_HOSTS;
import static org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile.DEVICE_NAME_FOR_NOTIFICATION;
import static org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile.IP_ADDRESS;
import static org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile.MAC_ADDRESS;
import static org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile.MAP_KEYWORD;
import static org.rm3l.ddwrt.utils.DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY;

public class ConnectedHostsService extends Service {

    private static final String TAG = ConnectedHostsService.class.getSimpleName();

    private PowerManager.WakeLock mWakeLock;

    private SharedPreferences mGlobalPreferences;

    private SharedPreferences mRouterPreferences;

    /**
     * Simply return null, since our Service will not be communicating with
     * any other components. It just does its work silently.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This is where we initialize. We call this when onStart/onStartCommand is
     * called by the system. We won't do anything with the intent here, and you
     * probably won't, either.
     */
    private void handleIntent(Intent intent) {
        // obtain the wake lock
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.acquire();

        // check the global background data setting
        String routerUuid;
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (intent == null || cm == null || cm.getActiveNetworkInfo() == null || 
                (routerUuid = intent
                        .getStringExtra(RouterManagementActivity.ROUTER_SELECTED)) == null) {
            /*
             * If getActiveNetworkInfo() is null, you do not have a network connection,
             * either because the device does not have a network connection,
             * or because user settings (e.g., bandwidth caps) prevent your app
             * from having a network connection.
             */
            stopSelf();
            return;
        }

        mGlobalPreferences = getSharedPreferences(DEFAULT_SHARED_PREFERENCES_KEY,
                        Context.MODE_PRIVATE);

        mRouterPreferences = getSharedPreferences(routerUuid, Context.MODE_PRIVATE);

        final DDWRTCompanionDAO dao = RouterManagementActivity.getDao(this);

        final Router router = dao.getRouter(routerUuid);
        if (router == null) {
            stopSelf();
            return;
        }

        // do the actual work, in a separate thread
        new PollTask().execute(router);
    }

    private class PollTask extends AsyncTask<Router, Void, RouterData<Collection<Device>>> {

        /**
         * This is where YOU do YOUR work. There's nothing for me to write here
         * you have to fill this in. Make your HTTP request(s) or whatever it is
         * you have to do to get your updates in here, because this is run in a
         * separate thread
         */
        @Override
        protected RouterData<Collection<Device>> doInBackground(Router... params) {
            
            if (params == null || params.length == 0) {
                return null;
            }

            final Router router = params[0];

            try {

                final String[] output = SSHUtils.getManualProperty(ConnectedHostsService.this,
                        router,
                        mGlobalPreferences, "grep dhcp-host /tmp/dnsmasq.conf | sed 's/.*=//' | awk -F , '{print \"" +
                                MAP_KEYWORD +
                                "\",$1,$3 ,$2}'",
                        "awk '{print \"" +
                                MAP_KEYWORD +
                                "\",$2,$3,$4}' /tmp/dnsmasq.leases",
                        "awk 'NR>1{print \"" +
                                MAP_KEYWORD +
                                "\",$4,$1,\"*\"}' /proc/net/arp",
                        "arp -a | awk '{print \"" +
                                MAP_KEYWORD +
                                "\",$4,$2,$1}'",
                        "echo done");

                Log.d(TAG, "output: " + Arrays.toString(output));

                if (output == null || output.length == 0) {
                    if (output == null) {
                        return null;
                    }
                }
                final Map<String, Device> macToDevice = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                final Multimap<String, Device> macToDeviceOutput = HashMultimap.create();

                final Splitter splitter = Splitter.on(" ");

                String ipAddress;
                final Pattern betweenParenthesisPattern = Pattern.compile("\\((.*?)\\)");
                for (final String stdoutLine : output) {
                    if ("done".equals(stdoutLine)) {
                        break;
                    }
                    final List<String> as = splitter.splitToList(stdoutLine);
                    if (as.size() >= 4 && MAP_KEYWORD.equals(as.get(0))) {
                        final String macAddress = Strings.nullToEmpty(as.get(1)).toLowerCase();
                        if (isNullOrEmpty(macAddress) ||
                                "00:00:00:00:00:00".equals(macAddress) ||
                                StringUtils.containsIgnoreCase(macAddress, "incomplete")) {
                            //Skip clients with incomplete ARP set-up
                            continue;
                        }

                        ipAddress = as.get(2);
                        if (ipAddress != null) {
                            final Matcher matcher = betweenParenthesisPattern.matcher(ipAddress);
                            if (matcher.find()) {
                                ipAddress = matcher.group(1);
                            }
                        }

                        final Device device = new Device(macAddress);
                        device.setIpAddress(ipAddress);

                        final String systemName = as.get(3);
                        if (!"*".equals(systemName)) {
                            device.setSystemName(systemName);
                        }

                        //Alias from SharedPreferences
                        if (mRouterPreferences != null) {
                            final String deviceAlias = mRouterPreferences.getString(macAddress, null);
                            if (!isNullOrEmpty(deviceAlias)) {
                                device.setAlias(deviceAlias);
                            }
                        }

                        device.setMacouiVendorDetails(WirelessClientsTile.mMacOuiVendorLookupCache.get(macAddress));

                        macToDeviceOutput.put(macAddress, device);
                    }
                }

                for (final Map.Entry<String, Collection<Device>> deviceEntry : macToDeviceOutput.asMap().entrySet()) {
                    final String macAddr = deviceEntry.getKey();
                    final Collection<Device> deviceCollection = deviceEntry.getValue();
                    for (final Device device : deviceCollection) {
                        //Consider the one that has a Name, if any
                        if (!isNullOrEmpty(device.getSystemName())) {
                            macToDevice.put(macAddr, device);
                            break;
                        }
                    }
                    if (deviceCollection.isEmpty() || macToDevice.containsKey(macAddr)) {
                        continue;
                    }

                    final Device dev = deviceCollection.iterator().next();

                    macToDevice.put(macAddr, dev);
                }

                final Collection<Device> deviceCollection = macToDevice.values();

                return new RouterData<Collection<Device>>() {
                }.setData(deviceCollection).setRouter(router);

            } catch (@NonNull final Exception e) {
                Log.e(TAG, e.getMessage() + ": " + Throwables.getStackTraceAsString(e));
                return new RouterData<Collection<Device>>() {
                }.setException(e).setRouter(router);
            }
        }

        /**
         * In here you should interpret whatever you fetched in doInBackground
         * and push any notifications you need to the status bar, using the
         * NotificationManager. I will not cover this here, go check the docs on
         * NotificationManager.
         *
         * What you HAVE to do is call stopSelf() after you've pushed your
         * notification(s). This will:
         * 1) Kill the service so it doesn't waste precious resources
         * 2) Call onDestroy() which will release the wake lock, so the device
         *    can go to sleep again and save precious battery.
         */
        @Override
        protected void onPostExecute(RouterData<Collection<Device>> result) {
            try {
                // handle your data
                if (result == null) {
                    return;
                }
                final Exception exception = result.getException();
                if (exception != null) {
                    Utils.reportException(exception);
                    return;
                }
                if (result.getData() == null) {
                    return;
                }
                final Router router = result.getRouter();
                if (router == null) {
                    return;
                }

                final Collection<Device> deviceCollection = result.getData();

                generateConnectedHostsNotification(ConnectedHostsService.this, mRouterPreferences,
                        router, deviceCollection);

            } finally {
                ConnectedHostsService.this.stopSelf();
            }
        }

    }

    /**
     * This is deprecated, but you have to implement it if you're planning on
     * supporting devices with an API level lower than 5 (Android 2.0).
     */
    @Override
    public void onStart(Intent intent, int startId) {
        handleIntent(intent);
    }

    /**
     * This is called on 2.0+ (API level 5 or higher). Returning
     * START_NOT_STICKY tells the system to not restart the service if it is
     * killed because of poor resource (memory/cpu) conditions.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_NOT_STICKY;
    }

    /**
     * In onDestroy() we release our wake lock. This ensures that whenever the
     * Service stops (killed for resources, stopSelf() called, etc.), the wake
     * lock will be released.
     */
    public void onDestroy() {
        super.onDestroy();
        mWakeLock.release();
    }

    public static void generateConnectedHostsNotification(@NonNull Context mCtx,
                                                          @NonNull SharedPreferences mRouterPreferences,
                                                          @Nullable Router router,
                                                          @NonNull Collection<Device> deviceCollection) {

        if (router == null) {
            return;
        }

        final NotificationManager mNotificationManager = (NotificationManager)
                mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        // Sets an ID for the notification, so it can be updated
        final int notifyID = router.getId();


        final boolean onlyActiveHosts = mRouterPreferences
                .getBoolean("notifications.connectedHosts.activeOnly", true);
        final ImmutableSet<Device> devicesCollFiltered = FluentIterable.from(deviceCollection)
                .filter(new Predicate<Device>() {
                    @Override
                    public boolean apply(@Nullable Device input) {
                        return ((!onlyActiveHosts) ||
                                (input != null && input.isActive()));
                    }
                }).toSortedSet(new Comparator<Device>() {
                    @Override
                    public int compare(Device lhs, Device rhs) {
                        if (lhs == rhs) {
                            return 0;
                        }
                        if (lhs == null) {
                            return -1;
                        }
                        if (rhs == null) {
                            return 1;
                        }
                        return ComparisonChain.start()
                                .compare(lhs.getAliasOrSystemName(),
                                        rhs.getAliasOrSystemName(),
                                        CASE_INSENSITIVE_STRING_ORDERING)
                                .result();
                    }
                });
        final int sizeFiltered = devicesCollFiltered.size();

        final Set<Device> previousConnectedHosts = new HashSet<>();
        final Set<String> devicesStringSet = new HashSet<>(mRouterPreferences
                .getStringSet(DDWRTTile.getFormattedPrefKey(WirelessClientsTile.class,
                                CONNECTED_HOSTS),
                        new HashSet<String>()));
        for (final String devStrEncrypted : devicesStringSet) {
            try {
                final String devStr = d(devStrEncrypted);
                if (isNullOrEmpty(devStr)) {
                    continue;
                }

                final Map objFromJson = GSON_BUILDER.create()
                        .fromJson(devStr, Map.class);

                final Object macAddress = objFromJson.get(MAC_ADDRESS);
                if (macAddress == null || macAddress.toString().isEmpty()) {
                    continue;
                }

                //IP Address may change as well
                final Device deviceFromJson = new Device(macAddress.toString());

                final Object ipAddr = objFromJson.get(IP_ADDRESS);
                if (ipAddr != null) {
                    deviceFromJson.setIpAddress(ipAddr.toString());
                }

                //Display name may change too
                final Object displayName = objFromJson.get(DEVICE_NAME_FOR_NOTIFICATION);
                if (displayName != null) {
                    deviceFromJson.setDeviceNameForNotification(displayName.toString());
                }

                previousConnectedHosts.add(deviceFromJson);

            } catch (final Exception e) {
                //No worries
                e.printStackTrace();
                Utils.reportException(new
                        IllegalStateException("Failed to decode and parse JSON: " + devStrEncrypted, e));
            }
        }

        boolean updateNotification = false;
        if (sizeFiltered != previousConnectedHosts.size()) {
            updateNotification = true;
        } else {

            //Now compare if anything has changed
            for (final Device newDevice : devicesCollFiltered) {
                if (newDevice == null) {
                    continue;
                }
                final String deviceMacAddress = newDevice.getMacAddress();
                final String deviceIpAddress = newDevice.getIpAddress();
                final String deviceDisplayName = newDevice.getAliasOrSystemName();

                boolean deviceFound = false;
                boolean deviceHasChanged = false;
                for (final Device previousConnectedHost : previousConnectedHosts) {
                    if (previousConnectedHost == null) {
                        continue;
                    }
                    if (!previousConnectedHost.getMacAddress()
                            .equalsIgnoreCase(deviceMacAddress)) {
                        continue;
                    }
                    deviceFound = true;
                    //Device found - now analyze if something has changed
                    if (!(Objects
                            .equal(deviceIpAddress, previousConnectedHost.getIpAddress())
                            && Objects
                            .equal(deviceDisplayName,
                                    previousConnectedHost.getDeviceNameForNotification()))) {
                        deviceHasChanged = true;
                        break;
                    }
                }
                if (deviceHasChanged || !deviceFound) {
                    //This is a new device or an updated one - so update is needed
                    updateNotification = true;
                    break;
                }
            }
        }

        //Build the String Set to save in preferences
        final ImmutableSet<String> stringImmutableSet = FluentIterable.from(devicesCollFiltered)
                .transform(new Function<Device, String>() {
                    @Override
                    public String apply(@Nullable Device device) {
                        if (device == null) {
                            return e(DDWRTCompanionConstants.EMPTY_STRING);
                        }
                        final Map<String, String> details = Maps.newHashMap();
                        details.put(MAC_ADDRESS, device.getMacAddress());
                        details.put(IP_ADDRESS, device.getIpAddress());
                        details.put(DEVICE_NAME_FOR_NOTIFICATION,
                                device.getAliasOrSystemName());

                        return e(GSON_BUILDER.create().toJson(details));
                    }
                }).toSet();

        mRouterPreferences.edit()
                .remove(DDWRTTile.getFormattedPrefKey(WirelessClientsTile.class, CONNECTED_HOSTS))
                .apply();

        mRouterPreferences.edit()
                .putStringSet(DDWRTTile.getFormattedPrefKey(WirelessClientsTile.class, CONNECTED_HOSTS),
                        stringImmutableSet)
                .apply();

        if (sizeFiltered == 0 ||
                !mRouterPreferences
                        .getBoolean(DDWRTCompanionConstants.NOTIFICATIONS_ENABLE, true)) {
            mNotificationManager.cancel(notifyID);
        } else {

            final Bitmap largeIcon = BitmapFactory.decodeResource(
                    mCtx.getResources(),
                    R.drawable.ic_launcher_ddwrt_companion);

            if (updateNotification) {

                final Intent resultIntent = new Intent(mCtx,
                        DDWRTMainActivity.class);
                resultIntent.putExtra(ROUTER_SELECTED, router.getUuid());
                resultIntent.putExtra(DDWRTMainActivity.SAVE_ITEM_SELECTED, 3); //Open right on Clients Section
                // Because clicking the notification opens a new ("special") activity, there's
                // no need to create an artificial back stack.
                final PendingIntent resultPendingIntent =
                        PendingIntent.getActivity(
                                mCtx,
                                0,
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                final String mRouterName = router.getName();
                final boolean mRouterNameNullOrEmpty = isNullOrEmpty(mRouterName);
                String summaryText = "";
                if (!mRouterNameNullOrEmpty) {
                    summaryText = (mRouterName + " (");
                }
                summaryText += router.getRemoteIpAddress();
                if (!mRouterNameNullOrEmpty) {
                    summaryText += ")";
                }

                final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                        mCtx)
                        .setSmallIcon(R.drawable.ic_launcher_ddwrt_companion)
                        .setLargeIcon(largeIcon)
                        .setAutoCancel(true)
                        .setGroup(WirelessClientsTile.class.getSimpleName())
                        .setGroupSummary(true)
                        .setContentIntent(resultPendingIntent);
//                                .setDefaults(Notification.DEFAULT_ALL);

                //Notification sound, if required
                final String ringtoneUri = mRouterPreferences
                        .getString(DDWRTCompanionConstants.NOTIFICATIONS_SOUND, null);
                if (ringtoneUri != null) {
                    mBuilder.setSound(Uri.parse(ringtoneUri), AudioManager.STREAM_NOTIFICATION);
                }

                final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                        .setSummaryText(summaryText);

                final String newDevicesTitle = String.format("%d connected host%s",
                        sizeFiltered, sizeFiltered > 1 ? "s" : "");

                mBuilder.setContentTitle(newDevicesTitle);

                inboxStyle.setBigContentTitle(newDevicesTitle);

                if (sizeFiltered == 1) {
                    //Only one device
                    final Device device = devicesCollFiltered.iterator().next();
                    final String deviceAliasOrSystemName = device.getAliasOrSystemName();

//                                mBuilder.setContentTitle(deviceNameToDisplay);

//                                inboxStyle.setBigContentTitle(deviceNameToDisplay);
                    //Name
                    if (!isNullOrEmpty(deviceAliasOrSystemName)) {
                        String nameLine = String.format("Name   %s", deviceAliasOrSystemName);
                        final Spannable nameSpannable = new SpannableString(nameLine);
                        nameSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                0, "Name".length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        inboxStyle.addLine(nameSpannable);
                    }

                    //IP Address
                    String ipLine = String.format("IP   %s", device.getIpAddress());
                    final Spannable ipSpannable = new SpannableString(ipLine);
                    ipSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                            0, "IP".length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    inboxStyle.addLine(ipSpannable);

                    //MAC Address
                    final String macLine = String.format("MAC   %s", device.getMacAddress());
                    final Spannable macSpannable = new SpannableString(macLine);
                    macSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                            0, "MAC".length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    inboxStyle.addLine(macSpannable);

                    final MACOUIVendor macouiVendorDetails = device.getMacouiVendorDetails();
                    if (macouiVendorDetails != null) {
                        //NIC Manufacturer
                        final String ouiLine = String.format("OUI   %s",
                                Strings.nullToEmpty(macouiVendorDetails.getCompany()));
                        final Spannable ouiSpannable = new SpannableString(ouiLine);
                        ouiSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                0, "OUI".length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        inboxStyle.addLine(ouiSpannable);
                    }

                    mBuilder.setContentText(ipSpannable);

                } else {

                    Spannable firstLine = null;
                    for (final Device device : devicesCollFiltered) {
                        final MACOUIVendor macouiVendorDetails = device.getMacouiVendorDetails();
                        final String deviceAliasOrSystemName = device.getAliasOrSystemName();
                        final String deviceNameToDisplay = isNullOrEmpty(deviceAliasOrSystemName) ?
                                device.getMacAddress() : deviceAliasOrSystemName;
                        final String line = String.format("%s   %s%s%s",
                                deviceNameToDisplay,
                                device.getIpAddress(),
                                isNullOrEmpty(deviceAliasOrSystemName) ?
                                        "" : String.format(" | %s", device.getMacAddress()),
                                macouiVendorDetails != null ? String.format(" (%s)",
                                        macouiVendorDetails.getCompany()) : "");
                        final Spannable sb = new SpannableString(line);
                        sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                0, deviceNameToDisplay.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        inboxStyle.addLine(sb);
                        if (firstLine == null) {
                            firstLine = sb;
                        }
                    }

                    mBuilder.setContentText(firstLine);
                    mBuilder.setNumber(sizeFiltered);

                }

                // Moves the expanded layout object into the notification object.
                mBuilder.setStyle(inboxStyle);

                // Because the ID remains unchanged, the existing notification is
                // updated.
                mNotificationManager.notify(notifyID, mBuilder.build());
            }
        }
    }
}
