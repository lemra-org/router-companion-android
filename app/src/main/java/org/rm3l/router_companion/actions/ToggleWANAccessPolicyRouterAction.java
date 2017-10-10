package org.rm3l.router_companion.actions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.common.base.Strings;
import java.util.Locale;
import org.rm3l.router_companion.common.resources.audit.ActionLog;
import org.rm3l.router_companion.resources.WANAccessPolicy;
import org.rm3l.router_companion.resources.conn.Router;
import org.rm3l.router_companion.utils.SSHUtils;

/**
 * Created by rm3l on 27/01/16.
 */
public class ToggleWANAccessPolicyRouterAction extends AbstractRouterAction<Void> {

    public static final int DISABLE = 0;

    public static final int ENABLE_1 = 1;

    public static final int ENABLE_2 = 2;

    @NonNull
    private final Context mContext;

    private final int mEnableStatus;

    @NonNull
    private final WANAccessPolicy mWanAccessPolicy;

    public ToggleWANAccessPolicyRouterAction(Router router, @NonNull Context context,
            @Nullable RouterActionListener listener, @NonNull SharedPreferences globalSharedPreferences,
            @NonNull WANAccessPolicy wanAccessPolicy, int enableStatus) {
        super(router, listener, enableStatus == 0 ? RouterAction.DISABLE_WAN_ACCESS_POLICY
                : RouterAction.ENABLE_WAN_ACCESS_POLICY, globalSharedPreferences);
        this.mContext = context;
        this.mWanAccessPolicy = wanAccessPolicy;
        this.mEnableStatus = enableStatus;
    }

    @NonNull
    @Override
    protected RouterActionResult<Void> doActionInBackground() {
        Exception exception = null;
        try {
            if (mWanAccessPolicy.getNumber() <= 0) {
                throw new IllegalArgumentException();
            }

            /*
            - Enabled / Filter: $STAT:2$NAME:myPolicy1StatusEnableFilter$DENY:0$$
            - Enabled / Deny: $STAT:1$NAME:myPolicy1StatusEnableDeny$DENY:1$$
            - Disabled / Deny: $STAT:0$NAME:myPolicy1StatusDisableDeny$DENY:1$$
            - Disabled / Filter : $STAT:0$NAME:myPolicy1StatusDisableFilter$DENY:0$$
             */
            final String filterRuleValue =
                    String.format(Locale.US, "\\$STAT:%d\\$NAME:%s\\$DENY:%d\\$\\$", mEnableStatus,
                            Strings.nullToEmpty(mWanAccessPolicy.getName()),
                            WANAccessPolicy.Companion.getDENY().equals(mWanAccessPolicy.getDenyOrFilter()) ? 1 : 0);

            final int exitStatus = SSHUtils.runCommands(mContext, globalSharedPreferences, router,
                    String.format(Locale.US, "/usr/sbin/nvram set filter_rule%d=\"%s\""
                            + " && /sbin/stopservice firewall"
                            + " && /sbin/startservice firewall", mWanAccessPolicy.getNumber(), filterRuleValue));
            if (exitStatus != 0) {
                throw new IllegalStateException();
            }
        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        }

        return new RouterActionResult<>(null, exception);
    }

    @Override
    protected ActionLog getActionLog() {
        return super.getActionLog()
                .setActionData(
                        String.format("- Policy Name: %s\n" + "- Status: %d", mWanAccessPolicy, mEnableStatus));
    }

    @Nullable
    @Override
    protected Context getContext() {
        return mContext;
    }
}
