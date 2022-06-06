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

package org.rm3l.router_companion.resources.conn;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.rm3l.router_companion.fragments.AbstractBaseFragment.getTabsForDDWRT;
import static org.rm3l.router_companion.fragments.AbstractBaseFragment.getTabsForTomato;
import static org.rm3l.router_companion.mgmt.RouterManagementActivity.ROUTER_SELECTED;
import static org.rm3l.router_companion.resources.Encrypted.d;
import static org.rm3l.router_companion.resources.Encrypted.e;
import static org.rm3l.router_companion.utils.SSHUtils.CONNECT_TIMEOUT_MILLIS;
import static org.rm3l.router_companion.utils.SSHUtils.MAX_NUMBER_OF_CONCURRENT_SSH_SESSIONS_PER_ROUTER;
import static org.rm3l.router_companion.utils.SSHUtils.NO;
import static org.rm3l.router_companion.utils.SSHUtils.STRICT_HOST_KEY_CHECKING;
import static org.rm3l.router_companion.utils.SSHUtils.YES;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Pair;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Striped;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.rm3l.ddwrt.R;
import org.rm3l.router_companion.RouterCompanionAppConstants;
import org.rm3l.router_companion.RouterCompanionApplication;
import org.rm3l.router_companion.common.resources.RouterInfo;
import org.rm3l.router_companion.fragments.AbstractBaseFragment;
import org.rm3l.router_companion.fragments.FragmentTabDescription;
import org.rm3l.router_companion.main.DDWRTMainActivity;
import org.rm3l.router_companion.tiles.services.wol.WakeOnLanTile;
import org.rm3l.router_companion.utils.ImageUtils;
import org.rm3l.router_companion.utils.ReportingUtils;
import org.rm3l.router_companion.utils.Utils;

/**
 * Encapsulates everything needed to establish a connection to a given router.
 *
 * <p>Connections can be either SSH-based or HTTP(S)-based.
 *
 * @author <a href="mailto:armel+router_companion@rm3l.org">Armel S.</a>
 */
public class Router implements Serializable {

  public static class RouterForSessionCache {

    private final String ipAddr;

    private final String login;

    private final Integer port;

    @NonNull private final Router router;

    public RouterForSessionCache(@NonNull final Router router) {
      this.router = router;
      final Pair<String, Integer> effectiveIpAndPortTuple = router.getEffectiveIpAndPortTuple();
      this.ipAddr = effectiveIpAndPortTuple.first;
      this.port = effectiveIpAndPortTuple.second;
      this.login = router.getUsernamePlain();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final RouterForSessionCache that = (RouterForSessionCache) o;

      if (ipAddr != null ? !ipAddr.equals(that.ipAddr) : that.ipAddr != null) {
        return false;
      }
      if (port != null ? !port.equals(that.port) : that.port != null) {
        return false;
      }
      return !(login != null ? !login.equals(that.login) : that.login != null);
    }

    @NonNull
    public Router getRouter() {
      return router;
    }

    @Override
    public int hashCode() {
      int result = ipAddr != null ? ipAddr.hashCode() : 0;
      result = 31 * result + (port != null ? port.hashCode() : 0);
      result = 31 * result + (login != null ? login.hashCode() : 0);
      return result;
    }
  }

  public static class LocalSSIDLookup {

    private String networkSsid;

    private int port = 22;

    private String reachableAddr;

    public String getNetworkSsid() {
      return networkSsid;
    }

    public void setNetworkSsid(String networkSsid) {
      this.networkSsid = networkSsid;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public String getReachableAddr() {
      return reachableAddr;
    }

    public void setReachableAddr(String reachableAddr) {
      this.reachableAddr = reachableAddr;
    }
  }

  private static class RouterShortcutAvatarDownloader implements Target {

    private final String mRouterUuid;

    final int mIconSize;

    @Nullable private final Context mApplicationContext;

    @Nullable private final Intent mHomeLauncherShortcutIntent;

    @Nullable private final Intent mOpenRouterShortcutIntent;

    private RouterShortcutAvatarDownloader(
        @NonNull final Router router,
        @Nullable final Context applicationContext,
        @Nullable final Intent openRouterIntent,
        @Nullable final Intent homeLauncherShortcutIntent) {
      this.mRouterUuid = router.getUuid();
      this.mApplicationContext = applicationContext;
      this.mOpenRouterShortcutIntent = openRouterIntent;
      this.mHomeLauncherShortcutIntent = homeLauncherShortcutIntent;
      if (this.mApplicationContext != null) {
        this.mIconSize =
            (int)
                this.mApplicationContext.getResources().getDimension(android.R.dimen.app_icon_size);
      } else {
        this.mIconSize = -1;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final RouterShortcutAvatarDownloader that = (RouterShortcutAvatarDownloader) o;
      return Objects.equal(this.getRouterUuid(), that.getRouterUuid());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getRouterUuid());
    }

    @Override
    public void onBitmapFailed(Exception e, android.graphics.drawable.Drawable errorDrawable) {
      // Callback indicating the image could not be successfully loaded.
      // No worries
      Utils.reportException(mApplicationContext, e);
    }

    @Override
    public void onBitmapLoaded(android.graphics.Bitmap bitmap, Picasso.LoadedFrom from) {
      // Callback when an image has been successfully loaded.
      if (mApplicationContext == null || mOpenRouterShortcutIntent == null) {
        return;
      }

      if (mHomeLauncherShortcutIntent != null) {
        // Update home launcher shortcut with actual icon: uninstall then install with new icon
        mHomeLauncherShortcutIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
        mApplicationContext.sendBroadcast(mHomeLauncherShortcutIntent);

        // Now update icon from bitmap
        // Might be needed to call the following lines:
        if (this.mIconSize > 0) {
          mHomeLauncherShortcutIntent.putExtra(
              Intent.EXTRA_SHORTCUT_ICON,
              Bitmap.createScaledBitmap(bitmap, this.mIconSize, this.mIconSize, false));
        } else {
          mHomeLauncherShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
        }

        mHomeLauncherShortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        mApplicationContext.sendBroadcast(mHomeLauncherShortcutIntent);
      }
    }

    @Override
    public void onPrepareLoad(android.graphics.drawable.Drawable placeHolderDrawable) {
      // Callback invoked right before your request is submitted.
    }

