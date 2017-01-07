package org.rm3l.router_companion.resources;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rm3l on 21/01/16.
 */
public class WANAccessPolicy {

    public static final String DENY = "Deny";
    public static final String FILTER = "Filter";

    public static final String STATUS_UNKNOWN = "unknown";

    private int number;

    private String name;

    private String status;

    private String timeOfDay;

    /**
     * S M T W T F S
     *
     * 1 if enabled, 0 otherwise
     *
     * 7 if "All days"
     */
    private String daysPattern;

    private String denyOrFilter;

    private final List<String> blockedServices = new ArrayList<>();

    private final List<String> blockedWebsitesByUrl = new ArrayList<>();

    private final List<String> blockedWebsitesByKeyword = new ArrayList<>();

    public int getNumber() {
        return number;
    }

    public WANAccessPolicy setNumber(int number) {
        this.number = number;
        return this;
    }

    public String getName() {
        return name;
    }

    public WANAccessPolicy setName(String name) {
        this.name = name;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public WANAccessPolicy setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    public WANAccessPolicy setTimeOfDay(String timeOfDay) {
        this.timeOfDay = timeOfDay;
        return this;
    }

    public String getDaysPattern() {
        return daysPattern;
    }

    public WANAccessPolicy setDaysPattern(String daysPattern) {
        this.daysPattern = daysPattern;
        return this;
    }

    public String getDenyOrFilter() {
        return denyOrFilter;
    }

    public WANAccessPolicy setDenyOrFilter(String denyOrFilter) {
        this.denyOrFilter = denyOrFilter;
        return this;
    }

    public List<String> getBlockedServices() {
        return blockedServices;
    }

    public List<String> getBlockedWebsitesByUrl() {
        return blockedWebsitesByUrl;
    }

    public List<String> getBlockedWebsitesByKeyword() {
        return blockedWebsitesByKeyword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WANAccessPolicy that = (WANAccessPolicy) o;

        if (number != that.number) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (timeOfDay != null ? !timeOfDay.equals(that.timeOfDay) : that.timeOfDay != null)
            return false;
        if (daysPattern != null ? !daysPattern.equals(that.daysPattern) : that.daysPattern != null)
            return false;
        if (denyOrFilter != null ? !denyOrFilter.equals(that.denyOrFilter) : that.denyOrFilter != null)
            return false;
        if (blockedServices != null ? !blockedServices.equals(that.blockedServices) : that.blockedServices != null)
            return false;
        if (blockedWebsitesByUrl != null ? !blockedWebsitesByUrl.equals(that.blockedWebsitesByUrl) : that.blockedWebsitesByUrl != null)
            return false;
        return blockedWebsitesByKeyword != null ? blockedWebsitesByKeyword.equals(that.blockedWebsitesByKeyword) : that.blockedWebsitesByKeyword == null;

    }

    @Override
    public int hashCode() {
        int result = number;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (timeOfDay != null ? timeOfDay.hashCode() : 0);
        result = 31 * result + (daysPattern != null ? daysPattern.hashCode() : 0);
        result = 31 * result + (denyOrFilter != null ? denyOrFilter.hashCode() : 0);
        result = 31 * result + (blockedServices != null ? blockedServices.hashCode() : 0);
        result = 31 * result + (blockedWebsitesByUrl != null ? blockedWebsitesByUrl.hashCode() : 0);
        result = 31 * result + (blockedWebsitesByKeyword != null ? blockedWebsitesByKeyword.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "WANAccessPolicy{" +
                "number=" + number +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", timeOfDay='" + timeOfDay + '\'' +
                ", daysPattern='" + daysPattern + '\'' +
                ", denyOrFilter='" + denyOrFilter + '\'' +
                ", blockedServices=" + blockedServices +
                ", blockedWebsitesByUrl=" + blockedWebsitesByUrl +
                ", blockedWebsitesByKeyword=" + blockedWebsitesByKeyword +
                '}';

    }
}
