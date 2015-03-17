package org.rm3l.ddwrt.main;

public class NavigationDrawerMenuItem {

    public String title;
    public boolean isHeader;

    public NavigationDrawerMenuItem(String title, boolean header) {
        this.title = title;
        this.isHeader = header;
    }

    public NavigationDrawerMenuItem(String title) {
        this(title, false);
    }
}
