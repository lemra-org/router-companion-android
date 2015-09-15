package org.rm3l.ddwrt.resources;

import android.support.annotation.Nullable;

/**
 * Created by rm3l on 07/09/15.
 */
public class PublicIPInfo {

    public static final String IPIFY_API_RAW = "https://api.ipify.org";
    public static final String IPIFY_API_JSON = "https://api.ipify.org?format=json";

    public static final String ICANHAZIP_HOST = "icanhazip.com";
    public static final int ICANHAZIP_PORT = 80;

    public static final String ICANHAZPTR_HOST = "icanhazptr.com";
    public static final int ICANHAZPTR_PORT = 80;

    @Nullable
    private String ip;

    @Nullable
    public String getIp() {
        return ip;
    }

    public PublicIPInfo setIp(@Nullable String ip) {
        this.ip = ip;
        return this;
    }

    @Override
    public String toString() {
        return "PublicIPInfo {" +
                "ip='" + ip + '\'' +
                '}';
    }

}
