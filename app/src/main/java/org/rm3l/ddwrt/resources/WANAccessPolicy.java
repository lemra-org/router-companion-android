package org.rm3l.ddwrt.resources;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rm3l on 21/01/16.
 */
public class WANAccessPolicy {

    public static final String DENY = "deny";
    public static final String FILTER = "filter";

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
}