    private String getRouterUuid() {
      return this.mRouterUuid;
    }
  }

  /** RouterConnectionProtocol enum */
  public enum RouterConnectionProtocol {
    SSH("ssh", 22, "root", ""),

    HTTP("http", 80, "admin", null),

    HTTPS("https", 443, "admin", null);

    @NonNull private final String channel;

    @Nullable private final String defaultPassword;

    private final int defaultPort;

    @Nullable private final String defaultUsername;

    /**
     * Constructor
     *
     * @param channel the channel
     * @param defaultPort the default port
     * @param defaultUsername the default username
     * @param defaultPassword the default password
     */
    RouterConnectionProtocol(
        @NonNull final String channel,
        final int defaultPort,
        @Nullable final String defaultUsername,
        @Nullable final String defaultPassword) {
      this.channel = channel;
      this.defaultPort = defaultPort;
      this.defaultUsername = defaultUsername;
      this.defaultPassword = defaultPassword;
    }

    /**
     * @return the channel
     */
    @NonNull
    public String getChannel() {
      return channel;
    }

    /**
     * @return the defaultPassword
     */
    @Nullable
    public String getDefaultPassword() {
      return defaultPassword;
    }

    /**
     * @return the defaultPort
     */
    public int getDefaultPort() {
      return defaultPort;
    }

    /**
     * @return the defaultUsername
     */
    @Nullable
    public String getDefaultUsername() {
      return defaultUsername;
    }
  }

  public enum SSHAuthenticationMethod {
    NONE,
    PASSWORD,
    PUBLIC_PRIVATE_KEY
  }

  public enum RouterFirmware {
    DDWRT("DD-WRT", "DD-WRT", null, getTabsForDDWRT(), "/usr/sbin/nvram"),

    // TODO Not supported as yet
    OPENWRT("OpenWrt", "OpenWrt (Beta)", null, null /*getTabsForOpenWRT()*/, null),

    TOMATO(
        "Tomato",
        "Tomato (Experimental)",
        R.drawable.tomato_nav_drawer_header_bg,
        getTabsForTomato(),
        "/bin/nvram"),

    DEMO("Demo", "Demo", null, getTabsForDDWRT(), null),

    AUTO("-", "Auto-detect", null, null, null),

    UNKNOWN("???", "???", null, null, null);

    private static ImmutableList<RouterFirmware> values;

    @NonNull public final String displayName;

    @DrawableRes public final int drawerHeaderBackgroundDrawable;

    @Nullable
    public final ArrayListMultimap<Integer, FragmentTabDescription<? extends AbstractBaseFragment>>
        fragmentTabs;

    @Nullable public final String nvramPath;

    @NonNull public final String officialName;

    // To prevent expensive call to values() (which creates a new array list each time)
    public static ImmutableList<RouterFirmware> getValuesAsList() {
      if (values == null) {
        values = ImmutableList.copyOf(values());
      }
      return values;
    }

    RouterFirmware(
        @NonNull final String officialName,
        @NonNull final String displayName,
        @Nullable final Integer drawerHeaderBackgroundDrawable,
        @Nullable
            ArrayListMultimap<Integer, FragmentTabDescription<? extends AbstractBaseFragment>>
                fragmentTabs,
        @Nullable final String nvramPath) {
      this.officialName = officialName;
      this.displayName = displayName;
      if (drawerHeaderBackgroundDrawable == null) {
        this.drawerHeaderBackgroundDrawable = R.drawable.nav_drawer_header_bg;
      } else {
        this.drawerHeaderBackgroundDrawable = drawerHeaderBackgroundDrawable;
      }
      this.fragmentTabs = fragmentTabs;
      this.nvramPath = nvramPath;
    }

    @NonNull
    public String getDisplayName() {
      return displayName;
    }
  }

  public static final String USE_LOCAL_SSID_LOOKUP = "useLocalSSIDLookup";

  public static final String LOCAL_SSID_LOOKUPS = "localSSIDLookups";

  public static final String FALLBACK_TO_PRIMARY_ADDR = "fallbackToPrimaryAddr";

  public static final Comparator<Pair<String, String>> ALIASES_PAIR_COMPARATOR =
      new Comparator<Pair<String, String>>() {
        @Override
        public int compare(Pair<String, String> lhs, Pair<String, String> rhs) {
          if (lhs.first == null) {
            if (rhs.first == null) {
              return 0;
            }
            return -1;
          }
          if (rhs.first == null) {
            return 1;
          }
          return lhs.first.compareToIgnoreCase(rhs.first);
        }
      };

  public static final String[] mAvatarDownloadOpts =
      new String[] {
        "w_300",
        "h_300",
        "q_100",
        "c_thumb",
        "g_center",
        "r_20",
        "e_improve",
        "e_make_transparent",
        "e_trim"
      };

  public static final int RouterConnectionProtocol_SSH = 22;

  public static final int RouterConnectionProtocol_HTTP = 80;

  public static final int RouterConnectionProtocol_HTTPS = 443;

  public static final int SSHAuthenticationMethod_NONE = 1;

  public static final int SSHAuthenticationMethod_PASSWORD = 2;

  public static final int SSHAuthenticationMethod_PUBLIC_PRIVATE_KEY = 3;

  public static final int RouterIcon_Auto = 10;

  public static final int RouterIcon_Custom = 11;

  private static final String TAG = Router.class.getSimpleName();

  /**
   * Archived or not (may be used to hide routers, w/o actually deleting them). This is useful for
   * example for a "Swipe-to-dismiss-with-undo" feature
   */
  private boolean archived;

  private final Context context;

  private int iconMethod = RouterIcon_Auto;

  private String iconPath;

  /** the internal id (in DB) */
  private int id = -1;

  /** the router name */
  @Nullable private String name;

  private int orderIndex = -1;

  /** the password */
  @Nullable private String password;

  /** the private key, applicable only if connection channel is SSH */
  @Nullable private String privKey;

  /** the router IP or DNS */
  @NonNull private String remoteIpAddress;

  /** the port to connect on */
  private int remotePort = -1;

  /** the connection protocol */
  @NonNull private RouterConnectionProtocol routerConnectionProtocol;

