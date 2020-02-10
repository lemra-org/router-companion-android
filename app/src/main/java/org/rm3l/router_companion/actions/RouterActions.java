package org.rm3l.router_companion.actions;

/** Created by rm3l on 08/12/15. */
public final class RouterActions {

  public static final int REBOOT = 1;

  public static final int BACKUP = 2;

  public static final int RESTORE = 3;

  public static final int RESTORE_FACTORY_DEFAULTS = 4;

  public static final int SET_NVRAM_VARIABLES = 5;

  public static final int UPGRADE_FIRMWARE = 6;

  public static final int WAKE_ON_LAN = 7;

  public static final int RESET_COUNTERS = 8;

  public static final int DISABLE_WAN_ACCESS = 9;

  public static final int ENABLE_WAN_ACCESS = 10;

  public static final int PING = 11;

  public static final int ARPING = 12;

  public static final int TRACEROUTE = 13;

  public static final int NSLOOKUP = 14;

  public static final int CMD_SHELL = 15;

  public static final int WHOIS = 16;

  public static final int MAC_OUI_LOOKUP = 17;

  public static final int BACKUP_WAN_TRAFF = 18;

  public static final int RESTORE_WAN_TRAFF = 19;

  public static final int DELETE_WAN_TRAFF = 20;

  public static final int TOGGLE_WL_RADIO = 21;

  public static final int DHCP_RELEASE = 22;

  public static final int DHCP_RENEW = 23;

  public static final int TOGGLE_PHY_IFACE_STATE = 24;

  public static final int EXPORT_ALIASES = 101;

  public static final int IMPORT_ALIASES = 102;

  private RouterActions() {}
}
