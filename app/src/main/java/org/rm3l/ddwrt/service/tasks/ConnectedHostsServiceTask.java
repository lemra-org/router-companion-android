package org.rm3l.ddwrt.service.tasks;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.apache.commons.lang3.StringUtils;
import org.rm3l.ddwrt.R;
import org.rm3l.ddwrt.main.DDWRTMainActivity;
import org.rm3l.ddwrt.resources.Device;
import org.rm3l.ddwrt.resources.MACOUIVendor;
import org.rm3l.ddwrt.resources.conn.Router;
import org.rm3l.ddwrt.tiles.DDWRTTile;
import org.rm3l.ddwrt.tiles.status.wireless.WirelessClientsTile;
import org.rm3l.ddwrt.utils.DDWRTCompanionConstants;
import org.rm3l.ddwrt.utils.ReportingUtils;
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
import static org.rm3l.ddwrt.utils.ImageUtils.updateNotificationIconWithRouterAvatar;

/**
 * Created by rm3l on 05/07/15.
 */
public class ConnectedHostsServiceTask extends AbstractBackgroundServiceTask {

    private static final String TAG = ConnectedHostsServiceTask.class.getSimpleName();

    public ConnectedHostsServiceTask(@NonNull Context context) {
        super(context);
    }

    @Override
    public void runBackgroundServiceTask(@NonNull final Router router) throws Exception {

        final SharedPreferences routerPreferences = mCtx.getSharedPreferences(
                router.getUuid(), Context.MODE_PRIVATE);

        final String[] output = SSHUtils.getManualProperty(mCtx,
                router,
                globalPreferences, "grep dhcp-host /tmp/dnsmasq.conf | sed 's/.*=//' | awk -F , '{print \"" +
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

        Crashlytics.log(Log.DEBUG,  TAG, "output: " + Arrays.toString(output));

        if (output == null || output.length == 0) {
            if (output == null) {
                return;
            }
        }
        final Map<String, Device> macToDevice = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final Multimap<String, Device> macToDeviceOutput = HashMultimap.create();

        final Splitter splitter = Splitter.on(" ");

        String ipAddress;
        final Pattern betweenParenthesisPattern = Pattern.compile("\\((.*?)\\)");

        //Active clients
        final String[] activeClients = SSHUtils.getManualProperty(mCtx, router, globalPreferences,
                "arp -a 2>/dev/null");

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
                if (routerPreferences != null) {
                    final String deviceAlias = routerPreferences.getString(macAddress, null);
                    if (!isNullOrEmpty(deviceAlias)) {
                        device.setAlias(deviceAlias);
                    }
                }

                if (activeClients != null) {
                    for (final String activeClient : activeClients) {
                        if (StringUtils.containsIgnoreCase(activeClient, macAddress)) {
                            device.setActive(true);
                            break;
                        }
                    }
                }

                device.setMacouiVendorDetails(WirelessClientsTile.mMacOuiVendorLookupCache.getUnchecked(macAddress));

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

        generateConnectedHostsNotification(mCtx, routerPreferences,
                router, macToDevice.values());
    }

    public static void generateConnectedHostsNotification(@NonNull Context mCtx,
                                                          @NonNull SharedPreferences mRouterPreferences,
                                                          @Nullable Router router,
                                                          @NonNull Collection<Device> deviceCollection) {

        if (!mCtx.getSharedPreferences(DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE).getStringSet(DDWRTCompanionConstants.NOTIFICATIONS_CHOICE_PREF,
                new HashSet<String>()).contains(ConnectedHostsServiceTask.class.getSimpleName())) {
            Crashlytics.log(Log.DEBUG,  TAG, "ConnectedHostsServiceTask notifications disabled");
            return;
        }

        if (router == null) {
            Crashlytics.log(Log.WARN, TAG, "router == null");
            return;
        }

        final NotificationManager mNotificationManager = (NotificationManager)
                mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        // Sets an ID for the notification, so it can be updated
        final int notifyID = router.getId();

        final boolean onlyActiveHosts = mRouterPreferences
                .getBoolean(DDWRTCompanionConstants.NOTIFICATIONS_CONNECTED_HOSTS_ACTIVE_ONLY, true);

        Crashlytics.log(Log.DEBUG,  TAG, "onlyActiveHosts=" + onlyActiveHosts);
        Crashlytics.log(Log.DEBUG,  TAG, "deviceCollection=" + deviceCollection);

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
                ReportingUtils.reportException(null, new
                        IllegalStateException("Failed to decode and parse JSON: " + devStrEncrypted, e));
            }
        }