  @Nullable private RouterFirmware routerFirmware;

  @Nullable private String routerModel;

  private final LoadingCache<RouterForSessionCache, Session> sessionsCache;

  private final Striped<Lock> sessionsStripes;

  private boolean strictHostKeyChecking = false;

  /** in case this is a clone of another router */
  @Nullable private final String templateUuid;

  /** the login username */
  @NonNull private String username;

  /** the router UUID */
  @NonNull private String uuid;

  public static void doFetchAndSetRouterAvatarInImageView(
      final Context context, final Router mRouter, final ImageView routerImageView) {
    Utils.downloadImageForRouter(
        context, mRouter, routerImageView, null, null, R.drawable.router, mAvatarDownloadOpts);
  }

  @NonNull
  public static Set<Pair<String, String>> getAliases(
      @Nullable final Context context, @Nullable final Router router) {
    final TreeSet<Pair<String, String>> aliases = new TreeSet<>(ALIASES_PAIR_COMPARATOR);
    final SharedPreferences preferences = getPreferences(router, context);
    if (preferences != null) {
      final Map<String, ?> preferencesAll = preferences.getAll();
      if (preferencesAll != null) {
        for (final Map.Entry<String, ?> entry : preferencesAll.entrySet()) {
          final String key = entry.getKey();
          final Object value = entry.getValue();
          if (isNullOrEmpty(key) || value == null) {
            continue;
          }
          // Check whether key is a MAC-Address
          if (!Utils.MAC_ADDRESS.matcher(key).matches()) {
            continue;
          }
          // This is a MAC Address - collect it right away!
          aliases.add(Pair.create(key.toLowerCase(), nullToEmpty(value.toString())));
        }
      }
    }
    return aliases;
  }

  @NonNull
  public static String getCanonicalHumanReadableNameWithEffectiveInfo(
      final Context ctx, final Router router, final boolean displayPort) {
    if (Utils.isDemoRouter(router)) {
      return ("(DEMO) " + router.getDisplayName());
    }
    return String.format(
        "%s (%s%s)",
        router.getDisplayName(),
        getEffectiveRemoteAddr(router, ctx),
        displayPort ? (":" + getEffectivePort(router, ctx)) : "");
  }

  @Nullable
  public static LocalSSIDLookup getEffectiveLocalSSIDLookup(
      @Nullable final Router router, @Nullable final Context ctx) {
    if (router == null || ctx == null) {
      return null;
    }
    final String currentNetworkSSID = Utils.getWifiName(ctx);
    // Detect network and use the corresponding IP Address if required
    final Collection<Router.LocalSSIDLookup> localSSIDLookupData =
        router.getLocalSSIDLookupData(ctx);
    if (!localSSIDLookupData.isEmpty()) {
      for (final Router.LocalSSIDLookup localSSIDLookup : localSSIDLookupData) {
        if (localSSIDLookup == null) {
          continue;
        }
        final String networkSsid = localSSIDLookup.getNetworkSsid();
        if (networkSsid == null || networkSsid.isEmpty()) {
          continue;
        }
        if (networkSsid.equals(currentNetworkSSID)
            || ("\"" + networkSsid + "\"").equals(currentNetworkSSID)) {
          return localSSIDLookup;
        }
      }
    }
    return null;
  }

  @Nullable
  public static Integer getEffectivePort(
      @Nullable final Router router, @Nullable final Context ctx) {
    if (router == null || ctx == null) {
      return null;
    }
    final int primaryRemotePort = router.getRemotePort();
    if (!isUseLocalSSIDLookup(router, ctx)) {
      return primaryRemotePort;
    }
    // else get alternate depending on current network
    final LocalSSIDLookup effectiveLocalSSIDLookup = getEffectiveLocalSSIDLookup(router, ctx);
    if (effectiveLocalSSIDLookup == null) {
      return primaryRemotePort;
    }
    return effectiveLocalSSIDLookup.getPort();
  }

  @Nullable
  public static String getEffectiveRemoteAddr(
      @Nullable final Router router, @Nullable final Context ctx) {
    if (router == null || ctx == null) {
      return null;
    }
    final String primaryRemoteIpAddress = router.getRemoteIpAddress();
    if (!isUseLocalSSIDLookup(router, ctx)) {
      return primaryRemoteIpAddress;
    }
    // else get alternate depending on current network
    final LocalSSIDLookup effectiveLocalSSIDLookup = getEffectiveLocalSSIDLookup(router, ctx);
    if (effectiveLocalSSIDLookup == null) {
      return primaryRemoteIpAddress;
    }
    return effectiveLocalSSIDLookup.getReachableAddr();
  }

  @Nullable
  public static SharedPreferences getPreferences(
      @Nullable final Router router, @Nullable final Context ctx) {
    if (router == null || ctx == null) {
      return null;
    }
    return ctx.getSharedPreferences(router.getPreferencesFile(), Context.MODE_PRIVATE);
  }

  public String getPreferencesFile() {
    return this.getUuid();
  }

  @Nullable
  public static Bitmap loadRouterAvatarUrlSync(
      @Nullable final Context context, @Nullable final Router router, @Nullable final String[] opts)
      throws IOException {
    final Uri routerAvatarUrl = Router.getRouterAvatarUrl(context, router, opts);
    if (routerAvatarUrl == null) {
      return null;
    }
    return new Picasso.Builder(context).build().load(routerAvatarUrl).get();
  }

  @Nullable
  public static Uri getRouterAvatarUrl(
      @Nullable final Context context, @Nullable final Router router, @Nullable final String[] opts)
      throws UnsupportedEncodingException {
    if (router == null) {
      return null;
    }
    if (Utils.isDemoRouter(router)) {
      // Demo router
      return ImageUtils.drawableToUri(context, R.drawable.demo_router);
    }
    if (router.getIconMethod() == RouterIcon_Custom && !TextUtils.isEmpty(router.getIconPath())) {
      return Uri.fromFile(new File(router.getIconPath()));
    }
    return Uri.parse(
        String.format(
            "%s/%s/%s.jpg",
            RouterCompanionAppConstants.IMAGE_CDN_URL_PREFIX,
            Joiner.on(",")
                .skipNulls()
                .join(opts != null ? opts : RouterCompanionAppConstants.CLOUDINARY_OPTS),
            URLEncoder.encode(
                nullToEmpty(Router.getRouterModel(context, router))
                    .toLowerCase()
                    .replaceAll("\\s+", ""),
                Charsets.UTF_8.name())));
  }

