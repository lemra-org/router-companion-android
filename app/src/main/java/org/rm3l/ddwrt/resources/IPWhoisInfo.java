package org.rm3l.ddwrt.resources;

import android.support.annotation.Nullable;

public class IPWhoisInfo {

    public static final String IP_WHOIS_INFO_API_PREFIX = "https://dazzlepod.com/ip/";

    /**
     * {
     * "ip": "216.58.208.234",
     * "prefix": "216.58.208.0/24",
     * "country_code": "US",
     * "asn": "AS15169",
     * "city": "Mountain View",
     * "country": "United States",
     * "region": "California",
     * "hostname": "par10s22-in-f10.1e100.net",
     * "longitude": -122.0574,
     * "latitude": 37.4192,
     * "organization": "GOOGLE - Google Inc.,US"
     * }
     */

    @Nullable
    private String ip;

    @Nullable
    private String prefix;

    @Nullable
    private String country_code;

    @Nullable
    private String asn;

    @Nullable
    private String city;

    @Nullable
    private String country;

    @Nullable
    private String region;

    @Nullable
    private String hostname;

    @Nullable
    private String longitude;

    @Nullable
    private String latitude;

    @Nullable
    private String organization;

    @Nullable
    public String getIp() {
        return ip;
    }

    public void setIp(@Nullable String ip) {
        this.ip = ip;
    }

    @Nullable
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(@Nullable String prefix) {
        this.prefix = prefix;
    }

    @Nullable
    public String getCountry_code() {
        return country_code;
    }

    public void setCountry_code(@Nullable String country_code) {
        this.country_code = country_code;
    }

    @Nullable
    public String getAsn() {
        return asn;
    }

    public void setAsn(@Nullable String asn) {
        this.asn = asn;
    }

    @Nullable
    public String getCity() {
        return city;
    }

    public void setCity(@Nullable String city) {
        this.city = city;
    }

    @Nullable
    public String getCountry() {
        return country;
    }

    public void setCountry(@Nullable String country) {
        this.country = country;
    }

    @Nullable
    public String getRegion() {
        return region;
    }

    public void setRegion(@Nullable String region) {
        this.region = region;
    }

    @Nullable
    public String getHostname() {
        return hostname;
    }

    public void setHostname(@Nullable String hostname) {
        this.hostname = hostname;
    }

    @Nullable
    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(@Nullable String longitude) {
        this.longitude = longitude;
    }

    @Nullable
    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(@Nullable String latitude) {
        this.latitude = latitude;
    }

    @Nullable
    public String getOrganization() {
        return organization;
    }

    public void setOrganization(@Nullable String organization) {
        this.organization = organization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IPWhoisInfo that = (IPWhoisInfo) o;

        if (asn != null ? !asn.equals(that.asn) : that.asn != null) return false;
        if (city != null ? !city.equals(that.city) : that.city != null) return false;
        if (country != null ? !country.equals(that.country) : that.country != null) return false;
        if (country_code != null ? !country_code.equals(that.country_code) : that.country_code != null)
            return false;
        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null)
            return false;
        if (ip != null ? !ip.equals(that.ip) : that.ip != null) return false;
        if (latitude != null ? !latitude.equals(that.latitude) : that.latitude != null)
            return false;
        if (longitude != null ? !longitude.equals(that.longitude) : that.longitude != null)
            return false;
        if (organization != null ? !organization.equals(that.organization) : that.organization != null)
            return false;
        if (prefix != null ? !prefix.equals(that.prefix) : that.prefix != null) return false;
        if (region != null ? !region.equals(that.region) : that.region != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        result = 31 * result + (country_code != null ? country_code.hashCode() : 0);
        result = 31 * result + (asn != null ? asn.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (country != null ? country.hashCode() : 0);
        result = 31 * result + (region != null ? region.hashCode() : 0);
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        result = 31 * result + (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (organization != null ? organization.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "IPWhoisInfo{" +
                "ip='" + ip + '\'' +
                ", prefix='" + prefix + '\'' +
                ", country_code='" + country_code + '\'' +
                ", asn='" + asn + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", region='" + region + '\'' +
                ", hostname='" + hostname + '\'' +
                ", longitude='" + longitude + '\'' +
                ", latitude='" + latitude + '\'' +
                ", organization='" + organization + '\'' +
                '}';
    }
}