        Crashlytics.log(Log.DEBUG,  TAG, "<sizeFiltered,previousConnectedHosts.size()>=<" +
                sizeFiltered + "," + previousConnectedHosts.size() + ">");

        boolean updateNotification = false;
        if (sizeFiltered != previousConnectedHosts.size()) {
            updateNotification = true;
        } else {

            Crashlytics.log(Log.DEBUG,  TAG, "devicesCollFiltered: " + devicesCollFiltered);
            Crashlytics.log(Log.DEBUG,  TAG, "previousConnectedHosts: " + previousConnectedHosts);

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

        Crashlytics.log(Log.DEBUG,  TAG, "updateNotification=" + updateNotification);

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

        Utils.requestBackup(mCtx);

        Crashlytics.log(Log.DEBUG,  TAG, "NOTIFICATIONS_ENABLE=" + mRouterPreferences
                .getBoolean(DDWRTCompanionConstants.NOTIFICATIONS_ENABLE, true));

        if (sizeFiltered == 0 ||
                !mRouterPreferences
                        .getBoolean(DDWRTCompanionConstants.NOTIFICATIONS_ENABLE, true)) {
            mNotificationManager.cancel(notifyID);
        } else {

            if (updateNotification) {

                final Bitmap largeIcon = BitmapFactory.decodeResource(
                        mCtx.getResources(),
                        R.drawable.ic_launcher_ddwrt_companion);

                final Intent resultIntent = new Intent(mCtx,
                        DDWRTMainActivity.class);
                resultIntent.putExtra(ROUTER_SELECTED, router.getUuid());
                resultIntent.putExtra(DDWRTMainActivity.SAVE_ITEM_SELECTED, 4); //Open right on Clients Section
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
                        .setSmallIcon(R.drawable.ic_connected_hosts_notification)
//                        .setLargeIcon(largeIcon)
                        .setAutoCancel(true)
                        .setGroup(WirelessClientsTile.class.getSimpleName())
                        .setGroupSummary(true)
                        .setContentIntent(resultPendingIntent);
//                                .setDefaults(Notification.DEFAULT_ALL);

                //Notification sound, if required
                final SharedPreferences sharedPreferences = mCtx.getSharedPreferences(
                        DDWRTCompanionConstants.DEFAULT_SHARED_PREFERENCES_KEY,
                        Context.MODE_PRIVATE);
                final String ringtoneUri = sharedPreferences
                        .getString(DDWRTCompanionConstants.NOTIFICATIONS_SOUND, null);
                if (ringtoneUri != null) {
                    mBuilder.setSound(Uri.parse(ringtoneUri), AudioManager.STREAM_NOTIFICATION);
                }

                if (!sharedPreferences
                        .getBoolean(DDWRTCompanionConstants.NOTIFICATIONS_VIBRATE, true)) {
                    mBuilder
                            .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                            .setVibrate(DDWRTCompanionConstants.NO_VIBRATION_PATTERN);
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
                        final String ouiLine = String.format("NIC Man.   %s",
                                Strings.nullToEmpty(macouiVendorDetails.getCompany()));
                        final Spannable ouiSpannable = new SpannableString(ouiLine);
                        ouiSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                0, "NIC Man.".length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        inboxStyle.addLine(ouiSpannable);
                    }

                    mBuilder.setContentText(summaryText);

                } else {

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
                                (macouiVendorDetails != null &&
                                        !Strings.isNullOrEmpty(macouiVendorDetails.getCompany()))?
                                        String.format(" (%s)",
                                                macouiVendorDetails.getCompany()) :
                                        "");
                        final Spannable sb = new SpannableString(line);
                        sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                0, deviceNameToDisplay.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        inboxStyle.addLine(sb);
                    }

                    mBuilder.setContentText(summaryText);
                    mBuilder.setNumber(sizeFiltered);

                }

                // Moves the expanded layout object into the notification object.
                mBuilder.setStyle(inboxStyle);

                // Because the ID remains unchanged, the existing notification is
                // updated.
                final Notification notification = mBuilder.build();
                mNotificationManager.notify(notifyID, notification);
                updateNotificationIconWithRouterAvatar(mCtx, router, notifyID, notification);


            }
        }
    }

}