  @Nullable
  public static String getRouterModel(
      @Nullable final Context context, @Nullable final Router routerAt) {
    if (context == null || routerAt == null) {
      return null;
    }
    final String model =
        context
            .getSharedPreferences(routerAt.getUuid(), Context.MODE_PRIVATE)
            .getString(NVRAMInfo.Companion.getMODEL(), routerAt.routerModel);
    routerAt.setRouterModel(model);
    return model;
  }

  public static boolean isUseLocalSSIDLookup(
      @Nullable final Router router, @Nullable final Context ctx) {
    if (router == null || ctx == null) {
      return false;
    }
    final SharedPreferences sharedPreferences =
        ctx.getSharedPreferences(router.getUuid(), Context.MODE_PRIVATE);
    return sharedPreferences.getBoolean(USE_LOCAL_SSID_LOOKUP, false);
  }

  @SuppressLint("DefaultLocale")
  public static void openSSHConsole(
      @Nullable final Router router, @Nullable final Context context) {
    if (router == null || context == null) {
      FirebaseCrashlytics.getInstance().log("Internal Error: either router or context are null");
      Toast.makeText(context, "Internal Error. Please try again later.", Toast.LENGTH_SHORT).show();
      return;
    }
    final Router.LocalSSIDLookup effectiveLocalSSIDLookup =
        Router.getEffectiveLocalSSIDLookup(router, context);
    String ipAddress = router.getRemoteIpAddress();
    int remotePort = router.getRemotePort();
    if (effectiveLocalSSIDLookup != null) {
      final String reachableAddr = effectiveLocalSSIDLookup.getReachableAddr();
      if (!TextUtils.isEmpty(reachableAddr)) {
        ipAddress = reachableAddr;
      }
      final int port = effectiveLocalSSIDLookup.getPort();
      if (port > 0) {
        remotePort = port;
      }
    }
    try {
      // No need to pass the username, as an existing session probably exists in the external SSH
      // Client app.
      // This works well for JuiceSSH (where user can use an existing connection).
      // With some clients such as Termius, user will be prompted for a username
      // (password is passed below, but no support for privkeys)
      final Intent intent =
          new Intent(
              Intent.ACTION_VIEW, Uri.parse(String.format("ssh://%s:%d", ipAddress, remotePort)));

      final String creds;
      switch (router.getSshAuthenticationMethod()) {
        case PASSWORD:
          creds = router.getPasswordPlain();
          break;
        case PUBLIC_PRIVATE_KEY:
          creds = router.getPrivKeyPlain();
          break;
        default:
          creds = null;
          break;
      }
      // TODO Review intent for other SSH Clients: this is specific to Termius
      if (creds != null) {
        intent.putExtra("com.serverauditor.password", creds);
      }
      intent.putExtra("com.serverauditor.groupname", "Routers");

      context.startActivity(intent);
    } catch (final ActivityNotFoundException anfe) {
      Toast.makeText(
              context,
              "No SSH client found! Please install one to use this feature.",
              Toast.LENGTH_SHORT)
          .show();
    }
  }

  /** Default constructor */
  public Router(@Nullable final Context ctx) {
    this(ctx, null);
  }

  /**
   * Constructor
   *
   * @param router the router to copy
   */
  public Router(@Nullable final Context ctx, @Nullable final Router router) {
    this.context = ctx;
    if (router != null) {
      this.id = router.id;
      this.name = router.name;
      this.uuid = router.uuid;
      this.templateUuid = router.uuid;
      this.routerConnectionProtocol = router.routerConnectionProtocol;
      this.remoteIpAddress = router.remoteIpAddress;
      this.remotePort = router.remotePort;
      this.username = router.username;
      this.password = router.password;
      this.privKey = router.privKey;
      this.strictHostKeyChecking = router.strictHostKeyChecking;
      this.routerFirmware = router.routerFirmware;
      this.iconMethod = router.iconMethod;
      this.iconPath = router.iconPath;
      this.setRouterModel(Router.getRouterModel(ctx, router));
    } else {
      this.templateUuid = null;
    }

    this.sessionsStripes = Striped.lock(MAX_NUMBER_OF_CONCURRENT_SSH_SESSIONS_PER_ROUTER);

    // Init sessions cache
    this.sessionsCache =
        CacheBuilder.newBuilder()
            .maximumSize(MAX_NUMBER_OF_CONCURRENT_SSH_SESSIONS_PER_ROUTER)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .removalListener(
                new RemovalListener<RouterForSessionCache, Session>() {
                  @Override
                  public void onRemoval(
                      @NonNull RemovalNotification<RouterForSessionCache, Session> notification) {
                    final RemovalCause removalCause = notification.getCause();
                    final RouterForSessionCache routerForSessionCache = notification.getKey();
                    if (routerForSessionCache != null) {
                      FirebaseCrashlytics.getInstance()
                          .log(
                              "Removal Notification for <"
                                  + routerForSessionCache.router
                                  + ">. Cause : "
                                  + removalCause);
                    }

                    final Session session = notification.getValue();
                    if (session == null) {
                      return;
                    }
                    if (session.isConnected()) {
                      session.disconnect();
                    }
                  }
                })
            .build(
                new CacheLoader<RouterForSessionCache, Session>() {
                  @Override
                  public Session load(@NonNull RouterForSessionCache key) throws Exception {

                    final String ip = key.ipAddr;
                    Integer port = key.port;
                    if (port == null || port <= 0 || Strings.isNullOrEmpty(ip)) {
                      throw new IllegalArgumentException("port is NULL");
                    }

                    final String login = key.login;

                    final String privKey = key.router.getPrivKeyPlain();
                    final JSch jsch = new JSch();

                    final String passwordPlain = key.router.getPasswordPlain();

                    final Router.SSHAuthenticationMethod sshAuthenticationMethod =
                        key.router.getSshAuthenticationMethod();

                    final Session sshSession;
                    switch (sshAuthenticationMethod) {
                      case PUBLIC_PRIVATE_KEY:
                        //noinspection ConstantConditions
                        jsch.addIdentity(
                            key.router.getUuid() + "_" + login + "_" + port + "_" + ip,
                            !isNullOrEmpty(privKey) ? privKey.getBytes() : null,
                            null,
                            !isNullOrEmpty(passwordPlain) ? passwordPlain.getBytes() : null);
                        sshSession = jsch.getSession(login, ip, port);
                        break;
                      case PASSWORD:
                        sshSession = jsch.getSession(login, ip, port);
                        sshSession.setPassword(passwordPlain);
                        break;
                      default:
                        sshSession = jsch.getSession(login, ip, port);
                        break;
                    }

                    final boolean strictHostKeyChecking = key.router.isStrictHostKeyChecking();
                    // Set known hosts file to preferences file
                    //        jsch.setKnownHosts();

                    final Properties config = new Properties();
                    config.put(STRICT_HOST_KEY_CHECKING, strictHostKeyChecking ? YES : NO);

                    sshSession.setConfig(config);

                    //                        sshSession
                    //
                    // .setServerAliveInterval(CONNECTION_KEEP_ALIVE_INTERVAL_MILLIS);

                    sshSession.connect(CONNECT_TIMEOUT_MILLIS);

                    return sshSession;
                  }
                });
  }

