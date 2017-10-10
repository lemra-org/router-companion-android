package org.rm3l.router_companion.mgmt.register.resources;

import android.text.TextUtils;
import com.google.gson.GsonBuilder;

/**
 * Created by rm3l on 11/04/16.
 */
public class RouterWizardAction {

    public static final String ROUTER_WIZARD_ACTION = "ROUTER_WIZARD_ACTION";

    public static final int ADD = 10;

    public static final int EDIT = 11;

    public static final int COPY = 12;

    public static final GsonBuilder GSON_BUILDER = new GsonBuilder();

    private Integer action;

    private String routerUuid;

    public int getAction() {
        return action != null ? action : TextUtils.isEmpty(routerUuid) ? ADD : EDIT;
    }

    public RouterWizardAction setAction(Integer action) {
        this.action = action;
        return this;
    }

    public String getRouterUuid() {
        return routerUuid;
    }

    public RouterWizardAction setRouterUuid(String routerUuid) {
        this.routerUuid = routerUuid;
        return this;
    }
}