  public void addHomeScreenShortcut(@Nullable final Context ctx) {
    if (ctx == null) {
      Toast.makeText(
              RouterCompanionApplication.Companion.getCurrentActivity(),
              "Internal Error - please try again later",
              Toast.LENGTH_SHORT)
          .show();
      return;
    }

    final Context appContext = ctx.getApplicationContext();
    final Context contextForshortcut = (appContext != null ? appContext : ctx);

    final String routerUuid = getUuid();
    final String canonicalHumanReadableName = getCanonicalHumanReadableName();
    final boolean demoRouter = Utils.isDemoRouter(this);

    // Add home-screen shortcut to this router
    final Intent shortcutIntent = new Intent(contextForshortcut, DDWRTMainActivity.class);
    //                    mShortcutIntent.setClassName("com.example.androidapp", "SampleIntent");
    shortcutIntent.setAction(Intent.ACTION_VIEW);
    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    shortcutIntent.putExtra(ROUTER_SELECTED, routerUuid);

    // Android O: pinned shortcuts
    if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
      // Normally, we should just use ShortcutManagerCompat, but we are using it just for Android O
      // (to create pinned shortcuts), as we need to update the icon on lower versions
      // Assumes there's already a shortcut with the ID "my-shortcut".
      // The shortcut must be enabled.
      final ShortcutInfoCompat pinShortcutInfoCompat =
          new ShortcutInfoCompat.Builder(context, routerUuid)
              .setIntent(shortcutIntent)
              .setShortLabel(canonicalHumanReadableName)
              .setIcon(
                  IconCompat.createWithResource(
                      context, demoRouter ? R.drawable.demo_router : R.drawable.router))
              .build();
      final ShortcutManager shortcutManager;
      if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
          && (shortcutManager = context.getSystemService(ShortcutManager.class)) != null) {
        boolean exists = false;
        final List<ShortcutInfo> pinnedShortcuts = shortcutManager.getPinnedShortcuts();
        for (final ShortcutInfo pinnedShortcut : pinnedShortcuts) {
          if (pinnedShortcut == null) {
            continue;
          }
          if (routerUuid.equals(pinnedShortcut.getId())) {
            exists = true;
            break;
          }
        }
        FirebaseCrashlytics.getInstance()
            .log(
                "addHomeScreenShortcut - Pinned shortcut for router "
                    + canonicalHumanReadableName
                    + "("
                    + routerUuid
                    + "): exists="
                    + exists);
        if (exists) {
          final ShortcutInfo pinShortcutInfo =
              new ShortcutInfo.Builder(context, routerUuid)
                  .setIntent(shortcutIntent)
                  .setShortLabel(canonicalHumanReadableName)
                  .setIcon(
                      Icon.createWithResource(
                          context, demoRouter ? R.drawable.demo_router : R.drawable.router))
                  .build();
          shortcutManager.updateShortcuts(Collections.singletonList(pinShortcutInfo));
        } else {
          ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfoCompat, null);
        }
      } else {
        ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfoCompat, null);
      }

      if (!demoRouter) {
        // Leverage Picasso to fetch router icon, if available
        try {
          ImageUtils.downloadImageFromUrl(
              contextForshortcut,
              getRouterAvatarUrl(contextForshortcut, this, mAvatarDownloadOpts),
              new Target() {
                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {}

                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                  final ShortcutManager shortcutManager;
                  if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
                      && (shortcutManager = context.getSystemService(ShortcutManager.class))
                          != null) {
                    final int iconSize;
                    if (appContext != null) {
                      iconSize =
                          (int)
                              appContext.getResources().getDimension(android.R.dimen.app_icon_size);
                    } else {
                      iconSize = -1;
                    }
                    final Bitmap scaledBitmap =
                        Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, false);

                    boolean exists = false;
                    final List<ShortcutInfo> pinnedShortcuts = shortcutManager.getPinnedShortcuts();
                    for (final ShortcutInfo pinnedShortcut : pinnedShortcuts) {
                      if (pinnedShortcut == null) {
                        continue;
                      }
                      if (routerUuid.equals(pinnedShortcut.getId())) {
                        exists = true;
                        break;
                      }
                    }
                    FirebaseCrashlytics.getInstance()
                        .log(
                            "Pinned shortcut for router "
                                + canonicalHumanReadableName
                                + "("
                                + routerUuid
                                + "): exists="
                                + exists);
                    if (exists) {
                      final ShortcutInfo pinShortcutInfo =
                          new ShortcutInfo.Builder(context, routerUuid)
                              .setIntent(shortcutIntent)
                              .setShortLabel(canonicalHumanReadableName)
                              .setIcon(Icon.createWithBitmap(scaledBitmap))
                              .build();
                      shortcutManager.updateShortcuts(Collections.singletonList(pinShortcutInfo));
                    } else {
                      final ShortcutInfoCompat pinShortcutInfo =
                          new ShortcutInfoCompat.Builder(context, routerUuid)
                              .setIntent(shortcutIntent)
                              .setShortLabel(canonicalHumanReadableName)
                              .setIcon(IconCompat.createWithBitmap(scaledBitmap))
                              .build();
                      ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null);
                    }
                  }
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {}
              },
              null,
              null,
              null);
        } catch (final Exception e) {
          // No worries
          Utils.reportException(contextForshortcut, e);
        }
      }
    } else {
      // otherwise, manually add it
      final Intent addIntent = new Intent();
      addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
      addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, canonicalHumanReadableName);
      addIntent.putExtra("duplicate", false); // Just create once
      addIntent.putExtra(
          Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
          Intent.ShortcutIconResource.fromContext(
              contextForshortcut, demoRouter ? R.drawable.demo_router : R.drawable.router));

      addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
      contextForshortcut.sendBroadcast(addIntent);

      if (!demoRouter) {
        // Leverage Picasso to fetch router icon, if available
        try {
          ImageUtils.downloadImageFromUrl(
              contextForshortcut,
              getRouterAvatarUrl(contextForshortcut, this, mAvatarDownloadOpts),
              new RouterShortcutAvatarDownloader(
                  this, contextForshortcut, shortcutIntent, addIntent),
              null,
              null,
              null);
        } catch (final Exception e) {
          // No worries
          Utils.reportException(contextForshortcut, e);
        }
      }
    }
    Toast.makeText(
            contextForshortcut,
            "Router pinned to Home Launcher Screen: " + canonicalHumanReadableName,
            Toast.LENGTH_SHORT)
        .show();
  }

  public void destroyActiveSession() {
    final RouterForSessionCache routerForSessionCache = new RouterForSessionCache(this);
    //        final Pair<String, Integer> effectiveIpAndPortTuple = getEffectiveIpAndPortTuple();
    final Lock lock = this.sessionsStripes.get(routerForSessionCache);
    try {
      lock.lock();
      this.sessionsCache.invalidate(routerForSessionCache);
    } finally {
      lock.unlock();
    }
  }

  public void destroyAllSessions() {
    this.sessionsCache.invalidateAll();
  }

  public void doFetchAndSetAvatarInImageView(
      final Context context, final ImageView routerImageView) {
    doFetchAndSetRouterAvatarInImageView(context, this, routerImageView);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Router router = (Router) o;

    if (id != router.id) {
      return false;
    }
    if (remotePort != router.remotePort) {
      return false;
    }
    if (strictHostKeyChecking != router.strictHostKeyChecking) {
      return false;
    }
    if (name != null ? !name.equals(router.name) : router.name != null) {
      return false;
    }
    if (password != null ? !password.equals(router.password) : router.password != null) {
      return false;
    }
    if (privKey != null ? !privKey.equals(router.privKey) : router.privKey != null) {
      return false;
    }
    if (!remoteIpAddress.equals(router.remoteIpAddress)) {
      return false;
    }
    if (routerConnectionProtocol != router.routerConnectionProtocol) {
      return false;
    }
    if (routerFirmware != router.routerFirmware) {
      return false;
    }
    if (!username.equals(router.username)) {
      return false;
    }
    return uuid.equals(router.uuid);
  }

  @NonNull
  public Set<Pair<String, String>> getAliases(@Nullable final Context context) {
    return getAliases(context, this);
  }

  public int getArchivedAsInt() {
    return (archived ? 1 : 0);
  }

  @SuppressLint("DefaultLocale")
  @NonNull
  public String getCanonicalHumanReadableName() {
    return getCanonicalHumanReadableName(this);
  }

  @SuppressLint("DefaultLocale")
  @NonNull
  public static String getCanonicalHumanReadableName(@NonNull final Router router) {
    return getCanonicalHumanReadableName(
        router.getDisplayName(), router.getRemoteIpAddress(), router.getRemotePort());
  }

  @SuppressLint("DefaultLocale")
  @NonNull
  public static String getCanonicalHumanReadableName(
      @Nullable final String displayName,
      @Nullable final String routerReachableAddr,
      final int port) {
    if (Utils.isDemoRouter(routerReachableAddr)) {
      return ("(DEMO) " + displayName);
    }
    return String.format("%s (%s:%d)", displayName, routerReachableAddr, port);
  }

  @NonNull
  public String getCanonicalHumanReadableNameWithEffectiveInfo(final boolean displayPort) {
    return getCanonicalHumanReadableNameWithEffectiveInfo(this.context, this, displayPort);
  }

  @NonNull
  public String getDisplayName() {
    return (isNullOrEmpty(name) ? "-" : name);
  }

  public int getIconMethod() {
    return iconMethod;
  }

  public void setIconMethod(int iconMethod) {
    this.iconMethod = iconMethod;
  }

  public String getIconPath() {
    return iconPath;
  }

  public void setIconPath(String iconPath) {
    this.iconPath = iconPath;
  }

  /**
   * @return the internal DB id
   */
  public int getId() {
    return id;
  }

  /**
   * Set the internal DB id
   *
   * @param id the internal DB id to set
   * @return this object
   */
  @NonNull
  public Router setId(int id) {
    this.id = id;
    return this;
  }

  @NonNull
  public Collection<LocalSSIDLookup> getLocalSSIDLookupData(@NonNull final Context ctx) {
    final SharedPreferences sharedPreferences =
        ctx.getSharedPreferences(this.getUuid(), Context.MODE_PRIVATE);
    final Set<String> localSSIDLookupStringSet =
        sharedPreferences.getStringSet(LOCAL_SSID_LOOKUPS, new HashSet<>());
    final List<LocalSSIDLookup> localSSIDLookups = new ArrayList<>(localSSIDLookupStringSet.size());
    final Gson gson = WakeOnLanTile.GSON_BUILDER.create();
    for (final String localSSIDLookupString : localSSIDLookupStringSet) {
      if (localSSIDLookupString == null || localSSIDLookupString.isEmpty()) {
        continue;
      }
      try {
        localSSIDLookups.add(gson.fromJson(localSSIDLookupString, LocalSSIDLookup.class));
      } catch (final Exception e) {
        e.printStackTrace();
        // No worries
      }
    }
    return localSSIDLookups;
  }

  /**
   * @return the name
   */
  @Nullable
  public String getName() {
    return name;
  }

  /**
   * Set the name
   *
   * @param name the name to set
   * @return this object
   */
  @NonNull
  public Router setName(@Nullable final String name) {
    this.name = name;
    return this;
  }

  @NonNull
  public String getNotificationChannelId() {
    return (this.uuid + "-router-events");
  }

  public int getOrderIndex() {
    return orderIndex;
  }

  public Router setOrderIndex(int orderIndex) {
    this.orderIndex = orderIndex;
    return this;
  }

  /**
   * @return the password
   */
  @Nullable
  public String getPassword() {
    return password;
  }

  /**
   * @return the password
   */
  @Nullable
  public String getPasswordPlain() {
    return password != null ? d(password) : null;
  }

  @Nullable
  public SharedPreferences getPreferences(@Nullable final Context ctx) {
    return getPreferences(this, ctx);
  }

  /**
   * @return the privKey
   */
  @Nullable
  public String getPrivKey() {
    return privKey;
  }

  /**
   * @return the decrypted privKey
   */
  @Nullable
  public String getPrivKeyPlain() {
    return privKey != null ? d(privKey) : null;
  }

  /**
   * @return the remoteIpAddress
   */
  @NonNull
  public String getRemoteIpAddress() {
    return remoteIpAddress;
  }

  /**
   * Set the remoteIpAddress
   *
   * @param remoteIpAddress the remoteIpAddress to set
   * @return this object
   */
  @NonNull
  public Router setRemoteIpAddress(@NonNull final String remoteIpAddress) {
    this.remoteIpAddress = remoteIpAddress;
    return this;
  }

  /**
   * @return the remotePort, if any, or the default port for the routerConnectionProtocol
   */
  public int getRemotePort() {
    return remotePort <= 0 ? this.routerConnectionProtocol.getDefaultPort() : remotePort;
  }

  /**
   * Set the remotePort
   *
   * @param remotePort the remotePort to set
   * @return this object
   */
  @NonNull
  public Router setRemotePort(final int remotePort) {
    this.remotePort = remotePort;
    return this;
  }

  /**
   * @return the RouterConnectionProtocol
   */
  @NonNull
  public RouterConnectionProtocol getRouterConnectionProtocol() {
    return routerConnectionProtocol;
  }

  /**
   * Set the RouterConnectionProtocol
   *
   * @param routerConnectionProtocol the RouterConnectionProtocol to set
   * @return this object
   */
  @NonNull
  public Router setRouterConnectionProtocol(
      @NonNull final RouterConnectionProtocol routerConnectionProtocol) {
    this.routerConnectionProtocol = routerConnectionProtocol;
    return this;
  }

  @Nullable
  public RouterFirmware getRouterFirmware() {
    return routerFirmware;
  }

  public void setRouterFirmware(@Nullable String routerFirmwareStr) {
    if (isNullOrEmpty(routerFirmwareStr)) {
      return;
    }
    try {
      setRouterFirmware(RouterFirmware.valueOf(routerFirmwareStr));
    } catch (final Exception e) {
      ReportingUtils.reportException(null, e);
    }
  }

  @Nullable
  public String getRouterModel() {
    return routerModel;
  }

  public Router setRouterModel(@Nullable String routerModel) {
    this.routerModel = routerModel;
    return this;
  }

  @Nullable
  public Session getSSHSession() throws Exception {
    final RouterForSessionCache routerForSessionCache = new RouterForSessionCache(this);
    //        final Pair<String, Integer> effectiveIpAndPortTuple = getEffectiveIpAndPortTuple();
    final Lock lock = this.sessionsStripes.get(routerForSessionCache);
    try {
      lock.lock();
      final Session session = this.sessionsCache.get(routerForSessionCache);
      if (session != null && !session.isConnected()) {
        session.connect(CONNECT_TIMEOUT_MILLIS);
      }
      return session;
    } catch (final Exception e) {
      /*
       * Invalidate record right away so it can be retried again later
       */
      this.sessionsCache.invalidate(routerForSessionCache);
      throw e;
    } finally {
      lock.unlock();
    }
  }

  @NonNull
  public SSHAuthenticationMethod getSshAuthenticationMethod() {
    if (!isNullOrEmpty(privKey)) {
      return SSHAuthenticationMethod.PUBLIC_PRIVATE_KEY;
    }
    if (!isNullOrEmpty(password)) {
      return SSHAuthenticationMethod.PASSWORD;
    }
    return SSHAuthenticationMethod.NONE;
  }

  @Nullable
  public String getTemplateUuid() {
    return templateUuid;
  }

  @NonNull
  public String getTemplateUuidOrUuid() {
    return templateUuid != null ? templateUuid : uuid;
  }

  /**
   * @return the username
   */
  @NonNull
  public String getUsername() {
    return username;
  }

  /**
   * @return the unencrypted username
   */
  @Nullable
  public String getUsernamePlain() {
    return d(username);
  }

  /**
   * @return the uuid
   */
  @NonNull
  public String getUuid() {
    return uuid;
  }

  /**
   * Set the uuid
   *
   * @param uuid the uuid to set
   * @return this object
   */
  @NonNull
  public Router setUuid(@NonNull final String uuid) {
    this.uuid = uuid;
    return this;
  }

  @Override
  public int hashCode() {
    int result = routerConnectionProtocol.hashCode();
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + remoteIpAddress.hashCode();
    result = 31 * result + remotePort;
    result = 31 * result + username.hashCode();
    result = 31 * result + (password != null ? password.hashCode() : 0);
    result = 31 * result + (privKey != null ? privKey.hashCode() : 0);
    result = 31 * result + id;
    result = 31 * result + (strictHostKeyChecking ? 1 : 0);
    result = 31 * result + uuid.hashCode();
    return result;
  }

  public boolean isArchived() {
    return archived;
  }

  public Router setArchived(boolean archived) {
    this.archived = archived;
    return this;
  }

  public boolean isFallbackToPrimaryAddr(@Nullable final Context ctx) {
    if (ctx == null) {
      return false;
    }
    final SharedPreferences sharedPreferences =
        ctx.getSharedPreferences(this.getUuid(), Context.MODE_PRIVATE);
    return sharedPreferences.getBoolean(FALLBACK_TO_PRIMARY_ADDR, false);
  }

  /**
   * @return whether the strictHostKeyChecking flag is on or off
   */
  public boolean isStrictHostKeyChecking() {
    return strictHostKeyChecking;
  }

  /**
   * Set the strictHostKeyChecking
   *
   * @param strictHostKeyChecking the strictHostKeyChecking to set
   * @return this object
   */
  @NonNull
  public Router setStrictHostKeyChecking(final boolean strictHostKeyChecking) {
    this.strictHostKeyChecking = strictHostKeyChecking;
    return this;
  }

  public boolean isUseLocalSSIDLookup(@Nullable final Context ctx) {
    return isUseLocalSSIDLookup(this, ctx);
  }

  //    public static final int RouterFirmware_DDWRT = 1;
  //    public static final int RouterFirmware_OPENWRT = 2;
  //    public static final int RouterFirmware_DEMO = 3;
  //    public static final int RouterFirmware_UNKNOWN = 4;
  //    public static final int RouterFirmware_AUTO = 5;
  //    public static final int RouterFirmware_TOMATO = 6;

  public Router setArchivedFromInt(final int archived) {
    if (archived != 0 && archived != 1) {
      throw new IllegalArgumentException("Invalid arg: " + archived + ". Accepted values: 0 or 1");
    }
    return this.setArchived(archived == 1);
  }

  public void setFallbackToPrimaryAddr(@NonNull final Context ctx, final boolean value) {
    final SharedPreferences sharedPreferences =
        ctx.getSharedPreferences(this.getUuid(), Context.MODE_PRIVATE);
    sharedPreferences.edit().putBoolean(FALLBACK_TO_PRIMARY_ADDR, value).apply();
    Utils.requestBackup(ctx);
  }

  public void setLocalSSIDLookupData(
      @NonNull final Context ctx, Collection<LocalSSIDLookup> localSSIDLookups) {
    final SharedPreferences sharedPreferences =
        ctx.getSharedPreferences(this.getUuid(), Context.MODE_PRIVATE);
    sharedPreferences.edit().remove(LOCAL_SSID_LOOKUPS).apply();
    final Set<String> localSSIDLookupStringSet = new HashSet<>();
    if (localSSIDLookups != null) {
      final Gson gson = WakeOnLanTile.GSON_BUILDER.create();
      for (final LocalSSIDLookup localSSIDLookup : localSSIDLookups) {
        localSSIDLookupStringSet.add(gson.toJson(localSSIDLookup));
      }
    }
    sharedPreferences.edit().putStringSet(LOCAL_SSID_LOOKUPS, localSSIDLookupStringSet).apply();
    Utils.requestBackup(ctx);
  }

  /**
   * Set the password
   *
   * @param password the password to set
   * @param encrypt whether to encrypt data. To avoid encrypting twice, set this to <code>false
   *     </code> if <code>privKey</code> is known to be encrypted (e.g., when retrieved from the DB)
   * @return this object
   */
  @NonNull
  public Router setPassword(@Nullable final String password, final boolean encrypt) {
    this.password = encrypt ? e(password) : password;
    return this;
  }

  /**
   * Set the privKey
   *
   * @param privKey the privKey to set
   * @param encrypt whether to encrypt data. To avoid encrypting twice, set this to <code>false
   *     </code> if <code>privKey</code> is known to be encrypted (e.g., when retrieved from the B)
   * @return this object
   */
  @NonNull
  public Router setPrivKey(@Nullable final String privKey, final boolean encrypt) {
    this.privKey = encrypt ? e(privKey) : privKey;
    return this;
  }

  public void setRouterFirmware(@Nullable RouterFirmware routerFirmware) {
    this.routerFirmware = routerFirmware;
  }

  public void setUseLocalSSIDLookup(@NonNull final Context ctx, final boolean value) {
    final SharedPreferences sharedPreferences =
        ctx.getSharedPreferences(this.getUuid(), Context.MODE_PRIVATE);
    sharedPreferences.edit().putBoolean(USE_LOCAL_SSID_LOOKUP, value).apply();
    Utils.requestBackup(ctx);
  }

  /**
   * Set the username
   *
   * @param username the username to set
   * @param encrypt whether to encrypt data. To avoid encrypting twice, set this to <code>false
   *     </code> if <code>privKey</code> is known to be encrypted (e.g., when retrieved from the DB)
   * @return this object
   */
  @NonNull
  public Router setUsername(@NonNull final String username, final boolean encrypt) {
    //noinspection ConstantConditions
    this.username = encrypt ? e(username) : username;
    return this;
  }

  public RouterInfo toRouterInfo() {
    return new RouterInfo()
        .setId(this.id)
        .setUuid(this.uuid)
        .setName(this.name)
        .setRouterConnectionProtocol(this.routerConnectionProtocol.name())
        .setRemoteIpAddress(this.remoteIpAddress)
        .setRemotePort(this.remotePort)
        .setRouterFirmware(this.routerFirmware != null ? this.routerFirmware.name() : null)
        .setRouterModel(this.routerModel)
        .setDemoRouter(Utils.isDemoRouter(this));
  }

  /**
   * @return the Router string representation
   */
  @Override
  @NonNull
  public String toString() {
    return "Router{"
        + "sshAuth=Type="
        + getSshAuthenticationMethod()
        + ", routerConnectionProtocol="
        + routerConnectionProtocol
        + ", name='"
        + name
        + '\''
        + ", remoteIpAddress='"
        + remoteIpAddress
        + '\''
        + ", remotePort="
        + remotePort
        + ", id="
        + id
        + ", strictHostKeyChecking="
        + strictHostKeyChecking
        + ", uuid='"
        + uuid
        + '\''
        + ", routerFirmware="
        + routerFirmware
        + '}';
  }

  @NonNull
  private Pair<String, Integer> getEffectiveIpAndPortTuple() {
    final LocalSSIDLookup ssidLookup = getEffectiveLocalSSIDLookup(this, context);
    String ip = remoteIpAddress;
    Integer port = remotePort;
    if (ssidLookup != null) {
      ip = ssidLookup.getReachableAddr();
      port = ssidLookup.getPort();
    }
    return Pair.create(ip, port);
  }
}
